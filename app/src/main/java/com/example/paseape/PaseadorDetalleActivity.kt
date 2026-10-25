package com.example.paseape

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.paseape.data.SessionManager
import com.example.paseape.ui.theme.PaseaPETheme

class PaseadorDetalleActivity : ComponentActivity() {

    companion object {
        const val EXTRA_USUARIO_UUID = "extra_usuario_uuid"
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[PaseadorDetalleViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val usuarioUuid = intent.getStringExtra(EXTRA_USUARIO_UUID)
            ?: intent.getStringExtra("usuario_uuid")
            ?: intent.getStringExtra("uuid")
            ?: ""

        cargarDetalle(usuarioUuid)

        setContent {
            PaseaPETheme {
                val uiState by viewModel.uiState.collectAsState()

                PaseadorDetalleScreen(
                    uiState = uiState,
                    onBackClick = { finish() },
                    onRetry = { cargarDetalle(usuarioUuid) },
                    onReservarClick = {
                        val detalle = (uiState as? PaseadorDetalleUiState.Success)?.detalle
                        val paseador = detalle?.paseador
                        val intent = Intent(this@PaseadorDetalleActivity, ReservaPaseoActivity::class.java).apply {
                            putExtra(ReservaPaseoActivity.EXTRA_PASEADOR_UUID, paseador?.uuid ?: usuarioUuid)
                            putExtra(ReservaPaseoActivity.EXTRA_PASEADOR_NOMBRE, detalle?.nombreCompleto ?: "Paseador")
                            putExtra(ReservaPaseoActivity.EXTRA_TARIFA_HORA, paseador?.tarifaHoraPen ?: 25.0)
                            putExtra(ReservaPaseoActivity.EXTRA_FOTO_URL, detalle?.fotoPerfilUrl)
                            putExtra(ReservaPaseoActivity.EXTRA_BIOGRAFIA, paseador?.biografia)
                            putExtra(ReservaPaseoActivity.EXTRA_DISTRITO, paseador?.distritoCoberturaDescripcion)
                            putExtra(ReservaPaseoActivity.EXTRA_DISTRITO_ID, paseador?.distritoCoberturaId ?: 0)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun cargarDetalle(usuarioUuid: String) {
        val authHeader = SessionManager.getAuthHeader(this)
        viewModel.cargarDetalle(authHeader = authHeader, usuarioUuid = usuarioUuid)
    }
}
