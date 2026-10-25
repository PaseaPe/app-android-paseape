package com.example.paseape

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.paseape.data.SessionManager
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.PaseaPETheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalkerDashboardActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(this)[WalkerProfileViewModel::class.java]
    }

    private val disponibilidadViewModel by lazy {
        ViewModelProvider(this)[DisponibilidadViewModel::class.java]
    }

    private val paseosViewModel by lazy {
        ViewModelProvider(this)[PaseosViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = SessionManager.getToken(this).orEmpty()
        val userUuid = intent.getStringExtra("EXTRA_USER_UUID")
            ?: intent.getStringExtra("user_uuid")
            ?: intent.getStringExtra("USER_UUID")
            ?: SessionManager.getUserUuid(this).orEmpty()

        setContent {
            PaseaPETheme {
                WalkerDashboardScreen(
                    token = token,
                    usuarioUuid = userUuid,
                    viewModel = viewModel,
                    disponibilidadViewModel = disponibilidadViewModel,
                    paseosViewModel = paseosViewModel,
                    onLogoutClick = { cerrarSesionYRedirigir() }
                )
            }
        }
    }

    private fun cerrarSesionYRedirigir() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.getToken(this@WalkerDashboardActivity)
                if (!token.isNullOrBlank()) {
                    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                    RetrofitClient.apiService.logout(authHeader)
                }
            } catch (e: Exception) {
                Log.e("WALKER_LOGOUT", "Error al revocar sesión en servidor: ${e.message}")
            } finally {
                getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE).edit().clear().commit()
                getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE).edit().clear().commit()

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@WalkerDashboardActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
