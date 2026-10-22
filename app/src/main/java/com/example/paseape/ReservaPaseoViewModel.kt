package com.example.paseape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.DisponibilidadSlotDto
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class DiaOpcion(
    val fechaIso: String,
    val etiqueta: String,
    val numero: String,
    val esHoy: Boolean = false
)

sealed interface SlotsUiState {
    data object Loading : SlotsUiState
    data class Success(val slots: List<DisponibilidadSlotDto>) : SlotsUiState
    data class Empty(val mensaje: String) : SlotsUiState
    data class Error(val mensaje: String) : SlotsUiState
}

class ReservaPaseoViewModel : ViewModel() {

    // DATOS DEL PASEADOR SELECCIONADO
    var paseadorUuid by mutableStateOf("")
    var paseadorNombre by mutableStateOf("Roberto Gómez Salas")
    var tarifaBaseHora by mutableStateOf(25.0)
    var fotoPerfilUrl by mutableStateOf<String?>(null)
    var biografia by mutableStateOf<String?>(null)
    var distrito by mutableStateOf<String?>(null)

    // PASO 1: Fecha y Bloques
    val diasDisponibles: List<DiaOpcion> = generarProximosDias()
    var diaSeleccionadoIso by mutableStateOf(diasDisponibles.firstOrNull()?.fechaIso ?: "")
    var diaSeleccionadoEtiqueta by mutableStateOf(
        diasDisponibles.firstOrNull()?.let { "${it.etiqueta} ${it.numero}" } ?: "Hoy"
    )

    val diaSeleccionado: String
        get() = diaSeleccionadoEtiqueta

    val horarioSeleccionado: String
        get() = bloqueSeleccionado?.let {
            val ini = it.horaInicio?.take(5).orEmpty()
            val fin = it.horaFin?.take(5).orEmpty()
            if (ini.isNotBlank() && fin.isNotBlank()) "$ini - $fin" else it.bloqueCodigo.orEmpty()
        } ?: "08:00 - 09:00"

    private val _slotsUiState = MutableStateFlow<SlotsUiState>(SlotsUiState.Loading)
    val slotsUiState: StateFlow<SlotsUiState> = _slotsUiState.asStateFlow()

    var bloqueSeleccionado by mutableStateOf<DisponibilidadSlotDto?>(null)

    // PASO 2: Mascota y Configuración
    var mascotaSeleccionada by mutableStateOf("Bobby")
    var duracionMinutos by mutableIntStateOf(60) // 30, 60, 90, 120
    var direccionRecojo by mutableStateOf("Calle Las Begonias 450, Dpto 302, San Isidro")
    var referenciaRecojo by mutableStateOf("Tocar timbre 302, frente al parque")

    // CÁLCULO DE COSTO Y FORMATEO DINÁMICO
    fun calcularCostoDuracion(minutos: Int): Double {
        return (tarifaBaseHora / 60.0) * minutos
    }

    fun formatearPrecio(monto: Double): String {
        return String.format(Locale.US, "S/ %.2f", monto)
    }

    val totalEstimado: Double
        get() = calcularCostoDuracion(duracionMinutos)

    val totalEstimadoFormateado: String
        get() = formatearPrecio(totalEstimado)

    fun inicializarPaseador(
        uuid: String,
        nombre: String,
        tarifa: Double,
        foto: String? = null,
        bio: String? = null,
        distritoNombre: String? = null
    ) {
        if (uuid.isNotBlank()) paseadorUuid = uuid
        if (nombre.isNotBlank()) paseadorNombre = nombre
        if (tarifa > 0.0) tarifaBaseHora = tarifa
        fotoPerfilUrl = foto
        biografia = bio
        distrito = distritoNombre
    }

    fun cargarDisponibilidadFecha(authHeader: String, fechaIso: String) {
        if (paseadorUuid.isBlank()) {
            _slotsUiState.value = SlotsUiState.Error("Identificador del paseador no disponible.")
            return
        }

        _slotsUiState.value = SlotsUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getPaseadorPorUsuarioUuid(
                    token = authHeader,
                    usuarioUuid = paseadorUuid,
                    fecha = fechaIso
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val slots = response.body()?.response?.disponibilidad?.slots.orEmpty()
                        if (slots.isEmpty()) {
                            _slotsUiState.value = SlotsUiState.Empty("El paseador no tiene bloques horarios configurados para este día.")
                            bloqueSeleccionado = null
                        } else {
                            _slotsUiState.value = SlotsUiState.Success(slots)
                            // Seleccionar automáticamente el primer bloque disponible
                            bloqueSeleccionado = slots.firstOrNull { it.disponible == true }
                        }
                    } else {
                        val rawError = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        val errorMsg = rawError ?: "HTTP ${response.code()} ${response.message()}".trim()
                        _slotsUiState.value = SlotsUiState.Error("Error al consultar horarios: $errorMsg")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _slotsUiState.value = SlotsUiState.Error("Error de red: ${e.localizedMessage ?: "No se pudo conectar"}")
                }
            }
        }
    }

    fun seleccionarDia(dia: DiaOpcion, authHeader: String) {
        diaSeleccionadoIso = dia.fechaIso
        diaSeleccionadoEtiqueta = "${dia.etiqueta} ${dia.numero}"
        cargarDisponibilidadFecha(authHeader, dia.fechaIso)
    }

    fun seleccionarBloque(slot: DisponibilidadSlotDto) {
        if (slot.disponible == true) {
            bloqueSeleccionado = slot
        }
    }

    fun enviarSolicitud(onSuccess: () -> Unit) {
        // Enviar solicitud (mock de confirmación hasta HU16 de backend)
        onSuccess()
    }

    companion object {
        fun generarProximosDias(): List<DiaOpcion> {
            val zonaLima = try {
                ZoneId.of("America/Lima")
            } catch (_: Exception) {
                ZoneId.systemDefault()
            }
            val hoy = LocalDate.now(zonaLima)
            return (0..6).map { offset ->
                val fecha = hoy.plusDays(offset.toLong())
                val nombreCorto = when (fecha.dayOfWeek) {
                    DayOfWeek.MONDAY -> "Lun"
                    DayOfWeek.TUESDAY -> "Mar"
                    DayOfWeek.WEDNESDAY -> "Mié"
                    DayOfWeek.THURSDAY -> "Jue"
                    DayOfWeek.FRIDAY -> "Vie"
                    DayOfWeek.SATURDAY -> "Sáb"
                    DayOfWeek.SUNDAY -> "Dom"
                }
                DiaOpcion(
                    fechaIso = fecha.toString(),
                    etiqueta = nombreCorto,
                    numero = fecha.dayOfMonth.toString(),
                    esHoy = offset == 0
                )
            }
        }
    }
}
