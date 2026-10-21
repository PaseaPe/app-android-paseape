package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaseosScreen(
    viewModel: PaseosViewModel,
    onBack: () -> Unit
) {
    // Estado para la pestaña seleccionada (0: Activos, 1: Pendientes, 2: Historial)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Activos (1)", "Pendientes (1)", "Historial (1)")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Paseos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // =========================================================================
            // BARRA DE PESTAÑAS (TABS)
            // =========================================================================
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color(0xFFFF6B00) else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // VISTA MOCKUP (Organizada por Pestaña)
            // =========================================================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> item { CardPaseoEnCurso() }     // Pestaña Activos (Primer cuadro)
                    1 -> item { CardPaseoPendiente() }   // Pestaña Pendientes (Segundo cuadro)
                    2 -> item { CardPaseoHistorial() }   // Pestaña Historial (Tercer cuadro)
                }
            }

            // =========================================================================
            // CÓDIGO REAL DE CONEXIÓN CON VM (Descomentar cuando integres con la API)
            // =========================================================================
            /*
            when {
                viewModel.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                viewModel.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Error: ${viewModel.errorMessage}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    // Filtrar la lista según el estado del paseo y selectedTab
                }
            }
            */
        }
    }
}

// =========================================================================
// TARJETAS INDIVIDUALES
// =========================================================================

@Composable
fun CardPaseoEnCurso() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Bobby", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "SOL-202610-0012", color = Color.Gray, fontSize = 12.sp)
                }
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "EN CURSO",
                        color = Color(0xFF1976D2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Paseador: Roberto Gómez • Mascota Bobby", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "• Paseo iniciado a las 08:05 AM / 25 min transcurridos",
                    color = Color(0xFF2563EB),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { /* Acción llamar */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Llamar Paseador", color = Color(0xFFFF6B00))
                }
                Button(
                    onClick = { /* Acción ver mapa */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Ver Mapa en Vivo", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CardPaseoPendiente() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Lucía Vega", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "SOL-202610-0015", color = Color.Gray, fontSize = 12.sp)
                }
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "PENDIENTE",
                        color = Color(0xFFE65100),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Esperando respuesta de Lucía Vega.", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* Acción cancelar */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B00))
            ) {
                Text("Cancelar Solicitud")
            }
        }
    }
}

@Composable
fun CardPaseoHistorial() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Paseo de ayer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "SOL-202610-0009", color = Color.Gray, fontSize = 12.sp)
                }
                Surface(
                    color = Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "FINALIZADO",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Paseo completado ayer con éxito.", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* Acción calificar */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B00))
            ) {
                Text("Calificar servicio ⭐")
            }
        }
    }
}