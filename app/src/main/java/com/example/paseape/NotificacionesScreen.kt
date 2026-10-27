package com.example.paseape

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.NotificacionDto

enum class TipoIcono {
    EN_CURSO, CONFIRMADO, SOLICITUD, CANCELADO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    viewModel: NotificacionesViewModel,
    authHeader: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val naranja = Color(0xFFFF6B00)
    val notificaciones = viewModel.listaNotificaciones

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notificaciones",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.marcarTodasComoLeidas(
                                authHeader = authHeader,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Todas las notificaciones fueron marcadas como leídas",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = !viewModel.marcandoLeidas && notificaciones.isNotEmpty()
                    ) {
                        Text(
                            text = if (viewModel.marcandoLeidas) {
                                "Marcando..."
                            } else {
                                "Marcar todas como leídas"
                            },
                            color = naranja,
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
            when {
                viewModel.isLoading && notificaciones.isEmpty() -> {
                    CircularProgressIndicator(
                        color = naranja,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                viewModel.errorMessage != null && notificaciones.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.errorMessage ?: "Error al cargar notificaciones",
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.cargarNotificaciones(authHeader, incluirLeidas = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = naranja)
                        ) {
                            Text("Reintentar", color = Color.White)
                        }
                    }
                }

                notificaciones.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tienes notificaciones pendientes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cuando haya novedades sobre tus paseos aparecerán aquí.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = notificaciones,
                            key = { it.uuid ?: "${it.titulo}-${it.creadoEl}" }
                        ) { notif ->
                            CardNotificacionReal(
                                notificacion = notif,
                                onClick = {
                                    if (notif.leido != true) {
                                        viewModel.marcarNotificacionIndividualComoLeida(
                                            authHeader = authHeader,
                                            notificacion = notif
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardNotificacionReal(
    notificacion: NotificacionDto,
    onClick: () -> Unit = {}
) {
    // 1=SOLICITUD_RECIBIDA, 2=SOLICITUD_ACEPTADA, 3=SOLICITUD_RECHAZADA,
    // 4=PASEO_INICIADO, 5=PASEO_FINALIZADO, 6=SOLICITUD_CANCELADA
    val tipoIcono = when (notificacion.tipoNotificacionId) {
        2, 5 -> TipoIcono.CONFIRMADO
        4 -> TipoIcono.EN_CURSO
        3, 6 -> TipoIcono.CANCELADO
        else -> TipoIcono.SOLICITUD
    }

    // `creado_el` llega en UTC (el backend corre en UTC): convertimos a la hora local
    val fechaFormateada = remember(notificacion.creadoEl) {
        textoFechaHoraNotificacion(notificacion.creadoEl) ?: "Reciente"
    }

    val esNueva = notificacion.leido != true

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = esNueva, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = when (tipoIcono) {
                    TipoIcono.EN_CURSO -> Color(0xFFE3F2FD)
                    TipoIcono.CONFIRMADO -> Color(0xFFE8F5E9)
                    TipoIcono.SOLICITUD -> Color(0xFFFFF3E0)
                    TipoIcono.CANCELADO -> Color(0xFFFEF2F2)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (tipoIcono) {
                            TipoIcono.EN_CURSO -> Icons.Default.Send
                            TipoIcono.CONFIRMADO -> Icons.Default.Check
                            TipoIcono.SOLICITUD -> Icons.Default.DateRange
                            TipoIcono.CANCELADO -> Icons.Default.Cancel
                        },
                        contentDescription = null,
                        tint = when (tipoIcono) {
                            TipoIcono.EN_CURSO -> Color(0xFF1976D2)
                            TipoIcono.CONFIRMADO -> Color(0xFF388E3C)
                            TipoIcono.SOLICITUD -> Color(0xFFE65100)
                            TipoIcono.CANCELADO -> Color(0xFFDC2626)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notificacion.titulo ?: "Notificación de PaseaPe",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notificacion.mensaje.orEmpty(),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (esNueva) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF2563EB), shape = CircleShape)
                        )
                        Text(
                            text = fechaFormateada,
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
                            text = fechaFormateada,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
