package com.example.paseape

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.PaseaPETheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Pantalla de paseo en curso del paseador.
 *
 * Carga la solicitud con `GET /solicitudes/{uuid}` y delega el GPS y el envío
 * de ubicaciones a [PaseoUbicacionForegroundService], que sigue funcionando con
 * la pantalla bloqueada o mientras el paseador navega en otras pantallas. Esta
 * Activity solo observa la ubicación para pintar el mapa.
 *
 * El mapa solo muestra la ubicación actual como punto (sin recorrido ni
 * velocidad). El botón "Finalizar paseo" detiene el servicio y ejecuta
 * `PATCH /paseador/solicitudes/{uuid}/finalizar`; al confirmarse, abre
 * [PaseoResumenPaseadorActivity] con el resumen y el recorrido consolidado.
 */
class PaseoActivoPaseadorActivity : ComponentActivity() {

    companion object {
        /** UUID de la solicitud que se está paseando. */
        const val EXTRA_SOLICITUD_UUID = "extra_solicitud_uuid"

        private const val TAG = "PASEO_ACTIVO"
        private const val TIEMPO_ESPERA_DETENER_MS = 8_000L
    }

    private var uiState by mutableStateOf(PaseoActivoUiState())
    private var authHeader: String = ""
    private var solicitudUuid: String? = null
    private var paseoFinalizado = false
    private var relojIniciado = false
    private var permisoNotificacionesSolicitado = false

    private val permisoUbicacionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        uiState = uiState.copy(permisoUbicacion = concedido, gpsActivo = concedido)
        if (concedido) {
            iniciarServicioUbicacion()
        } else {
            Toast.makeText(
                this,
                "Sin ubicación no se puede compartir el paseo con el cliente.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val permisoNotificacionesLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* La ubicación se comparte igual; solo se oculta el aviso. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authHeader = leerAuthHeader()
        solicitudUuid = intent.getStringExtra(EXTRA_SOLICITUD_UUID)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        observarServicioUbicacion()
        cargarPaseo()

        setContent {
            PaseaPETheme {
                PaseoActivoPaseadorScreen(
                    state = uiState,
                    onBack = { finish() },
                    onSolicitarPermiso = { solicitarPermisoUbicacion() },
                    onFinalizar = { finalizarPaseo() },
                    onReintentar = { cargarPaseo() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Al volver a la pantalla, rearma el servicio si el sistema lo detuvo.
        if (!paseoFinalizado && uiState.solicitud != null && tienePermisoUbicacion()) {
            iniciarServicioUbicacion()
        }
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

    private fun observarServicioUbicacion() {
        lifecycleScope.launch {
            PaseoUbicacionForegroundService.ubicacionActual.collect { ubicacion ->
                uiState = uiState.copy(ubicacion = ubicacion)
            }
        }
        lifecycleScope.launch {
            PaseoUbicacionForegroundService.precisionMetros.collect { precision ->
                uiState = uiState.copy(precisionMetros = precision)
            }
        }
    }

    private fun cargarPaseo() {
        val uuid = solicitudUuid
        if (uuid.isNullOrBlank()) {
            uiState = uiState.copy(
                cargando = false,
                error = "No se encontró el identificador de la solicitud."
            )
            return
        }
        if (authHeader.isBlank()) {
            uiState = uiState.copy(
                cargando = false,
                error = "Tu sesión expiró. Inicia sesión nuevamente."
            )
            return
        }

        uiState = uiState.copy(cargando = true, error = null)
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getSolicitudDetalle(authHeader, uuid)
                }
                if (!response.isSuccessful) {
                    uiState = uiState.copy(
                        cargando = false,
                        error = ApiErrorMapper.fromResponse(response, ApiErrorContext.WALK)
                    )
                    return@launch
                }

                val solicitud = response.body()?.response
                if (solicitud == null) {
                    uiState = uiState.copy(
                        cargando = false,
                        error = "No se encontraron los datos del paseo."
                    )
                    return@launch
                }

                val permiso = tienePermisoUbicacion()
                uiState = PaseoActivoUiState(
                    solicitud = solicitud,
                    mascotas = mascotasDetalle(solicitud),
                    inicioMillis = parseInicioMillis(solicitud.fechaHoraInicioReal)
                        ?: System.currentTimeMillis(),
                    duracionMinutos = solicitud.duracionMinutos,
                    ubicacion = PaseoUbicacionForegroundService.ubicacionActual.value,
                    precisionMetros = PaseoUbicacionForegroundService.precisionMetros.value,
                    permisoUbicacion = permiso,
                    gpsActivo = permiso,
                    cargando = false
                )

                iniciarReloj()

                if (permiso) {
                    iniciarServicioUbicacion()
                } else {
                    solicitarPermisoUbicacion()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al cargar el paseo activo", e)
                uiState = uiState.copy(
                    cargando = false,
                    error = ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK)
                )
            }
        }
    }

    /** Adapta las mascotas del endpoint al modelo que usa la pantalla. */
    private fun mascotasDetalle(solicitud: SolicitudPaseoResDto): List<MascotaDetalleItem> =
        solicitud.mascotas.orEmpty().map { mascota ->
            MascotaDetalleItem(
                uuid = mascota.uuid,
                nombre = mascota.nombre?.trim()?.takeIf { it.isNotBlank() } ?: "Mascota",
                raza = mascota.razaEfectiva,
                tamano = mascota.tamanoEfectivo,
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

    private fun iniciarReloj() {
        if (relojIniciado) return
        relojIniciado = true
        lifecycleScope.launch {
            while (isActive && !paseoFinalizado) {
                val inicio = uiState.inicioMillis
                if (inicio != null) {
                    val transcurrido = ((System.currentTimeMillis() - inicio) / 1000)
                        .coerceAtLeast(0)
                    uiState = uiState.copy(transcurridoSegundos = transcurrido)
                }
                delay(1_000L)
            }
        }
    }

    private fun tienePermisoUbicacion(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    private fun solicitarPermisoUbicacion() {
        if (tienePermisoUbicacion()) {
            iniciarServicioUbicacion()
            return
        }
        permisoUbicacionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun iniciarServicioUbicacion() {
        val uuid = solicitudUuid ?: return
        if (paseoFinalizado || authHeader.isBlank()) return
        if (!tienePermisoUbicacion()) return

        val intent = Intent(this, PaseoUbicacionForegroundService::class.java).apply {
            action = PaseoUbicacionForegroundService.ACTION_INICIAR
            putExtra(PaseoUbicacionForegroundService.EXTRA_SOLICITUD_UUID, uuid)
            putExtra(PaseoUbicacionForegroundService.EXTRA_AUTH_TOKEN, authHeader)
        }

        try {
            ContextCompat.startForegroundService(this, intent)
            solicitarPermisoNotificaciones()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar el servicio de ubicación", e)
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val concedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedido) {
            if (permisoNotificacionesSolicitado) return
            permisoNotificacionesSolicitado = true
            permisoNotificacionesLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Pide al servicio que envíe el último lote y se detenga. */
    private suspend fun detenerServicioUbicacion() {
        if (!PaseoUbicacionForegroundService.servicioActivo.value) return

        try {
            startService(
                Intent(this, PaseoUbicacionForegroundService::class.java).apply {
                    action = PaseoUbicacionForegroundService.ACTION_DETENER
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo detener el servicio de ubicación", e)
            return
        }

        withTimeoutOrNull(TIEMPO_ESPERA_DETENER_MS) {
            PaseoUbicacionForegroundService.servicioActivo.first { !it }
        }
    }

    private fun finalizarPaseo() {
        val uuid = solicitudUuid ?: return
        if (uiState.finalizando || paseoFinalizado) return

        uiState = uiState.copy(finalizando = true)
        lifecycleScope.launch {
            try {
                // El servicio manda su último lote antes de apagarse.
                detenerServicioUbicacion()

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.finalizarPaseo(authHeader, uuid)
                }
                if (!response.isSuccessful) {
                    uiState = uiState.copy(finalizando = false)
                    Toast.makeText(
                        this@PaseoActivoPaseadorActivity,
                        ApiErrorMapper.fromResponse(response, ApiErrorContext.WALK),
                        Toast.LENGTH_LONG
                    ).show()
                    // El paseo sigue EN_CURSO: se reanuda el seguimiento.
                    iniciarServicioUbicacion()
                    return@launch
                }

                paseoFinalizado = true
                setResult(RESULT_OK)
                startActivity(
                    Intent(
                        this@PaseoActivoPaseadorActivity,
                        PaseoResumenPaseadorActivity::class.java
                    ).apply {
                        putExtra(PaseoResumenPaseadorActivity.EXTRA_SOLICITUD_UUID, uuid)
                    }
                )
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al finalizar el paseo", e)
                uiState = uiState.copy(finalizando = false)
                Toast.makeText(
                    this@PaseoActivoPaseadorActivity,
                    ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK),
                    Toast.LENGTH_LONG
                ).show()
                iniciarServicioUbicacion()
            }
        }
    }

    /** El backend guarda `fecha_hora_inicio_real` en UTC. */
    private fun parseInicioMillis(isoUtc: String?): Long? {
        val texto = isoUtc?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            LocalDateTime.parse(texto.removeSuffix("Z").take(19))
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        }.getOrNull()
    }
}
