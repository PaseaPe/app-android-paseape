package com.example.paseape

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.paseape.data.model.UsuarioClienteData
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.PaseaPETheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val data: UsuarioClienteData) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class AccountActivity : ComponentActivity() {

    private var uiState by mutableStateOf<ProfileUiState>(ProfileUiState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cargarPerfil()

        setContent {
            PaseaPETheme {
                ProfileScreen(
                    uiState = uiState,
                    onRetry = { cargarPerfil() },
                    onLogoutClick = { cerrarSesionYRedirigir() },
                    onLogout = { cerrarSesionYRedirigir() },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun cargarPerfil() {
        val prefs = getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE)
        val legacyPrefs = getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)

        val uuid = intent.getStringExtra("EXTRA_USER_UUID")
            ?: intent.getStringExtra("user_uuid")
            ?: intent.getStringExtra("USER_UUID")
            ?: prefs.getString("user_uuid", null)
            ?: legacyPrefs.getString("user_uuid", null)

        val token = prefs.getString("auth_token", null)
            ?: prefs.getString("JWT_TOKEN", null)
            ?: legacyPrefs.getString("auth_token", null)
            ?: legacyPrefs.getString("JWT_TOKEN", null)

        Log.d("ACCOUNT_DEBUG", "Intentando cargar perfil. UUID: [$uuid], Token presente: [${!token.isNullOrBlank()}]")

        if (uuid.isNullOrBlank()) {
            Log.e("ACCOUNT_DEBUG", "Error: user_uuid no encontrado en Intent ni SharedPreferences")
            uiState = ProfileUiState.Error("Sesión expirada o identificador no encontrado. Por favor inicia sesión nuevamente.")
            return
        }

        if (token.isNullOrBlank()) {
            Log.e("ACCOUNT_DEBUG", "Error: auth_token no encontrado en SharedPreferences")
            uiState = ProfileUiState.Error("Sesión expirada: Token de autenticación ausente. Por favor inicia sesión nuevamente.")
            return
        }

        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        uiState = ProfileUiState.Loading
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("ACCOUNT_DEBUG", "Ejecutando GET cliente/$uuid con cabecera Authorization")
                val response = RetrofitClient.apiService.getClientePorUuid(token = authHeader, uuid = uuid)
                Log.d("ACCOUNT_DEBUG", "Status Code recibido: ${response.code()}, Message: [${response.message()}]")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val usuarioCliente = body?.extraerUsuarioCliente()
                        if (usuarioCliente != null) {
                            Log.d("ACCOUNT_DEBUG", "Perfil cargado con éxito para: ${usuarioCliente.nombres} ${usuarioCliente.apellidos}")
                            uiState = ProfileUiState.Success(usuarioCliente)
                        } else {
                            Log.w("ACCOUNT_DEBUG", "Respuesta 200 pero cuerpo o usuarioCliente nulo: $body")
                            uiState = ProfileUiState.Error("No se encontraron los datos del cliente en la respuesta.")
                        }
                    } else {
                        val rawError = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        Log.e("ACCOUNT_DEBUG", "Error del servidor: HTTP ${response.code()} ${response.message()} - Body: $rawError")
                        uiState = ProfileUiState.Error("Error del servidor: HTTP ${response.code()} ${response.message()}".trim())
                    }
                }
            } catch (e: Exception) {
                Log.e("ACCOUNT_DEBUG", "Error de red", e)
                withContext(Dispatchers.Main) {
                    uiState = ProfileUiState.Error("Error de conexión: ${e.localizedMessage ?: "Fallo de red"}")
                }
            }
        }
    }

    fun cerrarSesionYRedirigir() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE).getString("JWT_TOKEN", null)
                    ?: getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE).getString("JWT_TOKEN", null)
                if (!token.isNullOrEmpty()) {
                    RetrofitClient.apiService.logout("Bearer $token")
                }
            } catch (e: Exception) {
                Log.e("ACCOUNT_LOGOUT", "Error al revocar sesión en servidor: ${e.message}")
            } finally {
                // Limpieza completa de SharedPreferences
                getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE).edit().clear().commit()
                getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE).edit().clear().commit()
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@AccountActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}