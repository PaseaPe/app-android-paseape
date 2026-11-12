package com.example.paseape

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.paseape.data.model.CancelarSolicitudReqDto
import com.example.paseape.data.model.RechazarSolicitudRequest
import com.example.paseape.data.model.ResponseDto
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.PaseaPETheme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Pantalla de detalle de una solicitud de paseo del lado del paseador.
 *
 * Se abre desde un card de la bandeja: recibe la solicitud ya cargada como JSON
 * (snapshot) para no requerir un endpoint de detalle en el backend. A partir de
 * ese snapshot se enriquecen las mascotas con su detalle y se permiten las
 * acciones aceptar/rechazar (PENDIENTE), iniciar paseo o cancelar (ACEPTADA).
 */
class SolicitudDetallePaseadorActivity : ComponentActivity() {

    companion object {
        /** JSON de [SolicitudPaseoResDto] enviado por la bandeja. */
        const val EXTRA_SOLICITUD_JSON = "extra_solicitud_json"
        private const val TAG = "SOLICITUD_DETALLE"
    }

    private var uiState by mutableStateOf<SolicitudDetalleUiState>(SolicitudDetalleUiState.Loading)
    private var accionEnCurso by mutableStateOf<AccionPaseo?>(null)
    private var authHeader: String = ""
    private var huboCambios = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authHeader = leerAuthHeader()
        inicializar()

        setContent {
            PaseaPETheme {
                SolicitudDetallePaseadorScreen(
                    uiState = uiState,
                    accionEnCurso = accionEnCurso,
                    onBack = { finish() },
                    onRetry = { inicializar() },
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

    private fun leerSnapshot(): SolicitudPaseoResDto? {
        val json = intent.getStringExtra(EXTRA_SOLICITUD_JSON) ?: return null
        return try {
            Gson().fromJson(json, SolicitudPaseoResDto::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer el snapshot de la solicitud", e)
            null
        }
    }

    private fun inicializar() {
        val solicitud = leerSnapshot()
        if (solicitud == null) {
            uiState = SolicitudDetalleUiState.Error("No se encontraron los datos de la solicitud.")
            return
        }
        if (authHeader.isBlank()) {
            uiState = SolicitudDetalleUiState.Error("Tu sesión expiró. Inicia sesión nuevamente.")
            return
        }

        uiState = SolicitudDetalleUiState.Success(solicitud, emptyList())
        cargarMascotasEnriquecidas(solicitud)
    }

    /** Completa el snapshot con la foto y las precauciones médicas de cada mascota. */
    private fun cargarMascotasEnriquecidas(solicitud: SolicitudPaseoResDto) {
        if (solicitud.mascotas.isNullOrEmpty()) return

        lifecycleScope.launch {
            val mascotas = cargarMascotas(solicitud)
            val actual = uiState as? SolicitudDetalleUiState.Success ?: return@launch
            uiState = actual.copy(mascotas = mascotas)
        }
    }


    private suspend fun cargarMascotas(solicitud: SolicitudPaseoResDto): List<MascotaDetalleItem> {
        return solicitud.mascotas.orEmpty().map { mascota ->
            val detalle = mascota.uuid
                ?.takeIf { it.isNotBlank() }
                ?.let { uuid ->
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.apiService.getMascotaPorUuid(authHeader, uuid)
                        }
                        if (response.isSuccessful) response.body()?.extraerMascota() else null
                    } catch (e: Exception) {
                        Log.e(TAG, "No se pudo cargar el detalle de la mascota $uuid", e)
                        null
                    }
                }

            MascotaDetalleItem(
                uuid = mascota.uuid,
                nombre = mascota.nombre?.trim()?.takeIf { it.isNotBlank() } ?: "Mascota",
                raza = detalle?.tipoRazaDescripcion?.takeIf { it.isNotBlank() } ?: mascota.raza,
                tamano = detalle?.tipoTamanoMascotaDescripcion?.takeIf { it.isNotBlank() } ?: mascota.tamano,
                precaucionesMedicas = detalle?.precaucionesMedicas,
                fotoUrl = detalle?.fotoUrlEfectiva ?: mascota.fotoUrl,
                edadAnos = detalle?.edadAnos,
                edadMeses = detalle?.edadMeses,
                pesoKg = detalle?.pesoKg,
                nivelEnergia = detalle?.tipoNivelEnergiaDescripcion,
                sociablePerros = detalle?.sociableConPerros == 1,
                sociablePersonas = detalle?.sociableConPersonas == 1,
                esterilizado = detalle?.esterilizado == 1
            )
        }
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
                finish()
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

    private fun uuidActual(): String? = (uiState as? SolicitudDetalleUiState.Success)
        ?.solicitud
        ?.uuid
        ?.takeIf { it.isNotBlank() }

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
                val detalle = response.body()?.response

                when (accion) {
                    AccionPaseo.ACEPTAR -> {
                        val actual = (uiState as? SolicitudDetalleUiState.Success)
                        val actualizado = detalle ?: actual?.solicitud?.copy(
                            estadoId = 2,
                            estadoDescripcion = "ACEPTADA"
                        )
                        if (actualizado != null) {
                            uiState = SolicitudDetalleUiState.Success(
                                solicitud = actualizado,
                                mascotas = actual?.mascotas.orEmpty()
                            )
                        }
                        Toast.makeText(
                            this@SolicitudDetallePaseadorActivity,
                            "Paseo aceptado",
                            Toast.LENGTH_SHORT
                        ).show()
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
                        finish()
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
