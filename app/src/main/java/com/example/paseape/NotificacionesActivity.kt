package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.paseape.data.SessionManager
import com.example.paseape.ui.theme.PaseaPETheme

class NotificacionesActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(this)[NotificacionesViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authHeader = SessionManager.getAuthHeader(this)
        val userUuid = SessionManager.getUserUuid(this)
        NotificacionesViewModel.inicializarContexto(this, userUuid)
        viewModel.cargarNotificaciones(authHeader)

        setContent {
            PaseaPETheme {
                NotificacionesScreen(
                    viewModel = viewModel,
                    authHeader = authHeader,
                    onBack = { finish() }
                )
            }
        }
    }
}
