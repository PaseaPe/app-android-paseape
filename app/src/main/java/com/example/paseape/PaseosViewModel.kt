package com.example.paseape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.paseape.data.model.PaseoResDto

class PaseosViewModel : ViewModel() {

    // --- CÓDIGO REAL (Comentado para usar Mock Data por ahora) ---
    // private val repository = PaseosRepository()
    // var isLoading by mutableStateOf(false)
    //     private set
    // var errorMessage by mutableStateOf<String?>(null)
    //     private set

    var listaPaseos by mutableStateOf<List<PaseoResDto>>(emptyList())
        private set

    init {
        // Actualmente cargamos los datos falsos para probar la interfaz
        cargarPaseosMock()

        // CUANDO QUIERES CONECTAR LA API REAL, DESCOMENTA ESTO:
        // cargarPaseosReales(token = "TU_TOKEN_AQUÍ", clienteId = 1L)
    }

    // --- MOCK DATA (Activo para pruebas visuales) ---
    fun cargarPaseosMock() {
        listaPaseos = listOf(
            PaseoResDto(
                id = 1,
                clienteId = 101L,
                distritoId = 1,
                mascotaId = 10,
                mascotaNombre = "Luna",
                paseadorId = 5L,
                fechaProgramada = "2026-10-25 10:00 AM",
                tarifaPen = 25.0,
                estadoPaseoId = 1
            ),
            PaseoResDto(
                id = 2,
                clienteId = 101L,
                distritoId = 1,
                mascotaId = 11,
                mascotaNombre = "Max",
                paseadorId = 6L,
                fechaProgramada = "2026-10-26 03:30 PM",
                tarifaPen = 30.0,
                estadoPaseoId = 2
            ),
            PaseoResDto(
                id = 3,
                clienteId = 101L,
                distritoId = 2,
                mascotaId = 12,
                mascotaNombre = "Rocky",
                paseadorId = 5L,
                fechaProgramada = "2026-10-27 09:00 AM",
                tarifaPen = 20.0,
                estadoPaseoId = 3
            )
        )
    }

    /*
    // --- FUNCIÓN REAL DE CONEXIÓN (Descomentar cuando conectes el servidor) ---
    fun cargarPaseosReales(token: String, clienteId: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = repository.obtenerPaseos(token, clienteId)
            result.onSuccess { paseos ->
                listaPaseos = paseos
            }.onFailure { error ->
                errorMessage = error.localizedMessage
            }

            isLoading = false
        }
    }
    */
}