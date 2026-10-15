package com.example.paseape

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark

@Composable
fun WalkerDashboardScreen(
    token: String,
    usuarioUuid: String,
    viewModel: WalkerProfileViewModel,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    val distritos by viewModel.distritos.collectAsState()
    val isOperationLoading by viewModel.isOperationLoading.collectAsState()

    // Escuchar mensajes para el Snackbar
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Cargar perfil al iniciar
    LaunchedEffect(usuarioUuid) {
        viewModel.cargarPerfil(token = token, usuarioUuid = usuarioUuid)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Inicio"
                        )
                    },
                    label = { Text("Inicio", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryDark,
                        selectedTextColor = PrimaryDark,
                        indicatorColor = PrimaryDark.copy(alpha = 0.12f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil"
                        )
                    },
                    label = { Text("Perfil", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryDark,
                        selectedTextColor = PrimaryDark,
                        indicatorColor = PrimaryDark.copy(alpha = 0.12f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> WalkerHomeContent(
                    uiState = uiState,
                    onLogoutClick = onLogoutClick
                )
                1 -> WalkerProfileScreen(
                    uiState = uiState,
                    distritos = distritos,
                    isOperationLoading = isOperationLoading,
                    onRetry = { viewModel.cargarPerfil(token = token, usuarioUuid = usuarioUuid) },
                    onActualizarPerfil = { req, onSuccess ->
                        viewModel.actualizarPerfil(
                            token = token,
                            usuarioUuid = usuarioUuid,
                            req = req,
                            onSuccess = onSuccess
                        )
                    },
                    onActualizarCobertura = { distritoId, onSuccess ->
                        viewModel.actualizarCobertura(
                            token = token,
                            usuarioUuid = usuarioUuid,
                            distritoId = distritoId,
                            onSuccess = onSuccess
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkerHomeContent(
    uiState: WalkerProfileUiState,
    onLogoutClick: () -> Unit
) {
    var disponible by remember { mutableStateOf(true) }

    val nombrePaseador = when (uiState) {
        is WalkerProfileUiState.Success -> uiState.detalle.nombres ?: "Paseador"
        else -> "Paseador"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hola, $nombrePaseador 👋",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                        Text(
                            text = if (disponible) "Disponible para paseos" else "Modo No Disponible",
                            fontSize = 12.sp,
                            color = if (disponible) Color(0xFF2E7D32) else Color(0xFF64748B)
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = disponible,
                        onCheckedChange = { disponible = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryDark
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = PrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundOffWhite)
            )
        },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Métricas Rápidas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                            Text(text = "Paseos hoy", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "S/ 0.00", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                            Text(text = "Ganancia hoy", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "⭐ 5.0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                            Text(text = "Rating", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            // Banner Informativo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryDark.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Panel de Paseador Activo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mantén tu disponibilidad activada y tu distrito de cobertura al día para recibir solicitudes de paseo de clientes cercanos.",
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Solicitudes Pendientes (Placeholder Informativo)
            item {
                Text(
                    text = "Solicitudes de Paseo Pendientes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tienes solicitudes pendientes en este momento.",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
