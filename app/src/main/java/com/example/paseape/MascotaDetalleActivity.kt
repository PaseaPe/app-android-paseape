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
                    onActualizarMascota = { req -> actualizarMascota(req) }
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
            uiState = MascotaDetalleUiState.Error("Sesión expirada. Inicia sesión nuevamente.")
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
                        val errorBody = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        uiState = MascotaDetalleUiState.Error("Error al cargar mascota (HTTP ${response.code()})")
                        Log.e("MASCOTA_DETALLE", "Error HTTP: $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("MASCOTA_DETALLE", "Error de red al consultar mascota", e)
                withContext(Dispatchers.Main) {
                    uiState = MascotaDetalleUiState.Error(e.localizedMessage ?: "Error de red.")
                }
            }
        }
    }

    private fun actualizarMascota(req: ActualizarMascotaRequest) {
        val uuid = mascotaUuid ?: return
        val token = getAuthToken() ?: run {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MASCOTA_DETALLE", "Enviando PUT mascotas/$uuid con: $req")
                val response = RetrofitClient.apiService.actualizarMascota(authHeader, uuid, req)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MascotaDetalleActivity, "¡Mascota actualizada con éxito!", Toast.LENGTH_SHORT).show()
                        cargarDetalleMascota()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        Log.e("MASCOTA_DETALLE", "Error al actualizar: HTTP ${response.code()} $errorBody")
                        Toast.makeText(this@MascotaDetalleActivity, "Error al actualizar mascota (HTTP ${response.code()})", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MASCOTA_DETALLE", "Excepción al actualizar mascota", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MascotaDetalleActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
