package com.example.paseape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.paseape.data.model.NotificacionDto

class NotificacionesViewModel : ViewModel() {

    // --- CÓDIGO REAL (Comentado para pruebas con Mock Data) ---
    // var isLoading by mutableStateOf(false)
    //     private set
    // var errorMessage by mutableStateOf<String?>(null)
    //     private set

    var listaNotificaciones by mutableStateOf<List<NotificacionDto>>(emptyList())
        private set

    init {
        // Carga de datos de prueba para la interfaz
        cargarNotificacionesMock()

        // CUANDO QUIERAS CONECTAR LA API REAL, DESCOMENTA ESTA LÍNEA:
        // cargarNotificacionesReales(token = "TU_TOKEN_AQUÍ", usuarioId = 1L)
    }

    // --- MOCK DATA (Activo para pruebas visuales) ---
    fun cargarNotificacionesMock() {
        listaNotificaciones = listOf(
            NotificacionDto(
                id = 101L,

                titulo = "¡Paseo confirmado!",
                mensaje = "Tu paseo para Luna ha sido aceptado por el paseador.",
                fechaCreacion = "2026-10-19T10:00:00",
                leido = false
            ),
            NotificacionDto(
                id = 102L,

                titulo = "Paseo en curso",
                mensaje = "Max ya está paseando con su paseador asignado.",
                fechaCreacion = "2026-10-19T11:30:00",
                leido = true
            )
        )
    }

    /*
    // --- FUNCIÓN REAL DE CONEXIÓN A LA RED ---
    fun cargarNotificacionesReales(token: String, usuarioId: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.apiService.obtenerNotificaciones("Bearer $token", usuarioId)
                if (response.isSuccessful && response.body() != null) {
                    listaNotificaciones = response.body()!!
                } else {
                    errorMessage = "Error al obtener notificaciones"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            }
            isLoading = false
        }
    }
    */
}