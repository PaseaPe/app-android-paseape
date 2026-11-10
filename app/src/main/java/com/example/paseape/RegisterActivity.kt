package com.example.paseape

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.paseape.ui.theme.PaseaPETheme

class RegisterActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(this)[RegisterViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaseaPETheme {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = { rol, uuid ->
                        val token = viewModel.uiState.value.authToken

                        if (!token.isNullOrBlank()) {
                            // Registro con Google: el backend ya devolvió la sesión.
                            val rolFinal = rol ?: 1
                            guardarSesionLocal(
                                tokenJwt = token,
                                userUuid = uuid.orEmpty(),
                                userRole = rolFinal
                            )
                            Toast.makeText(
                                this,
                                "¡Cuenta creada con Google!",
                                Toast.LENGTH_SHORT
                            ).show()
                            irAlDashboard(rolFinal, uuid)
                        } else {
                            Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    },
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun guardarSesionLocal(tokenJwt: String, userUuid: String, userRole: Int) {
        val roleString = when (userRole) {
            99 -> "ADMIN"
            2 -> "PASEADOR"
            else -> "CLIENTE"
        }

        val paseaPrefs = getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE)
        paseaPrefs.edit()
            .putString("user_uuid", userUuid)
            .putString("auth_token", tokenJwt)
            .putString("JWT_TOKEN", tokenJwt)
            .putInt("USER_ROLE", userRole)
            .putString("user_role", roleString)
            .commit()

        val sharedPref = getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("user_uuid", userUuid)
            .putString("auth_token", tokenJwt)
            .putString("JWT_TOKEN", tokenJwt)
            .putInt("USER_ROLE", userRole)
            .putString("user_role", roleString)
            .commit()
    }

    private fun irAlDashboard(userRole: Int, userUuid: String?) {
        val uuid = userUuid.orEmpty()
        val destino = if (userRole == 2) {
            WalkerDashboardActivity::class.java
        } else {
            DashboardActivity::class.java
        }

        val intent = Intent(this, destino).apply {
            putExtra("USER_ROLE", userRole)
            putExtra("EXTRA_USER_UUID", uuid)
            putExtra("user_uuid", uuid)
            putExtra("USER_UUID", uuid)
        }
        startActivity(intent)
        finish()
    }
}
