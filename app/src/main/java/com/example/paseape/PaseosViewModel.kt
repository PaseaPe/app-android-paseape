package com.example.paseape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.CrearSolicitudInmediataReqDto
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.MascotaData
import com.example.paseape.data.model.PaseoResDto
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.data.repository.PaseosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

class PaseosViewModel : ViewModel() {

    private val repository = PaseosRepository()
    private val apiService = RetrofitClient.apiService

    // --- ESTADO GENERAL DE PASEOS ---
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var listaPaseos by mutableStateOf<List<PaseoResDto>>(emptyList())
        private set

    // Solicitudes inmediatas reales creadas en el backend (HU15)
    var solicitudesInmediatasCreadas by mutableStateOf<List<SolicitudPaseoResDto>>(emptyList())
        private set

    // --- DATOS DEL CLIENTE Y CATÁLOGOS PARA SOLICITUD INMEDIATA (MODO INDRIVE) ---
    var cargandoDatosCliente by mutableStateOf(false)
        private set
    var mascotasCliente by mutableStateOf<List<MascotaData>>(emptyList())
        private set
    var distritosDisponibles by mutableStateOf<List<DistritoParamDto>>(emptyList())
        private set

    // --- FORMULARIO SOLICITUD INMEDIATA (HU15 - MODO INDRIVE) ---
    var mascotasSeleccionadasUuids by mutableStateOf<Set<String>>(emptySet())
        private set
    var distritoSeleccionado by mutableStateOf<DistritoParamDto?>(null)
    var tipoDuracionPaseoId by mutableIntStateOf(2) // 1=30m, 2=60m, 3=90m, 4=120m
    var tipoMetodoPagoId by mutableIntStateOf(1) // 1=Yape/Plin, 2=Efectivo
    var tarifaHoraPen by mutableDoubleStateOf(20.0)
    var tarifaHoraInput by mutableStateOf("20.00")
        private set
    var direccionRecojo by mutableStateOf("")
    var referenciaDireccion by mutableStateOf("")

    var enviandoSolicitudInmediata by mutableStateOf(false)
        private set
    var errorSolicitudInmediata by mutableStateOf<String?>(null)
    var exitoSolicitudMensaje by mutableStateOf<String?>(null)

    private var datosInicializados = false

    init {
        cargarPaseosMock()
        viewModelScope.launch {
            PaseosRepository.solicitudesCreadasSesion.collect { lista ->
                solicitudesInmediatasCreadas = lista
            }
        }
    }

    val minutosSeleccionados: Int
        get() = when (tipoDuracionPaseoId) {
            1 -> 30
            2 -> 60
            3 -> 90
            4 -> 120
            else -> 60
        }

    val cantidadMascotasSeleccionadas: Int
        get() = max(1, mascotasSeleccionadasUuids.size)

    /**
     * Fórmula oficial del backend (SolicitudPaseoService):
     * monto_total = (tarifa_hora_pen / 60) * minutos * total_mascotas
     */
    val montoTotalEstimado: Double
        get() = (tarifaHoraPen / 60.0) * minutosSeleccionados * cantidadMascotasSeleccionadas

    fun calcularCostoPorDuracion(minutos: Int): Double {
        return (tarifaHoraPen / 60.0) * minutos * cantidadMascotasSeleccionadas
    }

    fun formatearMoneda(monto: Double): String {
        return String.format(Locale.US, "S/ %.2f", monto)
    }

    fun ajustarTarifaHora(delta: Double) {
        val nueva = (tarifaHoraPen + delta).coerceIn(5.0, 200.0)
        tarifaHoraPen = nueva
        tarifaHoraInput = String.format(Locale.US, "%.2f", nueva)
        errorSolicitudInmediata = null
    }

    fun actualizarTarifaDesdeTexto(texto: String) {
        val filtrado = texto.filter { it.isDigit() || it == '.' }
        if (filtrado.count { it == '.' } > 1) return
        tarifaHoraInput = filtrado
        val valor = filtrado.toDoubleOrNull()
        if (valor != null && valor > 0.0) {
            tarifaHoraPen = valor
            errorSolicitudInmediata = null
        }
    }

    fun toggleMascotaSeleccionada(uuid: String) {
        if (uuid.isBlank()) return
        val actual = mascotasSeleccionadasUuids.toMutableSet()
        if (actual.contains(uuid)) {
            if (actual.size > 1) {
                actual.remove(uuid)
            }
        } else {
            actual.add(uuid)
        }
        mascotasSeleccionadasUuids = actual
        errorSolicitudInmediata = null
    }

    fun inicializarDatosCliente(authHeader: String, userUuid: String?) {
        if (datosInicializados && mascotasCliente.isNotEmpty() && distritosDisponibles.isNotEmpty()) {
            return
        }
        datosInicializados = true
        cargandoDatosCliente = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Cargar distritos desde application-parameters
                val paramsRes = runCatching { apiService.getApplicationParameters(authHeader) }.getOrNull()
                val distritos = paramsRes?.body()?.response?.distritos.orEmpty()

                // 2. Cargar perfil del cliente y sus mascotas reales
                var mascotas: List<MascotaData> = emptyList()
                var direccionDefault = ""
                var distritoClienteDesc: String? = null

                if (!userUuid.isNullOrBlank()) {
                    val clienteRes = runCatching {
                        apiService.getClientePorUuid(authHeader, userUuid)
                    }.getOrNull()

                    val detalle = clienteRes?.body()?.extraerUsuarioCliente()?.detalleCliente
                    if (detalle != null) {
                        mascotas = detalle.mascotas.orEmpty().filter { !it.uuid.isNullOrBlank() }
                        direccionDefault = detalle.direccionReferencia.orEmpty()
                        distritoClienteDesc = detalle.distritoDescripcion
                    }
                }

                withContext(Dispatchers.Main) {
                    distritosDisponibles = distritos
                    mascotasCliente = mascotas

                    if (mascotasSeleccionadasUuids.isEmpty() && mascotas.isNotEmpty()) {
                        mascotas.firstOrNull()?.uuid?.let { primerUuid ->
                            mascotasSeleccionadasUuids = setOf(primerUuid)
                        }
                    }

                    if (direccionRecojo.isBlank() && direccionDefault.isNotBlank()) {
                        direccionRecojo = direccionDefault
                    }

                    if (distritoSeleccionado == null && distritos.isNotEmpty()) {
                        distritoSeleccionado = distritos.firstOrNull {
                            !distritoClienteDesc.isNullOrBlank() &&
                                it.descripcion.equals(distritoClienteDesc, ignoreCase = true)
                        } ?: distritos.first()
                    }
                    cargandoDatosCliente = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    cargandoDatosCliente = false
                }
            }
        }
    }

    fun enviarSolicitudInmediata(
        authHeader: String,
        onSuccess: (SolicitudPaseoResDto) -> Unit
    ) {
        errorSolicitudInmediata = null

        val uuids = mascotasSeleccionadasUuids.filter { it.isNotBlank() }
        if (uuids.isEmpty()) {
            errorSolicitudInmediata = "Selecciona al menos una mascota registrada para el paseo."
            return
        }

        val distrito = distritoSeleccionado
        if (distrito == null || distrito.id <= 0) {
            errorSolicitudInmediata = "Selecciona el distrito donde se realizará el paseo."
            return
        }

        if (tarifaHoraPen <= 0.0) {
            errorSolicitudInmediata = "La tarifa por hora ofrecida debe ser mayor a S/ 0.00."
            return
        }

        val direccionLimpia = direccionRecojo.trim()
        if (direccionLimpia.isBlank()) {
            errorSolicitudInmediata = "Ingresa la dirección de recojo."
            return
        }

        val req = CrearSolicitudInmediataReqDto(
            mascotaUuids = uuids,
            distritoId = distrito.id,
            tipoDuracionPaseoId = tipoDuracionPaseoId,
            tipoMetodoPagoId = tipoMetodoPagoId,
            tarifaHoraPen = tarifaHoraPen,
            direccionRecojo = direccionLimpia,
            referenciaDireccion = referenciaDireccion.trim().ifBlank { null }
        )

        enviandoSolicitudInmediata = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.crearSolicitudInmediata(authHeader, req)
            withContext(Dispatchers.Main) {
                enviandoSolicitudInmediata = false
                result.onSuccess { nuevaSolicitud ->
                    exitoSolicitudMensaje =
                        "¡Solicitud ${nuevaSolicitud.codigoReserva.orEmpty()} creada! Buscando paseadores en ${nuevaSolicitud.distritoDescripcion ?: distrito.descripcion}."
                    onSuccess(nuevaSolicitud)
                }.onFailure { err ->
                    errorSolicitudInmediata = err.message ?: "No se pudo registrar la solicitud inmediata."
                }
            }
        }
    }

    // --- MOCK DATA (Conservado hasta que se implemente HU21 GET /cliente/solicitudes) ---
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
}
