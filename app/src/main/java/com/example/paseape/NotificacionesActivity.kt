package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.paseape.ui.theme.PaseaPETheme

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