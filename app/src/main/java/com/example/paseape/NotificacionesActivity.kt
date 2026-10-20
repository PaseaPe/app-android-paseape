package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.paseape.ui.screens.notificaciones.NotificacionesScreen
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.viewmodel.NotificacionesViewModel

class NotificacionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = NotificacionesViewModel()

        setContent {
            PaseaPETheme {
                NotificacionesScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}