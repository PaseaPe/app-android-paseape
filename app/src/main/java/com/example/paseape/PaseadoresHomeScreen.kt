package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark

data class SolicitudPaseo(
    val id: String,
    val nombreDueno: String,
    val nombreMascota: String,
    val raza: String,
    val duracion: String,
    val tarifa: String,
    val hora: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaseadoresHomeScreen(
    nombrePaseador: String = "Mateo",
    onBackClick: () -> Unit = {}
) {
    var estadoDisponible by remember { mutableStateOf(true) }

    val solicitudes = remember {
        mutableStateListOf(
            SolicitudPaseo("1", "Carlos Mendoza", "Firulais", "Golden Retriever", "60 min", "S/ 25.00", "10:30 AM"),
            SolicitudPaseo("2", "Ana Torres", "Rocky", "Bulldog Francés", "30 min", "S/ 15.00", "11:45 AM")
        )
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
                            text = if (estadoDisponible) "Disponible para paseos" else "Modo Ocupado",
                            fontSize = 12.sp,
                            color = if (estadoDisponible) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = PrimaryDark
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = estadoDisponible,
                        onCheckedChange = { estadoDisponible = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryDark
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EstadisticaItem(titulo = "Paseos hoy", valor = "3")
                        Divider(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp),
                            color = Color.LightGray
                        )
                        EstadisticaItem(titulo = "Ganancia hoy", valor = "S/ 65.00")
                        Divider(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp),
                            color = Color.LightGray
                        )
                        EstadisticaItem(titulo = "Rating", valor = "⭐ 5.0")
                    }
                }
            }

            item {
                Text(
                    text = "Solicitudes pendientes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }

            if (solicitudes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes solicitudes pendientes en este momento.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(solicitudes) { solicitud ->
                    TarjetaSolicitud(
                        solicitud = solicitud,
                        onAceptar = { solicitudes.remove(solicitud) },
                        onRechazar = { solicitudes.remove(solicitud) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun EstadisticaItem(titulo: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = valor, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
        Text(text = titulo, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun TarjetaSolicitud(
    solicitud: SolicitudPaseo,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryDark.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = PrimaryDark
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${solicitud.nombreMascota} (${solicitud.raza})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PrimaryDark
                        )
                        Text(
                            text = "Dueño: ${solicitud.nombreDueno}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Text(
                    text = solicitud.tarifa,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${solicitud.duracion} • ${solicitud.hora}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRechazar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Rechazar", color = Color.Red, fontSize = 13.sp)
                }
                Button(
                    onClick = onAceptar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                ) {
                    Text("Aceptar", fontSize = 13.sp)
                }
            }
        }
    }
}