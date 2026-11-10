package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.paseape.data.SessionManager
import com.example.paseape.data.repository.PaseosRepository
import com.example.paseape.ui.theme.PaseaPETheme

class PaseosActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ABRIR_SOLICITUD_INMEDIATA = "extra_abrir_solicitud_inmediata"
        const val EXTRA_TAB_INICIAL = "extra_tab_inicial"
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[PaseosViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaseosRepository.inicializarCache(this)
        val authHeader = SessionManager.getAuthHeader(this)
        val userUuid = intent.getStringExtra("EXTRA_USER_UUID")
            ?: intent.getStringExtra("user_uuid")
            ?: SessionManager.getUserUuid(this)
        val abrirInmediata = intent.getBooleanExtra(EXTRA_ABRIR_SOLICITUD_INMEDIATA, false)
        val tabInicial = intent.getIntExtra(EXTRA_TAB_INICIAL, if (abrirInmediata) 3 else 1)

        viewModel.inicializarDatosCliente(authHeader, userUuid)

        setContent {
            PaseaPETheme {
                PaseosScreen(
                    viewModel = viewModel,
                    authHeader = authHeader,
                    userUuid = userUuid,
                    abrirInmediataInicial = abrirInmediata,
                    tabInicial = tabInicial,
                    onBack = { finish() }
                )
            }
        }
    }
}
