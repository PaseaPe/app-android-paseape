package com.example.paseape

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

    fun cargarNotificaciones(authHeader: String) {
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
                        listaNotificaciones = body.response.orEmpty()
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

    fun marcarTodasComoLeidas(
        authHeader: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (authHeader.isBlank() || listaNotificaciones.isEmpty()) return
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
