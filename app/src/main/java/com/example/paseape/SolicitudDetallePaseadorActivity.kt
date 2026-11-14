package com.example.paseape

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.paseape.data.model.CancelarSolicitudReqDto
import com.example.paseape.data.model.RechazarSolicitudRequest
import com.example.paseape.data.model.ResponseDto
import com.example.paseape.data.model.SolicitudMascotaResDto
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.PaseaPETheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Pantalla de detalle de una solicitud de paseo del lado del paseador.
 *
 * Recibe el uuid desde la bandeja y arma la pantalla solo con rutas del API:
 * - `GET /solicitudes/{uuid}` como fuente principal (paseador ya asignado).
 * - `GET /paseador/solicitudes/disponibles` como respaldo cuando la solicitud
 *   todavía está en BUSCANDO (el detalle responde 400 en ese estado).
 * - `GET /cliente/{uuid}` para el teléfono y `GET /mascotas/mascota/{uuid}`
 *   para completar la ficha de cada mascota.
 *
 * Permite aceptar/rechazar (PENDIENTE), tomar/descartar (BUSCANDO) e iniciar o
 * cancelar (ACEPTADA).
 */
class SolicitudDetallePaseadorActivity : ComponentActivity() {

    companion object {
        /** UUID de la solicitud seleccionada en la bandeja. */
        const val EXTRA_SOLICITUD_UUID = "extra_solicitud_uuid"
        private const val TAG = "SOLICITUD_DETALLE"
    }

    private var uiState by mutableStateOf<SolicitudDetalleUiState>(SolicitudDetalleUiState.Loading)
    private var accionEnCurso by mutableStateOf<AccionPaseo?>(null)
    private var authHeader: String = ""
    private var solicitudUuid: String? = null
    private var huboCambios = false

    /** Al volver del paseo activo se cierra el detalle para refrescar la bandeja. */
    private val paseoActivoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        huboCambios = true
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authHeader = leerAuthHeader()
        solicitudUuid = intent.getStringExtra(EXTRA_SOLICITUD_UUID)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        cargarDetalle()

        setContent {
            PaseaPETheme {
                SolicitudDetallePaseadorScreen(
                    uiState = uiState,
                    accionEnCurso = accionEnCurso,
                    onBack = { finish() },
                    onRetry = { cargarDetalle() },
                    onAceptar = { aceptar() },
                    onRechazar = { rechazar(it) },
                    onIniciar = { iniciar() },
                    onCancelar = { cancelar(it) },
                    onTomar = { tomar() },
                    onDescartar = { descartar() },
                    onAbrirMapa = { direccion -> abrirMapa(direccion) }
                )
            }
        }
    }

    override fun finish() {
        if (huboCambios) setResult(RESULT_OK)
        super.finish()
    }

    private fun leerAuthHeader(): String {
        val prefs = getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE)
        val legacyPrefs = getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
            ?: prefs.getString("JWT_TOKEN", null)
            ?: legacyPrefs.getString("auth_token", null)
            ?: legacyPrefs.getString("JWT_TOKEN", null)

        return token
            ?.takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
            .orEmpty()
    }

    private fun cargarDetalle() {
        val uuid = solicitudUuid
        if (uuid.isNullOrBlank()) {
            uiState = SolicitudDetalleUiState.Error("No se encontró el identificador de la solicitud.")
            return
        }
        if (authHeader.isBlank()) {
            uiState = SolicitudDetalleUiState.Error("Tu sesión expiró. Inicia sesión nuevamente.")
            return
        }

        uiState = SolicitudDetalleUiState.Loading
        lifecycleScope.launch {
            try {
                when (val carga = cargarSolicitudBase(uuid)) {
                    is CargaSolicitud.Fallo -> {
                        uiState = SolicitudDetalleUiState.Error(carga.mensaje)
                        return@launch
                    }

                    CargaSolicitud.NoDisponible -> {
                        uiState = SolicitudDetalleUiState.Error(
                            "Esta solicitud ya no está disponible. Es posible que otro " +
                                "paseador la haya tomado o que el cliente la haya cancelado."
                        )
                        return@launch
                    }

                    is CargaSolicitud.Ok -> {
                        val solicitud = enriquecerSolicitud(carga.solicitud, carga.desdeDisponibles)
                        uiState = SolicitudDetalleUiState.Success(
                            solicitud = solicitud,
                            mascotas = mascotasDetalle(solicitud)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al cargar el detalle de la solicitud", e)
                uiState = SolicitudDetalleUiState.Error(
                    ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK)
                )
            }
        }
    }

    /** Resultado de resolver la solicitud con la ruta de detalle o su respaldo. */
    private sealed interface CargaSolicitud {
        data class Ok(
            val solicitud: SolicitudPaseoResDto,
            val desdeDisponibles: Boolean
        ) : CargaSolicitud

        data object NoDisponible : CargaSolicitud

        data class Fallo(val mensaje: String) : CargaSolicitud
    }

    /**
     * Fuente principal: `GET /solicitudes/{uuid}`. Si el backend rechaza la
     * consulta (solicitud aún en BUSCANDO, sin paseador asignado), se busca el
     * mismo uuid en `GET /paseador/solicitudes/disponibles`.
     */
    private suspend fun cargarSolicitudBase(uuid: String): CargaSolicitud {
        val detalle = withContext(Dispatchers.IO) {
            RetrofitClient.apiService.getSolicitudDetalle(authHeader, uuid)
        }
        if (detalle.isSuccessful) {
            val solicitud = detalle.body()?.response
            if (solicitud != null) {
                return CargaSolicitud.Ok(solicitud, desdeDisponibles = false)
            }
        } else {
            Log.w(TAG, "Detalle no disponible (HTTP ${detalle.code()}); se busca en disponibles")
        }

        val disponibles = withContext(Dispatchers.IO) {
            RetrofitClient.apiService.getSolicitudesDisponibles(authHeader)
        }
        if (!disponibles.isSuccessful) {
            return CargaSolicitud.Fallo(
                ApiErrorMapper.fromResponse(disponibles, ApiErrorContext.WALK)
            )
        }

        val solicitud = disponibles.body()?.response?.firstOrNull { it.uuid == uuid }
            ?: return CargaSolicitud.NoDisponible
        return CargaSolicitud.Ok(solicitud, desdeDisponibles = true)
    }

    /**
     * Completa la solicitud con datos que el detalle o la bandeja no traen:
     * teléfono del cliente y, cuando viene de `disponibles`, la ficha de cada
     * mascota (precauciones, edad, peso, energía).
     */
    private suspend fun enriquecerSolicitud(
        base: SolicitudPaseoResDto,
        desdeDisponibles: Boolean
    ): SolicitudPaseoResDto {
        val telefono = base.clienteTelefono?.takeIf { it.isNotBlank() }
            ?: obtenerTelefonoCliente(base.clienteUuid)
        val mascotas = if (desdeDisponibles) {
            enriquecerMascotas(base.mascotas.orEmpty())
        } else {
            base.mascotas
        }
        return base.copy(clienteTelefono = telefono, mascotas = mascotas)
    }

    /** `GET /cliente/{uuid}` → `telefono` del usuario cliente. */
    private suspend fun obtenerTelefonoCliente(clienteUuid: String?): String? {
        val uuid = clienteUuid?.takeIf { it.isNotBlank() } ?: return null
        return try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getClientePorUuid(authHeader, uuid)
            }
            if (!response.isSuccessful) {
                Log.w(TAG, "No se pudo obtener el teléfono del cliente (HTTP ${response.code()})")
                return null
            }
            response.body()?.extraerUsuarioCliente()?.telefono?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Error al obtener el teléfono del cliente", e)
            null
        }
    }

    /** `GET /mascotas/mascota/{uuid}` → completa la ficha de cada mascota. */
    private suspend fun enriquecerMascotas(
        mascotas: List<SolicitudMascotaResDto>
    ): List<SolicitudMascotaResDto> = mascotas.map { mascota ->
        val uuid = mascota.uuid?.takeIf { it.isNotBlank() } ?: return@map mascota
        val response = try {
            withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getMascotaPorUuid(authHeader, uuid)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al obtener la mascota $uuid", e)
            return@map mascota
        }
        if (!response.isSuccessful) {
            Log.w(TAG, "No se pudo obtener la mascota $uuid (HTTP ${response.code()})")
            return@map mascota
        }

        val detalle = response.body()?.extraerMascota() ?: return@map mascota
        mascota.copy(
            nombre = detalle.nombre?.takeIf { it.isNotBlank() } ?: mascota.nombre,
            raza = detalle.tipoRazaDescripcion?.takeIf { it.isNotBlank() } ?: mascota.raza,
            tamano = detalle.tipoTamanoMascotaDescripcion?.takeIf { it.isNotBlank() }
                ?: mascota.tamano,
            tipoRazaDescripcion = detalle.tipoRazaDescripcion,
            tipoTamanoMascotaDescripcion = detalle.tipoTamanoMascotaDescripcion,
            tipoNivelEnergiaDescripcion = detalle.tipoNivelEnergiaDescripcion,
            precaucionesMedicas = detalle.precaucionesMedicas,
            edadAnos = detalle.edadAnos,
            edadMeses = detalle.edadMeses,
            pesoKg = detalle.pesoKg,
            sociableConPerros = detalle.sociableConPerros,
            sociableConPersonas = detalle.sociableConPersonas,
            esterilizado = detalle.esterilizado,
            fotoMascota = detalle.fotoMascota ?: mascota.fotoMascota,
            fotoUrl = mascota.fotoUrl ?: detalle.fotoUrl
        )
    }

    /** Adapta las mascotas del endpoint al modelo que ya usa la pantalla. */
    private fun mascotasDetalle(solicitud: SolicitudPaseoResDto): List<MascotaDetalleItem> =
        solicitud.mascotas.orEmpty().map { mascota ->
            MascotaDetalleItem(
                uuid = mascota.uuid,
                nombre = mascota.nombre?.trim()?.takeIf { it.isNotBlank() } ?: "Mascota",
                raza = mascota.tipoRazaDescripcion?.takeIf { it.isNotBlank() } ?: mascota.raza,
                tamano = mascota.tipoTamanoMascotaDescripcion?.takeIf { it.isNotBlank() }
                    ?: mascota.tamano,
                precaucionesMedicas = mascota.precaucionesMedicas,
                fotoUrl = mascota.fotoUrlEfectiva,
                edadAnos = mascota.edadAnos,
                edadMeses = mascota.edadMeses,
                pesoKg = mascota.pesoKg,
                nivelEnergia = mascota.tipoNivelEnergiaDescripcion,
                sociablePerros = mascota.sociableConPerros == 1,
                sociablePersonas = mascota.sociableConPersonas == 1,
                esterilizado = mascota.esterilizado == 1
            )
        }

    private fun aceptar() {
        ejecutarAccion(AccionPaseo.ACEPTAR) { uuid ->
            RetrofitClient.apiService.aceptarSolicitud(authHeader, uuid)
        }
    }

    private fun rechazar(motivo: String) {
        ejecutarAccion(AccionPaseo.RECHAZAR) { uuid ->
            RetrofitClient.apiService.rechazarSolicitud(
                token = authHeader,
                uuid = uuid,
                body = RechazarSolicitudRequest(
                    motivoRechazo = motivo.trim().takeIf { it.isNotBlank() }
                )
            )
        }
    }

    private fun iniciar() {
        ejecutarAccion(AccionPaseo.INICIAR) { uuid ->
            RetrofitClient.apiService.iniciarPaseo(authHeader, uuid)
        }
    }

    private fun descartar() {
        val uuid = uuidActual() ?: return
        if (accionEnCurso != null) return

        accionEnCurso = AccionPaseo.DESCARTAR
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.descartarSolicitud(authHeader, uuid)
                }
                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@SolicitudDetallePaseadorActivity,
                        ApiErrorMapper.fromResponse(response, ApiErrorContext.WALK),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                huboCambios = true
                Toast.makeText(
                    this@SolicitudDetallePaseadorActivity,
                    "Solicitud descartada",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al descartar la solicitud", e)
                Toast.makeText(
                    this@SolicitudDetallePaseadorActivity,
                    ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                accionEnCurso = null
            }
        }
    }

    /**
     * Flujo de solicitud inmediata: PATCH /tomar y, al confirmar, PATCH /iniciar
     * (igual que la bandeja). Si el inicio falla, el paseo queda ACEPTADA en
     * "Mis paseos" y se avisa al usuario.
     */
    private fun tomar() {
        val uuid = uuidActual() ?: return
        if (accionEnCurso != null) return

        accionEnCurso = AccionPaseo.TOMAR
        lifecycleScope.launch {
            try {
                val respuestaTomar = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.tomarSolicitud(authHeader, uuid)
                }
                if (!respuestaTomar.isSuccessful) {
                    Toast.makeText(
                        this@SolicitudDetallePaseadorActivity,
                        ApiErrorMapper.fromResponse(respuestaTomar, ApiErrorContext.WALK),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                huboCambios = true

                val respuestaIniciar = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.iniciarPaseo(authHeader, uuid)
                }
                val mensaje = if (respuestaIniciar.isSuccessful) {
                    "Paseo aceptado e iniciado"
                } else {
                    "Paseo aceptado, pero no se pudo iniciar: " +
                        ApiErrorMapper.fromResponse(respuestaIniciar, ApiErrorContext.WALK)
                }
                Toast.makeText(
                    this@SolicitudDetallePaseadorActivity,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
                if (respuestaIniciar.isSuccessful) {
                    abrirPaseoActivo()
                } else {
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al tomar la solicitud", e)
                Toast.makeText(
                    this@SolicitudDetallePaseadorActivity,
                    ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                accionEnCurso = null
            }
        }
    }

    private fun uuidActual(): String? = solicitudUuid?.takeIf { it.isNotBlank() }

    /** Abre el paseo en curso; al volver se cierra el detalle y la bandeja refresca. */
    private fun abrirPaseoActivo() {
        val uuid = uuidActual() ?: return
        paseoActivoLauncher.launch(
            Intent(this, PaseoActivoPaseadorActivity::class.java).apply {
                putExtra(PaseoActivoPaseadorActivity.EXTRA_SOLICITUD_UUID, uuid)
            }
        )
    }

    private fun cancelar(motivo: String) {
        ejecutarAccion(AccionPaseo.CANCELAR) { uuid ->
            RetrofitClient.apiService.cancelarPaseoPaseador(
                token = authHeader,
                uuid = uuid,
                request = CancelarSolicitudReqDto(motivo = motivo.trim())
            )
        }
    }

    /**
     * Ejecuta una transición de estado sobre la solicitud. Al aceptar se queda en
     * la pantalla con el nuevo estado; al rechazar/cancelar/iniciar se cierra y
     * la bandeja se refresca al volver.
     */
    private fun ejecutarAccion(
        accion: AccionPaseo,
        llamada: suspend (String) -> Response<ResponseDto<SolicitudPaseoResDto>>
    ) {
        val uuid = uuidActual() ?: return
        if (accionEnCurso != null) return

        accionEnCurso = accion
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { llamada(uuid) }

                if (!response.isSuccessful) {
                    val mensaje = ApiErrorMapper.fromResponse(response, ApiErrorContext.WALK)
                    Log.e(TAG, "Error HTTP ${response.code()} al ejecutar $accion: $mensaje")
                    Toast.makeText(
                        this@SolicitudDetallePaseadorActivity,
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                huboCambios = true

                when (accion) {
                    AccionPaseo.ACEPTAR -> {
                        Toast.makeText(
                            this@SolicitudDetallePaseadorActivity,
                            "Paseo aceptado",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Recarga el detalle para reflejar el nuevo estado y
                        // conservar los datos completos (incluido el teléfono).
                        cargarDetalle()
                    }

                    AccionPaseo.RECHAZAR -> {
                        Toast.makeText(
                            this@SolicitudDetallePaseadorActivity,
                            "Solicitud rechazada",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }

                    AccionPaseo.INICIAR -> {
                        Toast.makeText(
                            this@SolicitudDetallePaseadorActivity,
                            "Paseo iniciado",
                            Toast.LENGTH_SHORT
                        ).show()
                        abrirPaseoActivo()
                    }

                    AccionPaseo.CANCELAR -> {
                        Toast.makeText(
                            this@SolicitudDetallePaseadorActivity,
                            "Paseo cancelado",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }

                    else -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al ejecutar $accion", e)
                Toast.makeText(
                    this@SolicitudDetallePaseadorActivity,
                    ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                accionEnCurso = null
            }
        }
    }

    private fun abrirMapa(direccion: String) {
        val consulta = Uri.encode(direccion)
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:0,0?q=$consulta")
        ).apply {
            setPackage("com.google.android.apps.maps")
        }

        val intentGenerico = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:0,0?q=$consulta")
        )

        try {
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(intentGenerico)
            } catch (e2: Exception) {
                Toast.makeText(
                    this,
                    "No se encontró una aplicación de mapas instalada.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
