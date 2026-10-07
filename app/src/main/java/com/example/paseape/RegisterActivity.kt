package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.paseape.ui.auth.RegisterScreen
import com.example.paseape.ui.theme.PaseaPETheme

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaseaPETheme {
                RegisterScreen(
                    onRegisterSuccess = {
                        // Cuando el registro sea exitoso, cerramos esta pantalla y volvemos al login
                        finish()
                    },
                    onBackToLogin = {
                        // Si decide regresar al login manualmente
                        finish()
                    }
                )
            }
        }
    }
}