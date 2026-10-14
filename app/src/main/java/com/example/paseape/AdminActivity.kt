package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.SessionManager
import com.example.paseape.data.model.ClienteAdminDto
import com.example.paseape.data.model.PaseadorAdminDto
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaseaPETheme {
                AdminScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun AdminScreen(
    viewModel: AdminViewModel = remember { AdminViewModel() },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val authToken = remember(context) { SessionManager.getAuthHeader(context) }

    val tabs = listOf("Paseadores", "Clientes")

    LaunchedEffect(authToken) {
        viewModel.cargarDatos(authToken)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ENCABEZADO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = PrimaryDark
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Panel de Administrador",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    Text(
                        text = "Gestión de usuarios y validaciones",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // PESTAÑAS (TABS)
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryDark
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LISTADOS SEGÚN PESTAÑA
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = PrimaryDark)
                } else {
                    if (uiState.selectedTab == 0) {
                        // TAB PASEADORES
                        if (uiState.paseadores.isEmpty()) {
                            Text("No hay paseadores registrados", color = Color.Gray)
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.paseadores) { paseador ->
                                    AdminPaseadorCardReal(
                                        paseador = paseador,
                                        onClick = { viewModel.openPaseadorDialog(paseador) }
                                    )
                                }
                            }
                        }
                    } else {
                        // TAB CLIENTES
                        if (uiState.clientes.isEmpty()) {
                            Text("No hay clientes registrados", color = Color.Gray)
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.clientes) { cliente ->
                                    AdminClienteCardReal(
                                        cliente = cliente,
                                        onClick = { viewModel.openClienteDialog(cliente) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO DE ACTUALIZACIÓN DE ESTADO DE CLIENTE
    uiState.selectedClienteForDialog?.let { cliente ->
        ActualizarEstadoClienteDialog(
            cliente = cliente,
            isUpdating = uiState.isUpdating,
            onDismiss = { viewModel.closeClienteDialog() },
            onConfirm = { nuevoEstado ->
                val uuid = cliente.clienteIdentifier ?: ""
                viewModel.actualizarEstadoCliente(
                    token = authToken,
                    clienteUuid = uuid,
                    nuevoEstado = nuevoEstado
                )
            }
        )
    }

    // DIÁLOGO DE ACTUALIZACIÓN DE VERIFICACIÓN DE PASEADOR
    uiState.selectedPaseadorForDialog?.let { paseador ->
        ActualizarVerificacionPaseadorDialog(
            paseador = paseador,
            isUpdating = uiState.isUpdating,
            onDismiss = { viewModel.closePaseadorDialog() },
            onConfirm = { nuevoEstado ->
                val uuid = paseador.paseadorIdentifier ?: ""
                viewModel.actualizarVerificacionPaseador(
                    token = authToken,
                    paseadorUuid = uuid,
                    nuevoEstado = nuevoEstado
                )
            }
        )
    }
}

@Composable
fun AdminPaseadorCardReal(
    paseador: PaseadorAdminDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${paseador.nombres} ${paseador.apellidos}".trim(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = "Distrito: ${paseador.distrito}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toca para cambiar verificación",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }

            val estado = paseador.estadoVerificacion
            val (badgeBg, badgeText) = when (estado) {
                "APROBADO" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                "EN_REVISION" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
                "PENDIENTE" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                "OBSERVADO" -> Color(0xFFFFFDE7) to Color(0xFFF57F17)
                "RECHAZADO" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                else -> Color(0xFFF1F5F9) to Color(0xFF475569)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAprobado = estado == "APROBADO"
                    Icon(
                        imageVector = if (isAprobado) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Estado",
                        tint = badgeText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = estado,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }
            }
        }
    }
}

@Composable
fun AdminClienteCardReal(
    cliente: ClienteAdminDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${cliente.nombres} ${cliente.apellidos}".trim(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = cliente.correo ?: "Sin correo",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toca para cambiar estado",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }

            val estadoUser = cliente.estadoUsuario
            val (badgeBg, badgeText) = when (estadoUser) {
                "ACTIVO" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                "INACTIVO" -> Color(0xFFEEEEEE) to Color(0xFF757575)
                "PENDIENTE_ACTIVACION" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                "SUSPENDIDO" -> Color(0xFFFCE4EC) to Color(0xFFC2185B)
                "BLOQUEADO" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                else -> Color(0xFFF1F5F9) to Color(0xFF475569)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeBg
            ) {
                Text(
                    text = estadoUser,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeText
                )
            }
        }
    }
}