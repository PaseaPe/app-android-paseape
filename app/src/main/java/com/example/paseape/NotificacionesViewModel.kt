package com.example.paseape

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.NotificacionDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificacionesViewModel : ViewModel() {

    private val apiService = RetrofitClient.apiService

    var isLoading by mutableStateOf(false)
        private set
    var marcandoLeidas by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var listaNotificaciones by mutableStateOf<List<NotificacionDto>>(emptyList())
        private set

    val cantidadNoLeidas: Int
        get() = listaNotificaciones.count { it.leido != true }

    companion object {
        private val _unreadCount = MutableStateFlow(0)
        val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

        fun inicializarContexto(context: Context, userUuid: String?) {
            // No-op mantenido por compatibilidad
        }

        suspend fun sincronizarContadorRemoto(
            context: Context,
            authHeader: String,
            userUuid: String?
        ) {
            if (authHeader.isBlank()) return
            withContext(Dispatchers.IO) {
                runCatching {
                    val token = if (authHeader.startsWith("Bearer ")) authHeader else "Bearer $authHeader"
                    val response = RetrofitClient.apiService.listarNotificaciones(token)
                    if (response.isSuccessful && response.body() != null) {
                        val noLeidas = response.body()?.response?.count { it.leido != true } ?: 0
                        _unreadCount.value = noLeidas
                    }
                }
            }
        }
    }

    fun cargarNotificaciones(
        authHeader: String,
        incluirLeidas: Boolean = false
    ) {
        if (authHeader.isBlank()) {
            errorMessage = "No se encontró una sesión activa."
            return
        }
        isLoading = true
        errorMessage = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = if (authHeader.startsWith("Bearer ")) authHeader else "Bearer $authHeader"
                val response = apiService.listarNotificaciones(token)
                val body = response.body()

                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (response.isSuccessful && body != null) {
                        val remotas = body.response.orEmpty()
                        listaNotificaciones = remotas
                        _unreadCount.value = remotas.count { it.leido != true }
                    } else if (body != null && !body.message.isNullOrBlank()) {
                        errorMessage = body.message
                    } else {
                        errorMessage = ApiErrorMapper.fromResponse(response, ApiErrorContext.NOTIFICATION)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = ApiErrorMapper.fromThrowable(e, ApiErrorContext.NOTIFICATION)
                }
            }
        }
    }

    fun marcarNotificacionIndividualComoLeida(authHeader: String, notificacion: NotificacionDto) {
        val actualizada = listaNotificaciones.filterNot { it.uuid == notificacion.uuid }
        listaNotificaciones = actualizada
        _unreadCount.value = actualizada.count { it.leido != true }
    }

    fun marcarTodasComoLeidas(
        authHeader: String,
        onSuccess: () -> Unit = {},
        onAlreadyRead: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (authHeader.isBlank()) return

        val hayPendientes = listaNotificaciones.any { it.leido != true } || _unreadCount.value > 0
        if (!hayPendientes) {
            onAlreadyRead()
            return
        }

        marcandoLeidas = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = if (authHeader.startsWith("Bearer ")) authHeader else "Bearer $authHeader"
                val response = apiService.marcarTodasNotificacionesLeidas(token)
                val body = response.body()

                withContext(Dispatchers.Main) {
                    marcandoLeidas = false
                    if (response.isSuccessful) {
                        listaNotificaciones = emptyList()
                        _unreadCount.value = 0
                        onSuccess()
                    } else {
                        val msg = body?.message?.takeIf { it.isNotBlank() }
                            ?: ApiErrorMapper.fromResponse(response, ApiErrorContext.NOTIFICATION)
                        onError(msg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    marcandoLeidas = false
                    onError(ApiErrorMapper.fromThrowable(e, ApiErrorContext.NOTIFICATION))
                }
            }
        }
    }
}
