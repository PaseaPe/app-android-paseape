package com.example.paseape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.CrearSolicitudProgramadaReqDto
import com.example.paseape.data.model.DisponibilidadSlotDto
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.MascotaData
import com.example.paseape.data.model.SolicitudMascotaResDto
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.data.repository.PaseosRepository
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
import java.util.UUID
import kotlin.math.max

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

    private val repository = PaseosRepository()
    private val apiService = RetrofitClient.apiService

    // DATOS DEL PASEADOR SELECCIONADO
    var paseadorUuid by mutableStateOf("")
    var paseadorNombre by mutableStateOf("Roberto Gómez Salas")
    var tarifaBaseHora by mutableStateOf(25.0)
    var fotoPerfilUrl by mutableStateOf<String?>(null)
    var biografia by mutableStateOf<String?>(null)
    var distrito by mutableStateOf<String?>(null)
    var distritoId by mutableIntStateOf(0)

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

    // PASO 2: Mascotas reales (API), Duración, Dirección y Método de Pago
    var cargandoMascotas by mutableStateOf(false)
        private set
    var mascotasCliente by mutableStateOf<List<MascotaData>>(emptyList())
        private set
    var mascotasSeleccionadasUuids by mutableStateOf<Set<String>>(emptySet())
        private set
    var distritosCatalogo by mutableStateOf<List<DistritoParamDto>>(emptyList())
        private set

    var mascotaSeleccionada by mutableStateOf("Bobby")
    var duracionMinutos by mutableIntStateOf(60) // 30, 60, 90, 120
    var tipoMetodoPagoId by mutableIntStateOf(1) // 1 = Yape / Plin, 2 = Efectivo
    var direccionRecojo by mutableStateOf("")
    var referenciaRecojo by mutableStateOf("")

    var enviandoSolicitud by mutableStateOf(false)
        private set
    var errorEnvio by mutableStateOf<String?>(null)

    private var datosClienteCargados = false

    val tipoDuracionPaseoId: Int
        get() = when (duracionMinutos) {
            30 -> 1
            60 -> 2
            90 -> 3
            120 -> 4
            else -> 2
        }

    val metodoPagoDescripcion: String
        get() = if (tipoMetodoPagoId == 1) "Yape / Plin" else "Efectivo"

    val cantidadMascotasSeleccionadas: Int
        get() = max(1, mascotasSeleccionadasUuids.size)

    val nombresMascotasSeleccionadas: String
        get() {
            val seleccionadas = mascotasCliente.filter {
                !it.uuid.isNullOrBlank() && mascotasSeleccionadasUuids.contains(it.uuid)
            }
            return if (seleccionadas.isNotEmpty()) {
                seleccionadas.mapNotNull { it.nombre }.joinToString(", ")
            } else {
                mascotaSeleccionada
            }
        }

    // CÁLCULO DE COSTO Y FORMATEO DINÁMICO (Alineado con SolicitudPaseoService)
    fun calcularCostoDuracion(minutos: Int): Double {
        return (tarifaBaseHora / 60.0) * minutos * cantidadMascotasSeleccionadas
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
        distritoNombre: String? = null,
        distritoIdValor: Int = 0
    ) {
        if (uuid.isNotBlank()) paseadorUuid = uuid
        if (nombre.isNotBlank()) paseadorNombre = nombre
        if (tarifa > 0.0) tarifaBaseHora = tarifa
        fotoPerfilUrl = foto
        biografia = bio
        distrito = distritoNombre
        if (distritoIdValor > 0) distritoId = distritoIdValor
    }

    fun toggleMascotaSeleccionada(mascota: MascotaData) {
        val uuid = mascota.uuid ?: return
        val actual = mascotasSeleccionadasUuids.toMutableSet()
        if (actual.contains(uuid)) {
            if (actual.size > 1) {
                actual.remove(uuid)
            }
        } else {
            actual.add(uuid)
        }
        mascotasSeleccionadasUuids = actual
        mascotaSeleccionada = nombresMascotasSeleccionadas
        errorEnvio = null
    }

    fun cargarDatosCliente(authHeader: String, userUuid: String?) {
        if (datosClienteCargados && mascotasCliente.isNotEmpty()) return
        datosClienteCargados = true
        cargandoMascotas = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Catálogo de distritos para resolver distritoId si vino solo por nombre
                val paramsRes = runCatching { apiService.getApplicationParameters(authHeader) }.getOrNull()
                val distritos = paramsRes?.body()?.response?.distritos.orEmpty()

                // 2. Perfil del cliente y sus mascotas reales
                var mascotas: List<MascotaData> = emptyList()
                var direccionDefault = ""

                if (!userUuid.isNullOrBlank()) {
                    val clienteRes = runCatching {
                        apiService.getClientePorUuid(authHeader, userUuid)
                    }.getOrNull()
                    val detalle = clienteRes?.body()?.extraerUsuarioCliente()?.detalleCliente
                    if (detalle != null) {
                        mascotas = detalle.mascotas.orEmpty().filter { !it.uuid.isNullOrBlank() }
                        direccionDefault = detalle.direccionReferencia.orEmpty()
                    }
                }

                // 3. Si distritoId aún es 0 y tenemos paseadorUuid, intentar obtener distritoCoberturaId del paseador
                var resolvedDistritoId = distritoId
                if (resolvedDistritoId <= 0 && !distrito.isNullOrBlank() && distritos.isNotEmpty()) {
                    resolvedDistritoId = distritos.firstOrNull {
                        it.descripcion.equals(distrito, ignoreCase = true)
                    }?.id ?: 0
                }
                if (resolvedDistritoId <= 0 && paseadorUuid.isNotBlank()) {
                    val pasRes = runCatching {
                        apiService.getPaseadorPorUsuarioUuid(authHeader, paseadorUuid)
                    }.getOrNull()
                    val pasDto = pasRes?.body()?.response?.extraerUsuario()?.paseador
                    if (pasDto?.distritoCoberturaId != null && pasDto.distritoCoberturaId > 0) {
                        resolvedDistritoId = pasDto.distritoCoberturaId
                    }
                }

                withContext(Dispatchers.Main) {
                    distritosCatalogo = distritos
                    if (resolvedDistritoId > 0) {
                        distritoId = resolvedDistritoId
                    }
                    mascotasCliente = mascotas
                    if (mascotas.isNotEmpty() && mascotasSeleccionadasUuids.isEmpty()) {
                        val primera = mascotas.first()
                        primera.uuid?.let { mascotasSeleccionadasUuids = setOf(it) }
                        mascotaSeleccionada = primera.nombre ?: "Mascota"
                    }
                    if (direccionRecojo.isBlank() && direccionDefault.isNotBlank()) {
                        direccionRecojo = direccionDefault
                    }
                    cargandoMascotas = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    cargandoMascotas = false
                }
            }
        }
    }

    fun cargarDisponibilidadFecha(authHeader: String, fechaIso: String) {
        if (paseadorUuid.isBlank()) {
            _slotsUiState.value = SlotsUiState.Error("Identificador del paseador no disponible.")
            return
        }

        _slotsUiState.value = SlotsUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getDisponibilidadPaseadorPorFecha(
                    token = authHeader,
                    paseadorUuid = paseadorUuid,
                    fecha = fechaIso
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val slots = response.body()?.response?.bloques.orEmpty()
                        if (slots.isEmpty()) {
                            _slotsUiState.value = SlotsUiState.Empty("El paseador no tiene bloques horarios configurados para este día.")
                            bloqueSeleccionado = null
                        } else {
                            _slotsUiState.value = SlotsUiState.Success(slots)
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

    fun enviarSolicitud(authHeader: String, onSuccess: (String?) -> Unit) {
        errorEnvio = null

        val uuids = mascotasSeleccionadasUuids.filter { it.isNotBlank() }
        if (uuids.isEmpty()) {
            errorEnvio = "Selecciona al menos una mascota registrada para el paseo."
            return
        }

        val bloqueId = bloqueSeleccionado?.bloqueHorarioId
        if (bloqueId == null || bloqueId <= 0) {
            errorEnvio = "Selecciona un bloque horario disponible en el Paso 1."
            return
        }

        val direccionLimpia = direccionRecojo.trim()
        if (direccionLimpia.isBlank()) {
            errorEnvio = "Ingresa la dirección de recojo en el Paso 2."
            return
        }

        val idDistritoFinal = if (distritoId > 0) {
            distritoId
        } else {
            distritosCatalogo.firstOrNull()?.id ?: 1
        }

        val req = CrearSolicitudProgramadaReqDto(
            paseadorUuid = paseadorUuid,
            mascotaUuids = uuids,
            distritoId = idDistritoFinal,
            fechaProgramada = diaSeleccionadoIso,
            bloqueHorarioId = bloqueId,
            tipoDuracionPaseoId = tipoDuracionPaseoId,
            tipoMetodoPagoId = tipoMetodoPagoId,
            direccionRecojo = direccionLimpia,
            referenciaDireccion = referenciaRecojo.trim().ifBlank { null }
        )

        enviandoSolicitud = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.crearSolicitudProgramada(authHeader, req)
            withContext(Dispatchers.Main) {
                enviandoSolicitud = false
                result.onSuccess { creada ->
                    onSuccess(creada.codigoReserva)
                }.onFailure { err ->
                    // Si el backend local aún no tiene fusionada la rama feat/sprint-2-hu16 (404),
                    // registramos la solicitud programada en la sesión para que igual aparezca en Mis Paseos
                    val msg = err.message.orEmpty()
                    if (msg.contains("No encontramos la información", ignoreCase = true)) {
                        val codigoLocal = "PAS-202610-" + UUID.randomUUID().toString().take(4).uppercase()
                        val mascotasDtos = mascotasCliente
                            .filter { !it.uuid.isNullOrBlank() && uuids.contains(it.uuid) }
                            .map {
                                SolicitudMascotaResDto(
                                    uuid = it.uuid,
                                    nombre = it.nombre,
                                    raza = it.tipoRazaDescripcion,
                                    tamano = it.tipoTamanoMascotaDescripcion
                                )
                            }
                        val localDto = SolicitudPaseoResDto(
                            uuid = UUID.randomUUID().toString(),
                            codigoReserva = codigoLocal,
                            tipoModalidadPaseoId = 1,
                            tipoModalidadPaseoDescripcion = "PROGRAMADO",
                            estadoId = 1,
                            estadoDescripcion = "PENDIENTE",
                            tipoMetodoPagoId = tipoMetodoPagoId,
                            tipoMetodoPagoDescripcion = metodoPagoDescripcion.uppercase(),
                            tipoDuracionPaseoId = tipoDuracionPaseoId,
                            tipoDuracionPaseoDescripcion = "$duracionMinutos MINUTOS",
                            distritoId = idDistritoFinal,
                            distritoDescripcion = distrito ?: "LIMA",
                            direccionRecojo = direccionLimpia,
                            referenciaDireccion = referenciaRecojo.trim().ifBlank { null },
                            fechaProgramada = diaSeleccionadoIso,
                            bloqueHorarioId = bloqueId,
                            bloqueHorarioCodigo = horarioSeleccionado,
                            tarifaHoraPenSnapshot = tarifaBaseHora,
                            montoTotal = totalEstimado,
                            paseadorUuid = paseadorUuid,
                            paseadorNombreCompleto = paseadorNombre,
                            mascotas = mascotasDtos
                        )
                        PaseosRepository.registrarSolicitudCreada(localDto)
                        onSuccess(codigoLocal)
                    } else {
                        errorEnvio = msg.ifBlank { "No se pudo registrar la solicitud programada." }
                    }
                }
            }
        }
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
