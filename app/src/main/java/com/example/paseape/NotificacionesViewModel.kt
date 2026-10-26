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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    var mostrarTodasIncluyendoLeidas by mutableStateOf(false)
        private set

    var listaNotificaciones by mutableStateOf<List<NotificacionDto>>(emptyList())
        private set

    val cantidadNoLeidas: Int
        get() = listaNotificaciones.count { it.leido != true }

    companion object {
        private const val PREFS_NOTIF = "paseape_notificaciones_cache"
        private const val KEY_HISTORIAL_PREFIX = "historial_notificaciones_"
        private const val KEY_LEIDAS_LOCAL_PREFIX = "leidas_local_"
        private val gson = Gson()
        private var appContext: Context? = null
        private var currentUserKey: String = "default"

        private val _unreadCount = MutableStateFlow(0)
        val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

        fun inicializarContexto(context: Context, userUuid: String?) {
            appContext = context.applicationContext
            if (!userUuid.isNullOrBlank()) {
                currentUserKey = userUuid
            }
        }

        private fun claveHistorial(): String = "$KEY_HISTORIAL_PREFIX$currentUserKey"
        private fun claveLeidasLocal(): String = "$KEY_LEIDAS_LOCAL_PREFIX$currentUserKey"

        fun claveUnica(notif: NotificacionDto): String {
            return notif.uuid?.takeIf { it.isNotBlank() }
                ?: "${notif.tipoNotificacionId ?: 0}_${notif.referenciaId ?: 0}_${notif.creadoEl.orEmpty()}_${notif.titulo.orEmpty()}"
        }

        private fun leerHistorialLocal(): List<NotificacionDto> {
            val ctx = appContext ?: return emptyList()
            return runCatching {
                val prefs = ctx.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
                val json = prefs.getString(claveHistorial(), null) ?: return emptyList()
                val type = object : TypeToken<List<NotificacionDto>>() {}.type
                gson.fromJson<List<NotificacionDto>>(json, type).orEmpty()
            }.getOrDefault(emptyList())
        }

        private fun guardarHistorialLocal(lista: List<NotificacionDto>) {
            val ctx = appContext ?: return
            runCatching {
                val prefs = ctx.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
                prefs.edit().putString(claveHistorial(), gson.toJson(lista)).apply()
            }
        }

        private fun leerIdsLeidosLocal(): Set<String> {
            val ctx = appContext ?: return emptySet()
            return runCatching {
                val prefs = ctx.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
                prefs.getStringSet(claveLeidasLocal(), emptySet())?.toSet().orEmpty()
            }.getOrDefault(emptySet())
        }

        private fun guardarIdsLeidosLocal(ids: Set<String>) {
            val ctx = appContext ?: return
            runCatching {
                val prefs = ctx.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
                prefs.edit().putStringSet(claveLeidasLocal(), ids).apply()
            }
        }

        /**
         * Sincroniza las notificaciones no leídas que devuelve `GET /notificaciones`
         * con el historial local para conservar las ya leídas y mantener actualizado el contador.
         */
        private fun combinarConHistorialLocal(remotasNoLeidas: List<NotificacionDto>): List<NotificacionDto> {
            val historialPrevio = leerHistorialLocal()
            val leidasLocalmente = leerIdsLeidosLocal()
            val mapaRemotas = remotasNoLeidas.associateBy { claveUnica(it) }

            // 1. Las que vienen del backend siguen pendientes, salvo que se hayan marcado localmente
            val actualizadasDesdeServidor = remotasNoLeidas.map { remota ->
                val key = claveUnica(remota)
                val yaLeidaLocal = leidasLocalmente.contains(key) || remota.leido == true
                remota.copy(leido = yaLeidaLocal)
            }

            // 2. Las que estaban en historial previo y ya no vienen en GET /notificaciones
            //    significa que ya fueron marcadas como leídas en el backend (leido = true).
            val historicasLeidas = historialPrevio
                .filterNot { mapaRemotas.containsKey(claveUnica(it)) }
                .map { it.copy(leido = true) }

            val listaCompleta = (actualizadasDesdeServidor + historicasLeidas)
                .distinctBy { claveUnica(it) }
                .sortedByDescending { it.creadoEl.orEmpty() }

            guardarHistorialLocal(listaCompleta)
            _unreadCount.value = listaCompleta.count { it.leido != true }
            return listaCompleta
        }

        /**
         * Consulta el endpoint real GET /notificaciones en segundo plano para actualizar
         * la insignia numérica (badge) incluso cuando el usuario está en otra pantalla o pestaña.
         */
        suspend fun sincronizarContadorRemoto(
            context: Context,
            authHeader: String,
            userUuid: String?
        ) {
            if (authHeader.isBlank()) return
            inicializarContexto(context, userUuid)
            withContext(Dispatchers.IO) {
                runCatching {
                    val token = if (authHeader.startsWith("Bearer ")) authHeader else "Bearer $authHeader"
                    val response = RetrofitClient.apiService.listarNotificaciones(token)
                    if (response.isSuccessful && response.body() != null) {
                        val remotas = response.body()?.response.orEmpty()
                        combinarConHistorialLocal(remotas)
                    }
                }
            }
        }
    }

    fun cargarNotificaciones(
        authHeader: String,
        incluirLeidas: Boolean = mostrarTodasIncluyendoLeidas
    ) {
        if (authHeader.isBlank()) {
            errorMessage = "No se encontró una sesión activa."
            return
        }
        mostrarTodasIncluyendoLeidas = incluirLeidas
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
                        val todasCombinadas = combinarConHistorialLocal(remotas)
                        listaNotificaciones = if (mostrarTodasIncluyendoLeidas) {
                            todasCombinadas
                        } else {
                            // Mostrar las pendientes del servidor; si ya estaban en pantalla como leídas en esta vista,
                            // o si se activó la rueda dentada, se conserva la lista completa.
                            val noLeidas = todasCombinadas.filter { it.leido != true }
                            if (noLeidas.isNotEmpty()) noLeidas else todasCombinadas
                        }
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

    /**
     * Acción del botón de la rueda dentada (configuración):
     * Vuelve a mostrar la lista completa de notificaciones, incluidas todas las que ya fueron leídas.
     */
    fun mostrarListaCompletaConLeidas(authHeader: String, onMostradas: (Int) -> Unit = {}) {
        mostrarTodasIncluyendoLeidas = true
        val historial = leerHistorialLocal()
        if (historial.isNotEmpty()) {
            listaNotificaciones = historial
        }
        if (authHeader.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    val token = if (authHeader.startsWith("Bearer ")) authHeader else "Bearer $authHeader"
                    val response = apiService.listarNotificaciones(token)
                    if (response.isSuccessful && response.body() != null) {
                        val todas = combinarConHistorialLocal(response.body()?.response.orEmpty())
                        withContext(Dispatchers.Main) {
                            listaNotificaciones = todas
                            onMostradas(todas.size)
                        }
                        return@launch
                    }
                }
                withContext(Dispatchers.Main) {
                    onMostradas(listaNotificaciones.size)
                }
            }
        } else {
            onMostradas(listaNotificaciones.size)
        }
    }

    /**
     * Marca una notificación individual como leída al tocarla:
     * deja de estar iluminada y disminuye el contador de pendientes en 1.
     * Si era la última pendiente, sincroniza con PATCH /notificaciones/marcar-todas-leidas.
     */
    fun marcarNotificacionIndividualComoLeida(authHeader: String, notificacion: NotificacionDto) {
        if (notificacion.leido == true) return
        val targetKey = claveUnica(notificacion)

        val nuevosIdsLeidos = leerIdsLeidosLocal().toMutableSet().apply { add(targetKey) }
        guardarIdsLeidosLocal(nuevosIdsLeidos)

        val actualizada = listaNotificaciones.map { item ->
            if (claveUnica(item) == targetKey) item.copy(leido = true) else item
        }
        listaNotificaciones = actualizada

        val historialActualizado = leerHistorialLocal().map { item ->
            if (claveUnica(item) == targetKey) item.copy(leido = true) else item
        }
        guardarHistorialLocal(historialActualizado)

        val restantesNoLeidas = historialActualizado.count { it.leido != true }
        _unreadCount.value = restantesNoLeidas

        // Si ya no quedan notificaciones sin leer, sincronizar estado en el backend
        if (restantesNoLeidas == 0 && authHeader.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    val token = if (authHeader.startsWith("Bearer ")) authHeader else "Bearer $authHeader"
                    apiService.marcarTodasNotificacionesLeidas(token)
                }
            }
        }
    }

    /**
     * Llama a PATCH /notificaciones/marcar-todas-leidas solo si hay notificaciones pendientes de leer.
     * Al completarse, mantiene las notificaciones visibles en pantalla pero con estilo normal (leído = true)
     * y deja el contador en 0.
     */
    fun marcarTodasComoLeidas(
        authHeader: String,
        onSuccess: () -> Unit = {},
        onAlreadyRead: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (authHeader.isBlank()) return

        // No llamar nuevamente al endpoint si todas ya están marcadas como leídas
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
                        // Marcar todas como leídas sin eliminarlas de la lista visible
                        val marcadasVisibles = listaNotificaciones.map { it.copy(leido = true) }
                        listaNotificaciones = marcadasVisibles

                        val historialMarcado = leerHistorialLocal().map { it.copy(leido = true) }
                        val union = (marcadasVisibles + historialMarcado).distinctBy { claveUnica(it) }
                        guardarHistorialLocal(union)

                        val todosIds = union.map { claveUnica(it) }.toSet()
                        guardarIdsLeidosLocal(todosIds)

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
