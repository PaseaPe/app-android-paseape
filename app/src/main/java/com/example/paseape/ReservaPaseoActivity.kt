package com.example.paseape

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.paseape.ui.theme.PaseaPETheme

class ReservaPaseoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaseaPETheme {
                val viewModel = remember { ReservaPaseoViewModel() }
                var pasoActual by remember { mutableIntStateOf(1) }

                when (pasoActual) {
                    1 -> Paso1Screen(
                        viewModel = viewModel,
                        onBack = { finish() },
                        onNext = { pasoActual = 2 }
                    )
                    2 -> Paso2Screen(
                        viewModel = viewModel,
                        onBack = { pasoActual = 1 },
                        onNext = { pasoActual = 3 }
                    )
                    3 -> Paso3Screen(
                        viewModel = viewModel,
                        onBack = { pasoActual = 2 },
                        onConfirmSuccess = {
                            Toast.makeText(this, "¡Solicitud enviada con éxito!", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    )
                }
            }
        }
    }
}