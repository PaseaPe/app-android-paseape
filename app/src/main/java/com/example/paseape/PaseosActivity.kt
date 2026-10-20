package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.paseape.ui.screens.paseos.PaseosScreen
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.viewmodel.PaseosViewModel

class PaseosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = PaseosViewModel()

        setContent {
            PaseaPETheme {
                PaseosScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}