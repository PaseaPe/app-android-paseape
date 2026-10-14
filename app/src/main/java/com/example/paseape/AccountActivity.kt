package com.example.paseape

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.paseape.data.model.ActualizarPerfilReqDto
import com.example.paseape.data.model.CrearMascotaRequest
import com.example.paseape.data.model.MascotaData
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

        cargarPerfilCliente()

        setContent {
            PaseaPETheme {
                ProfileScreen(
                    uiState = uiState,
                    onRetry = { cargarPerfilCliente() },
                    onLogoutClick = { cerrarSesionYRedirigir() },
                    onLogout = { cerrarSesionYRedirigir() },
                    onBack = { finish() },
                    onGuardarPerfil = { req -> actualizarDatosPerfil(req) },
                    onCrearMascota = { req -> crearMascota(req) },
                    onMascotaClick = { mascota: MascotaData ->
                        val intent = Intent(this, MascotaDetalleActivity::class.java).apply {
                            putExtra(MascotaDetalleActivity.EXTRA_MASCOTA_UUID, mascota.uuid)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun getStoredToken(): String? {
        val prefs = getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE)
        val legacyPrefs = getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
        return prefs.getString("auth_token", null)
            ?: prefs.getString("JWT_TOKEN", null)
            ?: legacyPrefs.getString("auth_token", null)
            ?: legacyPrefs.getString("JWT_TOKEN", null)
    }

    private fun getStoredUuid(): String? {
        val prefs = getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE)
        val legacyPrefs = getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
        return intent.getStringExtra("EXTRA_USER_UUID")
            ?: intent.getStringExtra("user_uuid")
            ?: intent.getStringExtra("USER_UUID")
            ?: prefs.getString("user_uuid", null)
            ?: legacyPrefs.getString("user_uuid", null)
    }

    fun cargarPerfil() {
        cargarPerfilCliente()
    }

    fun cargarPerfilCliente() {
        val uuid = getStoredUuid()
        val token = getStoredToken()

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

    fun actualizarDatosPerfil(req: ActualizarPerfilReqDto) {
        val token = getStoredToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Sesión no válida. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show()
            return
        }
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.actualizarPerfil(authHeader, req)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AccountActivity, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                        cargarPerfilCliente()
                    } else {
                        val errorMsg = response.message().ifBlank { "HTTP ${response.code()}" }
                        Toast.makeText(this@AccountActivity, "Error al actualizar perfil: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ACCOUNT_DEBUG", "Error al actualizar perfil", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountActivity, "Error de conexión al actualizar perfil", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun crearMascota(req: CrearMascotaRequest) {
        val token = getStoredToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Sesión no válida. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show()
            return
        }
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        // Aseguramos que usuarioUuid esté presente
        val finalReq = if (req.usuarioUuid.isBlank()) {
            val uuid = getStoredUuid() ?: ""
            req.copy(usuarioUuid = uuid)
        } else {
            req
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.crearMascota(authHeader, finalReq)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AccountActivity, "Mascota agregada con éxito", Toast.LENGTH_SHORT).show()
                        cargarPerfilCliente()
                    } else {
                        val errorMsg = response.message().ifBlank { "HTTP ${response.code()}" }
                        Toast.makeText(this@AccountActivity, "Error al agregar mascota: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ACCOUNT_DEBUG", "Error al crear mascota", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountActivity, "Error de conexión al crear mascota", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun cerrarSesionYRedirigir() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = getStoredToken()
                if (!token.isNullOrEmpty()) {
                    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                    RetrofitClient.apiService.logout(authHeader)
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