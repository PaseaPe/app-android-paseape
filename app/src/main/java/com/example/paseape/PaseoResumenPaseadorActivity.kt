package com.example.paseape

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.PaseaPETheme
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Pantalla "Servicio Concluido" del paseador.
 *
 * Se abre al finalizar un paseo y arma el resumen con datos exclusivamente del
 * backend:
 * - `GET /solicitudes/{uuid}`: monto, duración contratada, dirección, mascotas,
 *   horas reales de inicio/fin y el promedio del paseador.
 * - `GET /solicitudes/{uuid}/recorrido`: distancia, duración real, velocidad
 *   promedio y la polilínea codificada del recorrido.
 *
 * No hay reporte de comportamiento ni calificaciones todavía: solo se muestra
 * el promedio real del paseador.
 */
class PaseoResumenPaseadorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SOLICITUD_UUID = "extra_solicitud_uuid"

        private const val TAG = "PASEO_RESUMEN"
    }

    private var uiState by mutableStateOf(PaseoResumenUiState())
    private var authHeader: String = ""
    private var solicitudUuid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authHeader = leerAuthHeader()
        solicitudUuid = intent.getStringExtra(EXTRA_SOLICITUD_UUID)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        cargarResumen()

        setContent {
            PaseaPETheme {
                PaseoResumenPaseadorScreen(
                    state = uiState,
                    onVolver = { finish() },
                    onReintentar = { cargarResumen() }
                )
            }
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

    private fun cargarResumen() {
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
                val (detalleResp, recorridoResp) = coroutineScope {
                    val detalle = async(Dispatchers.IO) {
                        RetrofitClient.apiService.getSolicitudDetalle(authHeader, uuid)
                    }
                    // El recorrido es complementario: si falla, el resumen se muestra igual.
                    val recorrido = async(Dispatchers.IO) {
                        runCatching {
                            RetrofitClient.apiService.obtenerRecorridoPaseo(authHeader, uuid)
                        }.getOrNull()
                    }
                    detalle.await() to recorrido.await()
                }

                if (!detalleResp.isSuccessful) {
                    uiState = uiState.copy(
                        cargando = false,
                        error = ApiErrorMapper.fromResponse(detalleResp, ApiErrorContext.WALK)
                    )
                    return@launch
                }

                val solicitud = detalleResp.body()?.response
                if (solicitud == null) {
                    uiState = uiState.copy(
                        cargando = false,
                        error = "No se encontraron los datos del paseo."
                    )
                    return@launch
                }

                val recorrido = recorridoResp
                    ?.takeIf { it.isSuccessful }
                    ?.body()
                    ?.response

                val puntosRuta = recorrido?.coordenadasCodificadas
                    ?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { PolyUtil.decode(it) }.getOrNull() }
                    .orEmpty()

                uiState = PaseoResumenUiState(
                    solicitud = solicitud,
                    recorrido = recorrido,
                    puntosRuta = puntosRuta,
                    cargando = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al cargar el resumen del paseo", e)
                uiState = uiState.copy(
                    cargando = false,
                    error = ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK)
                )
            }
        }
    }
}
