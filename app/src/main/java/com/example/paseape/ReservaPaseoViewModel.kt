package com.example.paseape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ReservaPaseoViewModel : ViewModel() {

    // DATOS DE PRUEBA / SELECCIONADOS
    var paseadorNombre by mutableStateOf("Roberto Gómez Salas")
    var tarifaBaseHora by mutableStateOf(25.0)

    // PASO 1: Horario
    var diaSeleccionado by mutableStateOf("Mié 20")
    var horarioSeleccionado by mutableStateOf("08:00 - 09:00")

    // PASO 2: Mascota y Configuración
    var mascotaSeleccionada by mutableStateOf("Bobby")
    var duracionMinutos by mutableStateOf(60) // 30, 60, 90, 120
    var direccionRecojo by mutableStateOf("Calle Las Begonias 450, Dpto 302, San Isidro")
    var referenciaRecojo by mutableStateOf("Tocar timbre 302, frente al parque")

    // CÁLCULO DE COSTO TOTAL
    val totalEstimado: Double
        get() = (tarifaBaseHora / 60) * duracionMinutos

    fun enviarSolicitud(onSuccess: () -> Unit) {
        // AQUÍ SE CONECTARÁ CON EL BACKEND (RETROFIT) MÁS ADELANTE
        onSuccess()
    }
}