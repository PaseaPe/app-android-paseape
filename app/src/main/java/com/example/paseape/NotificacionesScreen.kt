package com.example.paseape.ui.screens.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.viewmodel.NotificacionesViewModel

// Modelo de datos simple para una notificación
data class NotificacionItem(
    val id: String,
    val titulo: String,
    val mensaje: String,
    val tiempo: String,
    val esNueva: Boolean,
    val tipoIcono: TipoIcono
)

enum class TipoIcono {
    EN_CURSO, CONFIRMADO, SOLICITUD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    viewModel: NotificacionesViewModel,
    onBack: () -> Unit,
    onMarcarTodasLeidas: () -> Unit = {}
) {
    // Mock data basado en tu diseño de referencia
    val notificaciones = remember {
        mutableStateListOf(
            NotificacionItem(
                id = "1",
                titulo = "¡El paseo de Bobby acaba de comenzar!",
                mensaje = "Roberto Gómez inició el recorrido a las 08:05 AM desde tu domicilio.",
                tiempo = "Hace 5 min",
                esNueva = true,
                tipoIcono = TipoIcono.EN_CURSO
            ),
            NotificacionItem(
                id = "2",
                titulo = "¡Paseo Confirmado!",
                mensaje = "Lucía Vega aceptó tu solicitud para el Miércoles 20 a las 10:00 AM.",
                tiempo = "Hace 40 min",
                esNueva = true,
                tipoIcono = TipoIcono.CONFIRMADO
            ),
            NotificacionItem(
                id = "3",
                titulo = "Nueva solicitud de paseo",
                mensaje = "Valeria Vela te envió una solicitud para pasear a Zeus.",
                tiempo = "Hace 2 horas",
                esNueva = false,
                tipoIcono = TipoIcono.SOLICITUD
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    TextButton(onClick = onMarcarTodasLeidas) {
                        Text(
                            text = "Marcar todas como leídas",
                            color = Color(0xFFFF6B00),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            if (notificaciones.isEmpty()) {
                Text(
                    text = "No tienes notificaciones.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notificaciones) { notif ->
                        CardNotificacionItem(notificacion = notif)
                    }
                }
            }
        }
    }
}

@Composable
fun CardNotificacionItem(notificacion: NotificacionItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icono circular según el tipo de notificación
            Surface(
                shape = CircleShape,
                color = when (notificacion.tipoIcono) {
                    TipoIcono.EN_CURSO -> Color(0xFFE3F2FD)
                    TipoIcono.CONFIRMADO -> Color(0xFFE8F5E9)
                    TipoIcono.SOLICITUD -> Color(0xFFFFF3E0)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (notificacion.tipoIcono) {
                            TipoIcono.EN_CURSO -> Icons.Default.Send
                            TipoIcono.CONFIRMADO -> Icons.Default.Check
                            TipoIcono.SOLICITUD -> Icons.Default.DateRange
                        },
                        contentDescription = null,
                        tint = when (notificacion.tipoIcono) {
                            TipoIcono.EN_CURSO -> Color(0xFF1976D2)
                            TipoIcono.CONFIRMADO -> Color(0xFF388E3C)
                            TipoIcono.SOLICITUD -> Color(0xFFE65100)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Textos y detalles
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notificacion.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notificacion.mensaje,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Fila inferior: indicador NUEVA y tiempo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notificacion.esNueva) {
                        // Punto indicador azul
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF2563EB), shape = CircleShape)
                        )
                        Text(
                            text = notificacion.tiempo,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "NUEVA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    } else {
                        Text(
                            text = notificacion.tiempo,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}