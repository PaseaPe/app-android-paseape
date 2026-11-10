package com.example.paseape

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import com.example.paseape.data.SessionManager
import com.example.paseape.data.repository.PaseosRepository
import com.example.paseape.ui.theme.PaseaPETheme

class ReservaPaseoActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PASEADOR_UUID = "extra_paseador_uuid"
        const val EXTRA_PASEADOR_NOMBRE = "extra_paseador_nombre"
        const val EXTRA_TARIFA_HORA = "extra_tarifa_hora"
        const val EXTRA_FOTO_URL = "extra_foto_url"
        const val EXTRA_BIOGRAFIA = "extra_biografia"
        const val EXTRA_DISTRITO = "extra_distrito"
        const val EXTRA_DISTRITO_ID = "extra_distrito_id"
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[ReservaPaseoViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaseosRepository.inicializarCache(this)
        val authHeader = SessionManager.getAuthHeader(this)
        val userUuid = SessionManager.getUserUuid(this)
        val paseadorUuid = intent.getStringExtra(EXTRA_PASEADOR_UUID)
            ?: intent.getStringExtra("usuario_uuid")
            ?: intent.getStringExtra("uuid")
            ?: ""
        val paseadorNombre = intent.getStringExtra(EXTRA_PASEADOR_NOMBRE) ?: "Paseador"
        val tarifaHora = intent.getDoubleExtra(EXTRA_TARIFA_HORA, 25.0)
        val fotoUrl = intent.getStringExtra(EXTRA_FOTO_URL)
        val biografia = intent.getStringExtra(EXTRA_BIOGRAFIA)
        val distrito = intent.getStringExtra(EXTRA_DISTRITO)
        val distritoId = intent.getIntExtra(EXTRA_DISTRITO_ID, 0)

        viewModel.inicializarPaseador(
            uuid = paseadorUuid,
            nombre = paseadorNombre,
            tarifa = tarifaHora,
            foto = fotoUrl,
            bio = biografia,
            distritoNombre = distrito,
            distritoIdValor = distritoId
        )

        // Cargar mascotas reales del dueño y catálogo de distritos
        viewModel.cargarDatosCliente(authHeader, userUuid)

        // Cargar turnos iniciales del día seleccionado
        if (viewModel.diaSeleccionadoIso.isNotBlank() && paseadorUuid.isNotBlank()) {
            viewModel.cargarDisponibilidadFecha(authHeader, viewModel.diaSeleccionadoIso)
        }

        setContent {
            PaseaPETheme {
                var pasoActual by remember { mutableIntStateOf(1) }

                when (pasoActual) {
                    1 -> Paso1Screen(
                        viewModel = viewModel,
                        authHeader = authHeader,
                        onBack = { finish() },
                        onNext = { pasoActual = 2 }
                    )
                    2 -> Paso2Screen(
                        viewModel = viewModel,
                        authHeader = authHeader,
                        userUuid = userUuid,
                        onBack = { pasoActual = 1 },
                        onNext = { pasoActual = 3 }
                    )
                    3 -> Paso3Screen(
                        viewModel = viewModel,
                        authHeader = authHeader,
                        onBack = { pasoActual = 2 },
                        onConfirmSuccess = { codigoReserva ->
                            val msg = if (!codigoReserva.isNullOrBlank()) {
                                "¡Solicitud $codigoReserva enviada con éxito!"
                            } else {
                                "¡Solicitud de paseo enviada con éxito!"
                            }
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                            val intentPaseos = Intent(this, PaseosActivity::class.java).apply {
                                putExtra(PaseosActivity.EXTRA_TAB_INICIAL, 1)
                            }
                            startActivity(intentPaseos)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
