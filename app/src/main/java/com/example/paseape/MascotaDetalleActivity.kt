package com.example.paseape

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.paseape.data.model.ActualizarMascotaRequest
import com.example.paseape.data.model.ArchivoPendiente
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.FotoUpload
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.PaseaPETheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MascotaDetalleActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MASCOTA_UUID = "extra_mascota_uuid"
    }

    private var uiState by mutableStateOf<MascotaDetalleUiState>(MascotaDetalleUiState.Loading)
    private var mascotaUuid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mascotaUuid = intent.getStringExtra(EXTRA_MASCOTA_UUID)
            ?: intent.getStringExtra("mascota_uuid")
            ?: intent.getStringExtra("uuid")

        cargarDetalleMascota()

        setContent {
            PaseaPETheme {
                MascotaDetalleScreen(
                    uiState = uiState,
                    onRetry = { cargarDetalleMascota() },
                    onBack = { finish() },
                    onActualizarMascota = { req, foto -> actualizarMascota(req, foto) }
                )
            }
        }
    }

    private fun getAuthToken(): String? {
        val prefs = getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE)
        val legacyPrefs = getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
            ?: prefs.getString("JWT_TOKEN", null)
            ?: legacyPrefs.getString("auth_token", null)
            ?: legacyPrefs.getString("JWT_TOKEN", null)
        return token?.takeIf { it.isNotBlank() }
    }

    private fun cargarDetalleMascota() {
        val uuid = mascotaUuid
        if (uuid.isNullOrBlank()) {
            uiState = MascotaDetalleUiState.Error("No se encontró el identificador de la mascota.")
            return
        }

        val token = getAuthToken()
        if (token == null) {
            uiState = MascotaDetalleUiState.Error("Tu sesión expiró. Inicia sesión nuevamente.")
            return
        }

        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        uiState = MascotaDetalleUiState.Loading
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MASCOTA_DETALLE", "Consultando GET mascotas/mascota/$uuid")
                val response = RetrofitClient.apiService.getMascotaPorUuid(authHeader, uuid)
                Log.d("MASCOTA_DETALLE", "Status Code: ${response.code()} ${response.message()}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val mascota = body?.extraerMascota()
                        if (mascota != null) {
                            uiState = MascotaDetalleUiState.Success(mascota)
                        } else {
                            uiState = MascotaDetalleUiState.Error("No se encontraron los datos de la mascota en la respuesta.")
                        }
                    } else {
                        val errorMessage = ApiErrorMapper.fromResponse(response, ApiErrorContext.PET)
                        Log.e("MASCOTA_DETALLE", "Error HTTP ${response.code()}: $errorMessage")
                        uiState = MascotaDetalleUiState.Error(errorMessage)
                    }
                }
            } catch (e: Exception) {
                Log.e("MASCOTA_DETALLE", "Error de red al consultar mascota", e)
                withContext(Dispatchers.Main) {
                    uiState = MascotaDetalleUiState.Error(
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.PET)
                    )
                }
            }
        }
    }

    private fun actualizarMascota(req: ActualizarMascotaRequest, fotoPendiente: ArchivoPendiente? = null) {
        val uuid = mascotaUuid ?: return
        val token = getAuthToken() ?: run {
            Toast.makeText(this, "Tu sesión expiró. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show()
            return
        }

        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MASCOTA_DETALLE", "Enviando PUT mascotas/$uuid con: $req")
                val response = RetrofitClient.apiService.actualizarMascota(authHeader, uuid, req)
                if (!response.isSuccessful) {
                    val errorMessage = ApiErrorMapper.fromResponse(response, ApiErrorContext.PET)
                    Log.e("MASCOTA_DETALLE", "Error al actualizar: HTTP ${response.code()} - $errorMessage")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MascotaDetalleActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // La foto se sube como documento después de guardar los datos.
                val avisoFoto = fotoPendiente?.let { subirFotoMascota(authHeader, uuid, it) }

                withContext(Dispatchers.Main) {
                    val mensaje = if (avisoFoto == null) {
                        "¡Mascota actualizada con éxito!"
                    } else {
                        "Mascota actualizada, pero no se pudo subir la foto: $avisoFoto"
                    }
                    Toast.makeText(this@MascotaDetalleActivity, mensaje, Toast.LENGTH_LONG).show()
                    cargarDetalleMascota()
                }
            } catch (e: Exception) {
                Log.e("MASCOTA_DETALLE", "Excepción al actualizar mascota", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MascotaDetalleActivity,
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.PET),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /** Sube/reemplaza la foto de la mascota. Devuelve el error o `null`. */
    private suspend fun subirFotoMascota(
        authHeader: String,
        mascotaUuid: String,
        foto: ArchivoPendiente
    ): String? {
        val parte = FotoUpload.crearParte(contentResolver, foto) { }
            ?: return "No se pudo leer la imagen seleccionada."
        return try {
            val response = RetrofitClient.apiService.subirFotoMascota(authHeader, mascotaUuid, parte)
            if (response.isSuccessful) null
            else ApiErrorMapper.fromResponse(response, ApiErrorContext.PET)
        } catch (e: Exception) {
            Log.e("MASCOTA_DETALLE", "Error al subir foto de mascota", e)
            ApiErrorMapper.fromThrowable(e, ApiErrorContext.PET)
        }
    }
}
