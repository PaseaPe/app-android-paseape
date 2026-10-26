package com.example.paseape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.CrearSolicitudInmediataReqDto
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.MascotaData
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.data.repository.PaseosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

class PaseosViewModel : ViewModel() {

    private val repository = PaseosRepository()
    private val apiService = RetrofitClient.apiService

    // --- BANDEJA DE SOLICITUDES DEL CLIENTE (HU21 - API REAL) ---
    var cargandoBandeja by mutableStateOf(false)
        private set
    var errorBandeja by mutableStateOf<String?>(null)
        private set

    var solicitudesActivas by mutableStateOf<List<SolicitudPaseoResDto>>(emptyList())
        private set
    var totalActivos by mutableLongStateOf(0L)
        private set

    var solicitudesPendientes by mutableStateOf<List<SolicitudPaseoResDto>>(emptyList())
        private set
    var totalPendientes by mutableLongStateOf(0L)
        private set

    var solicitudesHistorial by mutableStateOf<List<SolicitudPaseoResDto>>(emptyList())
        private set
    var totalHistorial by mutableLongStateOf(0L)
        private set

    var cancelandoUuid by mutableStateOf<String?>(null)
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

    fun cargarBandejaCliente(authHeader: String) {
        if (authHeader.isBlank()) return
        cargandoBandeja = true
        errorBandeja = null

        viewModelScope.launch(Dispatchers.IO) {
            val defActivos = async { repository.obtenerBandejaCliente(authHeader, "ACTIVOS", 0, 30) }
            val defPendientes = async { repository.obtenerBandejaCliente(authHeader, "PENDIENTES", 0, 30) }
            val defHistorial = async { repository.obtenerBandejaCliente(authHeader, "HISTORIAL", 0, 30) }

            val resActivos = defActivos.await()
            val resPendientes = defPendientes.await()
            val resHistorial = defHistorial.await()

            withContext(Dispatchers.Main) {
                cargandoBandeja = false

                resActivos.onSuccess { page ->
                    val items = page.items.orEmpty()
                    solicitudesActivas = items
                    totalActivos = page.totalElements ?: items.size.toLong()
                }

                resPendientes.onSuccess { page ->
                    val items = page.items.orEmpty()
                    solicitudesPendientes = items
                    totalPendientes = page.totalElements ?: items.size.toLong()
                }

                resHistorial.onSuccess { page ->
                    val items = page.items.orEmpty()
                    solicitudesHistorial = items
                    totalHistorial = page.totalElements ?: items.size.toLong()
                }

                if (resActivos.isFailure && resPendientes.isFailure && resHistorial.isFailure) {
                    errorBandeja = resPendientes.exceptionOrNull()?.message
                        ?: "No se pudo cargar la bandeja de paseos."
                }
            }
        }
    }

    fun cancelarSolicitud(
        authHeader: String,
        solicitudUuid: String,
        motivo: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (solicitudUuid.isBlank()) return
        cancelandoUuid = solicitudUuid

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.cancelarSolicitudCliente(authHeader, solicitudUuid, motivo)
            withContext(Dispatchers.Main) {
                cancelandoUuid = null
                result.onSuccess { cancelada ->
                    exitoSolicitudMensaje = "La solicitud ${cancelada.codigoReserva.orEmpty()} fue cancelada."
                    cargarBandejaCliente(authHeader)
                    onSuccess()
                }.onFailure { err ->
                    onError(err.message ?: "No se pudo cancelar la solicitud.")
                }
            }
        }
    }

    fun inicializarDatosCliente(authHeader: String, userUuid: String?) {
        cargarBandejaCliente(authHeader)

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
                    cargarBandejaCliente(authHeader)
                    onSuccess(nuevaSolicitud)
                }.onFailure { err ->
                    errorSolicitudInmediata = err.message ?: "No se pudo registrar la solicitud inmediata."
                }
            }
        }
    }
}
