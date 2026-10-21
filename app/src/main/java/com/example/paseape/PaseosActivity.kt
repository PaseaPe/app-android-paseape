package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.paseape.ui.theme.PaseaPETheme

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