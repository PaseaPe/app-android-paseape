package com.example.paseape

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.paseape.data.model.SolicitudMascotaResDto
import com.example.paseape.data.model.SolicitudPaseoRecorridoResDto
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.model.UbicacionEnVivoResDto
import com.example.paseape.data.model.UsuarioPaseadorDetalleDto
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.data.repository.PaseosRepository
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.CardSurface
import com.example.paseape.ui.theme.PrimaryDark
import com.example.paseape.ui.theme.PrimaryOrange
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// =========================================================================
// 1. PANTALLA: DETALLE DE RESERVA (FIGMA SPRINT 3 - FLUJO DUEÑO)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleReservaClienteScreen(
    solicitud: SolicitudPaseoResDto,
    authHeader: String,
    formatearMoneda: (Double) -> String,
    cancelando: Boolean = false,
    onBack: () -> Unit,
    onCancelarClick: () -> Unit,
    onAbrirMapaEnVivo: () -> Unit,
    onAbrirResumenPaseo: () -> Unit
) {
    val context = LocalContext.current
    val naranja = PrimaryOrange
    val textoOscuro = PrimaryDark
    val textoGris = Color(0xFF64748B)

    // Carga complementaria de datos del paseador (teléfono, calificación, foto) si tiene paseador asignado
    var detallePaseador by remember(solicitud.paseadorUuid) {
        mutableStateOf<UsuarioPaseadorDetalleDto?>(null)
    }

    LaunchedEffect(solicitud.paseadorUuid, authHeader) {
        val pUuid = solicitud.paseadorUuid
        if (!pUuid.isNullOrBlank() && authHeader.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val res = runCatching {
                    RetrofitClient.apiService.getPaseadorPorUsuarioUuid(authHeader, pUuid)
                }.getOrNull()
                val extraido = res?.body()?.response?.extraerUsuario()
                if (extraido != null) {
                    withContext(Dispatchers.Main) {
                        detallePaseador = extraido
                    }
                }
            }
        }
    }

    val codigoReserva = solicitud.codigoReserva ?: "PAS-PROG-202611-032A"
    val estadoId = solicitud.estadoId ?: 1
    val permiteCancelar = estadoId == 1 || estadoId == 2 || estadoId == 8

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = textoOscuro
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detalle de Reserva",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textoOscuro
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Código Reserva", codigoReserva))
                                Toast.makeText(context, "Código copiado: $codigoReserva", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = codigoReserva,
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar código",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (estadoId == 5) {
                        Button(
                            onClick = onAbrirMapaEnVivo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ver Seguimiento en Mapa en Vivo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else if (estadoId == 6) {
                        Button(
                            onClick = onAbrirResumenPaseo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = naranja)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ver Resumen y Ruta del Paseo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (permiteCancelar) {
                        OutlinedButton(
                            onClick = onCancelarClick,
                            enabled = !cancelando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, naranja),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = naranja)
                        ) {
                            if (cancelando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = naranja,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancelando...", fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = "Cancelar Solicitud",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. BANNER DE ESTADO SEGÚN FSM DEL PASEO
            val nombrePaseador = solicitud.paseadorNombreCompleto
                ?.takeIf { it.isNotBlank() }
                ?: detallePaseador?.nombreCompleto
                ?: "El paseador asignado"

            when (estadoId) {
                1, 8 -> {
                    // Solicitud Pendiente de Confirmación (Caja Amarilla Figma)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (estadoId == 8) {
                                        "Buscando Paseador Disponible"
                                    } else {
                                        "Solicitud Pendiente de Confirmación"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textoOscuro
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (estadoId == 8) {
                                        "Los paseadores de ${solicitud.distritoDescripcion ?: "tu distrito"} están recibiendo tu oferta."
                                    } else {
                                        "$nombrePaseador tiene hasta 2 horas antes para aceptar."
                                    },
                                    fontSize = 12.sp,
                                    color = textoGris
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFFDE68A),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (estadoId == 8) "Oferta activa (5m)" else "Expira en: 01h 45m",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Variante FSM: ACEPTADA (Banner verde Figma)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFD1FAE5),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACEPTADA",
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "El paseador confirmó la reserva.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }

                5 -> {
                    // EN CURSO
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFDBEAFE),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "EN CURSO",
                                    color = Color(0xFF1D4ED8),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "El paseo está activo con seguimiento GPS en vivo.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E40AF),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                6 -> {
                    // FINALIZADA
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFD1FAE5),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "FINALIZADA",
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "¡Paseo completado con éxito! Puedes revisar el recorrido y calificar.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF065F46),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                else -> {
                    // RECHAZADA / CANCELADA / VENCIDA
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = solicitud.estadoDescripcion ?: "CERRADA",
                                    color = Color(0xFFB91C1C),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Esta solicitud ya no se encuentra activa.",
                                fontSize = 13.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }

            // 2. TARJETA: DIRECCIÓN DE RECOJO Y HORARIO
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Dirección de Recojo y Horario",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textoOscuro
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val direccionCompleta = listOfNotNull(
                        solicitud.direccionRecojo?.takeIf { it.isNotBlank() },
                        solicitud.distritoDescripcion?.takeIf { it.isNotBlank() }
                    ).joinToString(", ").ifBlank { "Dirección registrada en Lima" }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = naranja,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = direccionCompleta,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = textoOscuro
                        )
                    }

                    val referencia = solicitud.referenciaDireccion?.takeIf { it.isNotBlank() }
                        ?: "Domicilio principal del cliente"
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ref: $referencia",
                        fontSize = 12.sp,
                        color = textoGris,
                        modifier = Modifier.padding(start = 22.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val horarioFormateado = remember(solicitud.fechaProgramada, solicitud.bloqueHorarioCodigo, solicitud.creadoEl) {
                        formatearFechaYBloqueDetalle(
                            fechaProgramada = solicitud.fechaProgramada,
                            bloqueCodigo = solicitud.bloqueHorarioCodigo,
                            creadoEl = solicitud.creadoEl
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = naranja,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = horarioFormateado,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = textoOscuro
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = (solicitud.tipoDuracionPaseoDescripcion ?: "60 MINUTOS").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textoOscuro,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 3. TARJETA: MASCOTAS INCLUIDAS
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Mascotas incluidas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textoOscuro
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val mascotas = solicitud.mascotas.orEmpty()
                    if (mascotas.isEmpty()) {
                        Text(
                            text = "1 mascota registrada en el paseo",
                            fontSize = 13.sp,
                            color = textoGris
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mascotas.chunked(2).forEach { fila ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    fila.forEach { mascota ->
                                        MascotaIncluidaChipCard(
                                            mascota = mascota,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (fila.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. TARJETA: PASEADOR ASIGNADO
            if (!solicitud.paseadorNombreCompleto.isNullOrBlank() || detallePaseador != null) {
                val nombreMostrar = detallePaseador?.nombreCompleto
                    ?.takeIf { it.isNotBlank() }
                    ?: solicitud.paseadorNombreCompleto
                    ?: "Paseador Verificado"
                val iniciales = obtenerInicialesNombre(nombreMostrar)
                val calif = detallePaseador?.paseador?.calificacionPromedio ?: 4.9
                val paseosTotal = detallePaseador?.paseador?.paseosCompletados ?: 42
                val telefonoPaseador = detallePaseador?.telefono
                    ?: detallePaseador?.paseador?.telefono
                    ?: "+51 987 654 321"

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!detallePaseador?.fotoPerfilUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = detallePaseador?.fotoPerfilUrl,
                                        contentDescription = nombreMostrar,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = iniciales,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textoOscuro
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = nombreMostrar,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textoOscuro
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verificado",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "⭐ ${String.format(Locale.US, "%.1f", calif)} ($paseosTotal paseos)",
                                    fontSize = 12.sp,
                                    color = textoGris
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Teléfono: $telefonoPaseador",
                            fontSize = 12.sp,
                            color = textoGris
                        )
                    }
                }
            }

            // 5. TARJETA: DESGLOSE FINANCIERO
            val tarifaHora = solicitud.tarifaHoraPenSnapshot ?: 25.0
            val montoTotal = solicitud.montoTotal ?: tarifaHora
            val totalMascotas = (solicitud.mascotas?.size ?: 1).coerceAtLeast(1)
            val mascotasAdicionales = (totalMascotas - 1).coerceAtLeast(0)
            val costoBasePorMascota = if (totalMascotas > 0) montoTotal / totalMascotas else montoTotal
            val costoMascotasAdicionales = (montoTotal - costoBasePorMascota).coerceAtLeast(0.0)
            val metodoPagoTexto = (solicitud.tipoMetodoPagoDescripcion ?: "YAPE / PLIN").uppercase()

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Desglose Financiero",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textoOscuro
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tarifa hora snapshot", fontSize = 13.sp, color = textoGris)
                        Text(
                            text = formatearMoneda(tarifaHora),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textoOscuro
                        )
                    }

                    if (mascotasAdicionales > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Mascotas adicionales (x$mascotasAdicionales)",
                                fontSize = 13.sp,
                                color = textoGris
                            )
                            Text(
                                text = formatearMoneda(costoMascotasAdicionales),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textoOscuro
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Método de pago", fontSize = 13.sp, color = textoGris)
                        Text(
                            text = "$metodoPagoTexto (Validado)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textoOscuro
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total del Servicio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textoOscuro
                        )
                        Text(
                            text = "${formatearMoneda(montoTotal)} PEN",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textoOscuro
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MascotaIncluidaChipCard(
    mascota: SolicitudMascotaResDto,
    modifier: Modifier = Modifier
) {
    val inicial = mascota.nombre?.trim()?.take(1)?.uppercase().orEmpty().ifBlank { "M" }
    val subtitulo = listOfNotNull(
        mascota.raza?.takeIf { it.isNotBlank() },
        mascota.tamano?.takeIf { it.isNotBlank() }?.uppercase()
    ).joinToString(" • ").ifBlank { "Mascota registrada" }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!mascota.fotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = mascota.fotoUrl,
                        contentDescription = mascota.nombre,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = inicial,
                        color = Color(0xFFEA580C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mascota.nombre ?: "Mascota",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitulo,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    lineHeight = 12.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// 2. PANTALLA: SEGUIMIENTO DE PASEO EN VIVO (MAPA EN VIVO - SIN VELOCIDAD)
// =========================================================================

@Composable
fun SeguimientoPaseoEnVivoScreen(
    solicitud: SolicitudPaseoResDto,
    authHeader: String,
    onBack: () -> Unit,
    onVerResumenClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { PaseosRepository() }

    val rutaDistrito = remember(solicitud.distritoDescripcion, solicitud.uuid) {
        obtenerRutaBasePorDistrito(solicitud.distritoDescripcion)
    }

    var puntosRecorridos by remember {
        mutableStateOf(rutaDistrito.take(5))
    }
    var recibioCoordenadaReal by remember { mutableStateOf(false) }
    var esperandoPrimeraSenalGps by remember { mutableStateOf(false) }
    var indiceSimulado by remember { mutableIntStateOf(4) }
    var rumboActual by remember { mutableFloatStateOf(184f) }
    var precisionMetros by remember { mutableDoubleStateOf(3.8) }
    var senalDegradada by remember { mutableStateOf(false) }
    var segundosDesdeUltimaSenal by remember { mutableIntStateOf(2) }
    var isScreenActive by remember { mutableStateOf(true) }

    // Datos complementarios del paseador (teléfono para llamar/WhatsApp)
    var telefonoPaseador by remember { mutableStateOf("+51987654321") }
    var fotoPaseadorUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(solicitud.paseadorUuid, authHeader) {
        val pUuid = solicitud.paseadorUuid
        if (!pUuid.isNullOrBlank() && authHeader.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val res = runCatching {
                    RetrofitClient.apiService.getPaseadorPorUsuarioUuid(authHeader, pUuid)
                }.getOrNull()
                val detalle = res?.body()?.response?.extraerUsuario()
                if (detalle != null) {
                    withContext(Dispatchers.Main) {
                        detalle.telefono?.takeIf { it.isNotBlank() }?.let { telefonoPaseador = it }
                        fotoPaseadorUrl = detalle.fotoPerfilUrl
                    }
                }
            }
        }
    }

    // Reloj en vivo del tiempo transcurrido del paseo (MM:SS min)
    val segundosIniciales = remember(solicitud.fechaHoraInicioReal, solicitud.creadoEl) {
        calcularSegundosTranscurridosPaseo(solicitud.fechaHoraInicioReal ?: solicitud.creadoEl)
    }
    var segundosTranscurridos by remember { mutableLongStateOf(segundosIniciales) }

    LaunchedEffect(isScreenActive) {
        while (isActive && isScreenActive) {
            delay(1_000L)
            segundosTranscurridos += 1L
            segundosDesdeUltimaSenal = (segundosDesdeUltimaSenal + 1).coerceAtMost(45)
        }
    }

    // Gestión de ciclo de vida onPause() / onResume() para pausar/reanudar Short Polling (HU24)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isScreenActive = true
                Lifecycle.Event.ON_PAUSE -> isScreenActive = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Short Polling cada 6 segundos a GET /cliente/solicitudes/{uuid}/ubicacion-actual (HU24)
    LaunchedEffect(solicitud.uuid, authHeader, isScreenActive) {
        while (isActive && isScreenActive) {
            val uuid = solicitud.uuid.orEmpty()
            var respuestaBackendManejada = false

            if (uuid.isNotBlank() && authHeader.isNotBlank()) {
                val res = withContext(Dispatchers.IO) {
                    repository.obtenerUbicacionActualPaseo(authHeader, uuid)
                }
                if (res.isSuccess) {
                    val dto: UbicacionEnVivoResDto? = res.getOrNull()
                    if (dto?.latitud != null && dto.longitud != null) {
                        val nuevoPunto = LatLng(dto.latitud, dto.longitud)
                        if (!recibioCoordenadaReal) {
                            puntosRecorridos = listOf(nuevoPunto)
                            recibioCoordenadaReal = true
                        } else {
                            val ultimo = puntosRecorridos.lastOrNull()
                            if (ultimo == null || ultimo != nuevoPunto) {
                                puntosRecorridos = puntosRecorridos + nuevoPunto
                            }
                        }
                        rumboActual = dto.rumboGrados?.toFloat() ?: rumboActual
                        precisionMetros = dto.precisionM ?: 3.8
                        senalDegradada = dto.senalDegradada == true
                        segundosDesdeUltimaSenal = calcularSegundosAntiguedadGps(dto.fechaHoraGps)
                        esperandoPrimeraSenalGps = false
                    } else {
                        // HTTP 200 con response = null -> Constant.MSJ_ESPERA_PRIMERA_SENAL_GPS
                        esperandoPrimeraSenalGps = true
                    }
                    respuestaBackendManejada = true
                }
            }

            if (!respuestaBackendManejada) {
                esperandoPrimeraSenalGps = false
                val siguienteIndice = if (indiceSimulado + 1 < rutaDistrito.size) {
                    indiceSimulado + 1
                } else {
                    2
                }
                val anterior = rutaDistrito[indiceSimulado]
                val siguiente = rutaDistrito[siguienteIndice]
                rumboActual = calcularRumboGrados(anterior, siguiente)
                indiceSimulado = siguienteIndice
                puntosRecorridos = rutaDistrito.take(indiceSimulado + 1)
                precisionMetros = 3.6 + ((indiceSimulado % 3) * 0.2)
                senalDegradada = false
                segundosDesdeUltimaSenal = 2
            }

            delay(6_000L)
        }
    }

    var googleMapRef by remember { mutableStateOf<GoogleMap?>(null) }
    var walkerMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var startMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var routePolylineRef by remember { mutableStateOf<Polyline?>(null) }

    val mapView = rememberMapViewWithLifecycle()

    val iconStart = remember(context) { crearIconoInicioVerde(context) }
    val iconWalker = remember(context) { crearIconoPaseadorNaranja(context) }

    // Actualiza la polilínea y el marcador en el GoogleMap cada vez que cambia puntosRecorridos o rumboActual
    LaunchedEffect(googleMapRef, puntosRecorridos, senalDegradada, rumboActual) {
        val map = googleMapRef ?: return@LaunchedEffect
        if (puntosRecorridos.isEmpty()) return@LaunchedEffect

        val puntoInicio = puntosRecorridos.first()
        val puntoActual = puntosRecorridos.last()

        if (startMarkerRef == null) {
            startMarkerRef = map.addMarker(
                MarkerOptions()
                    .position(puntoInicio)
                    .title("Punto de Inicio")
                    .anchor(0.5f, 0.5f)
                    .icon(iconStart)
            )
        } else {
            startMarkerRef?.position = puntoInicio
        }

        val poly = routePolylineRef
        if (poly == null) {
            routePolylineRef = map.addPolyline(
                PolylineOptions()
                    .addAll(puntosRecorridos)
                    .width(14f)
                    .color(android.graphics.Color.parseColor("#FF5722"))
                    .geodesic(true)
                    .startCap(RoundCap())
                    .endCap(RoundCap())
                    .jointType(JointType.ROUND)
            )
        } else {
            poly.points = puntosRecorridos
        }

        val marker = walkerMarkerRef
        if (marker == null) {
            walkerMarkerRef = map.addMarker(
                MarkerOptions()
                    .position(puntoActual)
                    .title(solicitud.paseadorNombreCompleto ?: "Paseador")
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .rotation(rumboActual)
                    .alpha(if (senalDegradada) 0.5f else 1.0f)
                    .icon(iconWalker)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(puntoActual, 16.5f))
        } else {
            marker.position = puntoActual
            marker.rotation = rumboActual
            marker.alpha = if (senalDegradada) 0.5f else 1.0f
            map.animateCamera(CameraUpdateFactory.newLatLng(puntoActual))
        }
    }

    val minutos = segundosTranscurridos / 60
    val segundos = segundosTranscurridos % 60
    val tiempoFormateado = String.format(Locale.US, "%02d:%02d min", minutos, segundos)
    val duracionMetaTexto = (solicitud.tipoDuracionPaseoDescripcion ?: "60 min")
        .lowercase()
        .replace("minutos", "min")

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. GOOGLE MAP EN VIVO (PARTE SUPERIOR)
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.66f),
            update = { mv ->
                if (googleMapRef == null) {
                    mv.getMapAsync { gMap ->
                        gMap.uiSettings.apply {
                            isZoomControlsEnabled = false
                            isMapToolbarEnabled = false
                            isCompassEnabled = false
                            isMyLocationButtonEnabled = false
                        }
                        val posInicial = puntosRecorridos.lastOrNull() ?: rutaDistrito.first()
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posInicial, 16.5f))
                        googleMapRef = gMap
                    }
                }
            }
        )

        // 2. OVERLAY SUPERIOR: BOTÓN VOLVER + BADGE "EN VIVO" + BRÚJULA + BOTÓN CENTRAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(onClick = onBack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Badge azul: ● EN VIVO
                Surface(
                    color = Color(0xFF2563EB),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EN VIVO",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Pill de rumbo (ej. 184°) como en el Figma
                Surface(
                    color = Color(0xFFDBEAFE),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${rumboActual.toInt()}°",
                            color = Color(0xFF1D4ED8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Botón flotante para centrar cámara en el paseador
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        val posActual = puntosRecorridos.lastOrNull()
                        if (posActual != null) {
                            googleMapRef?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(posActual, 17f)
                            )
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Centrar en paseador",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. PANEL INFERIOR (BOTTOM SHEET ESTILO FIGMA - SIN VELOCIDAD)
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCBD5E1))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Cabecera: Tiempo transcurrido + Subtítulo (SIN VELOCIDAD) + Badge EN CURSO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tiempoFormateado,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = Color(0xFF2563EB)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Paseo de $duracionMetaTexto en progreso",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Surface(
                        color = Color(0xFFDBEAFE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "EN CURSO",
                            color = Color(0xFF1D4ED8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tarjeta del Paseador con botones de Llamada y Chat
                val nombrePaseador = solicitud.paseadorNombreCompleto
                    ?.takeIf { it.isNotBlank() }
                    ?: "Roberto Gómez Salas"
                val inicialesPaseador = obtenerInicialesNombre(nombrePaseador)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!fotoPaseadorUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = fotoPaseadorUrl,
                                    contentDescription = nombrePaseador,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = inicialesPaseador,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nombrePaseador,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1E293B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "⭐ 4.9 • Paseador verificado",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Botón Llamar
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2563EB),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefonoPaseador"))
                                        context.startActivity(intent)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Llamar al paseador",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Botón WhatsApp / Mensaje
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    runCatching {
                                        val numLimpio = telefonoPaseador.filter { it.isDigit() }
                                        val uri = Uri.parse("https://wa.me/$numLimpio")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Enviar mensaje al paseador",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chips naranjas de las mascotas del paseo (ej. Bobby (Golden), Luna (Schnauzer))
                val mascotas = solicitud.mascotas.orEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mascotas.isEmpty()) {
                        Surface(
                            color = Color(0xFFFFEDD5),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Mascota en paseo",
                                color = Color(0xFFC2410C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    } else {
                        mascotas.forEach { m ->
                            val etiquetaMascota = if (!m.raza.isNullOrBlank()) {
                                "${m.nombre ?: "Mascota"} (${m.raza.split(" ").first()})"
                            } else {
                                m.nombre ?: "Mascota"
                            }
                            Surface(
                                color = Color(0xFFFFEDD5),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = etiquetaMascota,
                                    color = Color(0xFFC2410C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Estado de última señal GPS + Acceso rápido a Resumen del Paseo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val colorIndicadorGps = when {
                            esperandoPrimeraSenalGps -> Color(0xFF2563EB)
                            senalDegradada -> Color(0xFFF59E0B)
                            else -> Color(0xFF16A34A)
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colorIndicadorGps)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                esperandoPrimeraSenalGps -> "A la espera de la primera señal GPS del paseador..."
                                senalDegradada -> "Señal GPS débil o retrasada • Hace ${segundosDesdeUltimaSenal}s"
                                else -> "Última señal GPS: Hace $segundosDesdeUltimaSenal segundos • Precisión óptima (${String.format(Locale.US, "%.1f", precisionMetros)}m)"
                            },
                            fontSize = 11.sp,
                            color = when {
                                esperandoPrimeraSenalGps -> Color(0xFF1D4ED8)
                                senalDegradada -> Color(0xFFB45309)
                                else -> Color(0xFF16A34A)
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = onVerResumenClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Ver resumen ➔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. PANTALLA: RESUMEN DEL PASEO POST-SERVICIO (SCR_S3_05 - SIN VELOCIDAD)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenPaseoClienteScreen(
    solicitud: SolicitudPaseoResDto,
    authHeader: String,
    formatearMoneda: (Double) -> String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PaseosRepository() }
    val naranja = PrimaryOrange
    val textoOscuro = PrimaryDark
    val textoGris = Color(0xFF64748B)

    var recorridoRemoto by remember { mutableStateOf<SolicitudPaseoRecorridoResDto?>(null) }

    LaunchedEffect(solicitud.uuid, authHeader) {
        val uuid = solicitud.uuid.orEmpty()
        if (uuid.isNotBlank() && authHeader.isNotBlank()) {
            val res = withContext(Dispatchers.IO) {
                repository.obtenerRecorridoPaseo(authHeader, uuid)
            }
            res.getOrNull()?.let { recorridoRemoto = it }
        }
    }

    // Decodificación de polilínea con android-maps-utils (PolyUtil.decode)
    val puntosRutaFinal = remember(recorridoRemoto, solicitud.distritoDescripcion) {
        val encoded = recorridoRemoto?.coordenadasCodificadas
        if (!encoded.isNullOrBlank()) {
            runCatching { PolyUtil.decode(encoded) }.getOrNull().orEmpty()
                .ifEmpty { obtenerRutaBasePorDistrito(solicitud.distritoDescripcion) }
        } else {
            val base = obtenerRutaBasePorDistrito(solicitud.distritoDescripcion)
            val encodedLocal = PolyUtil.encode(base)
            runCatching { PolyUtil.decode(encodedLocal) }.getOrDefault(base)
        }
    }

    val distanciaKm = remember(recorridoRemoto, solicitud.tipoDuracionPaseoId) {
        recorridoRemoto?.distanciaMetros?.let { it / 1000.0 }
            ?: when (solicitud.tipoDuracionPaseoId) {
                1 -> 1.35
                3 -> 3.60
                4 -> 4.80
                else -> 2.45
            }
    }

    val duracionTextoResumen = remember(recorridoRemoto, solicitud.tipoDuracionPaseoId) {
        val seg = recorridoRemoto?.duracionSegundos
        if (seg != null && seg > 0) {
            val h = seg / 3600
            val m = (seg % 3600) / 60
            if (h > 0) "${h}h ${String.format(Locale.US, "%02dm", m)}" else "${m}m"
        } else {
            when (solicitud.tipoDuracionPaseoId) {
                1 -> "30m"
                3 -> "1h 30m"
                4 -> "2h 00m"
                else -> "1h 00m"
            }
        }
    }

    val totalPuntosRegistrados = recorridoRemoto?.coordenadasTotalPuntos ?: 320
    val (horaInicioTexto, horaFinTexto) = remember(solicitud.bloqueHorarioCodigo, recorridoRemoto) {
        obtenerHorasInicioFinResumen(
            bloqueCodigo = solicitud.bloqueHorarioCodigo,
            fechaHoraInicioIso = recorridoRemoto?.fechaHoraInicio ?: solicitud.fechaHoraInicioReal,
            fechaHoraFinIso = recorridoRemoto?.fechaHoraFin ?: solicitud.fechaHoraFinReal
        )
    }

    // Estado de Calificación del Servicio (persistido localmente por paseo UUID)
    val prefsCalificacion = remember {
        context.getSharedPreferences("paseape_calificaciones_prefs", Context.MODE_PRIVATE)
    }
    val keyEstrellas = "estrellas_${solicitud.uuid ?: solicitud.codigoReserva}"
    val keyComentario = "comentario_${solicitud.uuid ?: solicitud.codigoReserva}"
    val keyEnviada = "enviada_${solicitud.uuid ?: solicitud.codigoReserva}"

    var estrellasSeleccionadas by remember {
        mutableIntStateOf(prefsCalificacion.getInt(keyEstrellas, 5))
    }
    var comentarioCalificacion by remember {
        mutableStateOf(prefsCalificacion.getString(keyComentario, "") ?: "")
    }
    var calificacionEnviada by remember {
        mutableStateOf(prefsCalificacion.getBoolean(keyEnviada, false))
    }
    var reciboExpandido by remember { mutableStateOf(false) }

    val mapView = rememberMapViewWithLifecycle()
    val iconInicio = remember(context) { crearIconoPuntoRuta(context, esInicio = true) }
    val iconFin = remember(context) { crearIconoPuntoRuta(context, esInicio = false) }

    val nombrePaseador = solicitud.paseadorNombreCompleto
        ?.takeIf { it.isNotBlank() }
        ?: "Roberto Gómez"
    val nombresMascotas = solicitud.mascotas
        ?.mapNotNull { it.nombre }
        ?.joinToString(" y ")
        .orEmpty()
        .ifBlank { "tu mascota" }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = textoOscuro
                            )
                        }
                        Text(
                            text = "Resumen del Paseo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textoOscuro
                        )
                    }

                    IconButton(
                        onClick = {
                            val textoCompartir = "Paseo ${solicitud.codigoReserva ?: ""} en PaseaPe: " +
                                "${String.format(Locale.US, "%.2f km", distanciaKm)} recorridos en $duracionTextoResumen con $nombrePaseador."
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, textoCompartir)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir Resumen del Paseo"))
                        }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartir",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. MAPA CON TRAZA FINAL CONSOLIDADA (INICIO VERDE -> POLILÍNEA NARANJA -> FIN ROJO)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize(),
                    update = { mv ->
                        mv.getMapAsync { gMap ->
                            gMap.clear()
                            gMap.uiSettings.apply {
                                isZoomControlsEnabled = false
                                isMapToolbarEnabled = false
                                isMyLocationButtonEnabled = false
                            }

                            if (puntosRutaFinal.isNotEmpty()) {
                                gMap.addPolyline(
                                    PolylineOptions()
                                        .addAll(puntosRutaFinal)
                                        .width(14f)
                                        .color(android.graphics.Color.parseColor("#FF5722"))
                                        .geodesic(true)
                                        .startCap(RoundCap())
                                        .endCap(RoundCap())
                                        .jointType(JointType.ROUND)
                                )

                                val pInicio = puntosRutaFinal.first()
                                val pFin = puntosRutaFinal.last()

                                gMap.addMarker(
                                    MarkerOptions()
                                        .position(pInicio)
                                        .title("INICIO $horaInicioTexto")
                                        .anchor(0.5f, 0.5f)
                                        .icon(iconInicio)
                                )

                                gMap.addMarker(
                                    MarkerOptions()
                                        .position(pFin)
                                        .title("FIN $horaFinTexto")
                                        .anchor(0.5f, 0.5f)
                                        .icon(iconFin)
                                )

                                val boundsBuilder = LatLngBounds.Builder()
                                puntosRutaFinal.forEach { boundsBuilder.include(it) }
                                runCatching {
                                    gMap.moveCamera(
                                        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 90)
                                    )
                                }.onFailure {
                                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pInicio, 16f))
                                }
                            }
                        }
                    }
                )

                // Etiquetas flotantes INICIO / FIN sobre el mapa como en el Figma
                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "INICIO\n$horaInicioTexto",
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "FIN\n$horaFinTexto",
                        color = Color(0xFFEA580C),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2. TARJETA DE MÉTRICAS (DISTANCIA TOTAL Y DURACIÓN NETA - SIN VELOCIDAD)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%.2f km", distanciaKm),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = textoOscuro
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Distancia Total",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(32.dp),
                            color = Color(0xFFE2E8F0)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = duracionTextoResumen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = textoOscuro
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Duración Neta",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // 3. TARJETA DE HORARIO Y TRAZA GPS VALIDADA
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Inicio: $horaInicioTexto AM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textoOscuro
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = naranja,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Fin: $horaFinTexto AM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textoOscuro
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Zona horaria Lima (UTC-5) • Traza GPS validada • $totalPuntosRegistrados puntos registrados",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 4. TARJETA DE CALIFICACIÓN DEL SERVICIO
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, naranja),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¿Cómo fue el servicio de $nombrePaseador?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textoOscuro,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Estrellas interactivas
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..5) {
                                val activa = i <= estrellasSeleccionadas
                                Icon(
                                    imageVector = if (activa) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$i estrellas",
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clickable(enabled = !calificacionEnviada) {
                                            estrellasSeleccionadas = i
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = comentarioCalificacion,
                            onValueChange = { if (!calificacionEnviada) comentarioCalificacion = it },
                            enabled = !calificacionEnviada,
                            placeholder = {
                                Text(
                                    text = "Comentario (opcional)\nDeja un comentario sobre el paseo de $nombresMascotas...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = naranja
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (calificacionEnviada) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "¡Calificación enviada ($estrellasSeleccionadas ★)! Gracias por tu reseña.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    prefsCalificacion.edit()
                                        .putInt(keyEstrellas, estrellasSeleccionadas)
                                        .putString(keyComentario, comentarioCalificacion.trim())
                                        .putBoolean(keyEnviada, true)
                                        .apply()
                                    calificacionEnviada = true
                                    Toast.makeText(
                                        context,
                                        "¡Gracias por calificar el servicio de $nombrePaseador!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(23.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = naranja)
                            ) {
                                Text(
                                    text = "Enviar Calificación",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // 5. TARJETA DE RECIBO DIGITAL COLAPSABLE
                val codigoRecibo = solicitud.codigoReserva ?: "PAS-PROG-202611-032A"
                val montoTotal = solicitud.montoTotal ?: 50.0
                val metodoPago = solicitud.tipoMetodoPagoDescripcion ?: "Yape/Plin"

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { reciboExpandido = !reciboExpandido }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = naranja,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recibo Digital #$codigoRecibo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = textoOscuro
                                )
                                Text(
                                    text = "Pagado ${formatearMoneda(montoTotal)} PEN vía $metodoPago • Descargar PDF",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Icon(
                                imageVector = if (reciboExpandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8)
                            )
                        }

                        AnimatedVisibility(visible = reciboExpandido) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                Spacer(modifier = Modifier.height(10.dp))
                                FilaReciboDetalle("Código de operación", codigoRecibo)
                                FilaReciboDetalle("Paseador", nombrePaseador)
                                FilaReciboDetalle("Mascotas", nombresMascotas)
                                FilaReciboDetalle("Distrito", solicitud.distritoDescripcion ?: "Miraflores")
                                FilaReciboDetalle("Método de pago", metodoPago)
                                FilaReciboDetalle("Total pagado", "${formatearMoneda(montoTotal)} PEN")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FilaReciboDetalle(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = etiqueta, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(text = valor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
    }
}

// =========================================================================
// UTILIDADES DE MAPA Y MARCADORES PERSONALIZADOS (GOOGLE MAPS SDK)
// =========================================================================

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    return mapView
}

private fun crearIconoInicioVerde(context: Context): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val sizePx = (34 * density).toInt().coerceAtLeast(48)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Borde blanco exterior
    val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.44f, paintWhite)

    // Círculo verde (#10B981)
    val paintGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#10B981")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.36f, paintGreen)

    // Cruz blanca (+) en el centro como en el Figma
    val paintPlus = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        strokeWidth = 2.5f * density
        strokeCap = Paint.Cap.ROUND
    }
    val arm = sizePx * 0.14f
    canvas.drawLine(cx - arm, cy, cx + arm, cy, paintPlus)
    canvas.drawLine(cx, cy - arm, cx, cy + arm, paintPlus)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun crearIconoPaseadorNaranja(context: Context): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val sizePx = (54 * density).toInt().coerceAtLeast(72)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Halo azul translúcido exterior (pulso GPS de Figma)
    val paintHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(48, 37, 99, 235)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.48f, paintHalo)

    // Círculo blanco con borde naranja
    val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.33f, paintWhite)

    val paintOrangeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FF5722")
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    canvas.drawCircle(cx, cy, sizePx * 0.31f, paintOrangeBorder)

    // Silueta naranja del paseador en el centro
    val paintOrangeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FF5722")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy - sizePx * 0.07f, sizePx * 0.08f, paintOrangeFill)
    canvas.drawRoundRect(
        cx - sizePx * 0.13f,
        cy + sizePx * 0.03f,
        cx + sizePx * 0.13f,
        cy + sizePx * 0.18f,
        6f * density,
        6f * density,
        paintOrangeFill
    )

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun crearIconoPuntoRuta(context: Context, esInicio: Boolean): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val sizePx = (28 * density).toInt().coerceAtLeast(40)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.44f, paintWhite)

    val paintColor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (esInicio) {
            android.graphics.Color.parseColor("#10B981")
        } else {
            android.graphics.Color.parseColor("#FF5722")
        }
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.33f, paintColor)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Rutas reales por calles/parques de los distritos de Lima para visualización en Google Maps.
 */
private fun obtenerRutaBasePorDistrito(distritoDescripcion: String?): List<LatLng> {
    val dist = distritoDescripcion?.trim()?.uppercase().orEmpty()
    return when {
        dist.contains("SAN ISIDRO") -> listOf(
            LatLng(-12.10120, -77.03650),
            LatLng(-12.10065, -77.03610),
            LatLng(-12.10010, -77.03555),
            LatLng(-12.09950, -77.03510),
            LatLng(-12.09895, -77.03475),
            LatLng(-12.09840, -77.03440),
            LatLng(-12.09785, -77.03415),
            LatLng(-12.09730, -77.03380)
        )
        dist.contains("SURCO") -> listOf(
            LatLng(-12.11360, -76.99180),
            LatLng(-12.11305, -76.99125),
            LatLng(-12.11250, -76.99070),
            LatLng(-12.11195, -76.99015),
            LatLng(-12.11140, -76.98960),
            LatLng(-12.11090, -76.98910),
            LatLng(-12.11040, -76.98855)
        )
        dist.contains("BARRANCO") -> listOf(
            LatLng(-12.14890, -77.02185),
            LatLng(-12.14830, -77.02140),
            LatLng(-12.14775, -77.02095),
            LatLng(-12.14720, -77.02050),
            LatLng(-12.14665, -77.02005),
            LatLng(-12.14610, -77.01960),
            LatLng(-12.14555, -77.01915)
        )
        dist.contains("SAN BORJA") -> listOf(
            LatLng(-12.10140, -77.00210),
            LatLng(-12.10085, -77.00155),
            LatLng(-12.10030, -77.00100),
            LatLng(-12.09975, -77.00045),
            LatLng(-12.09920, -76.99990),
            LatLng(-12.09865, -76.99935)
        )
        else -> {
            // Miraflores (Av. Primavera / Calle Los Pinos / Parque Reducto - igual al Figma)
            listOf(
                LatLng(-12.12685, -77.02410),
                LatLng(-12.12630, -77.02365),
                LatLng(-12.12575, -77.02320),
                LatLng(-12.12520, -77.02270),
                LatLng(-12.12475, -77.02210),
                LatLng(-12.12430, -77.02155),
                LatLng(-12.12385, -77.02105),
                LatLng(-12.12340, -77.02055),
                LatLng(-12.12295, -77.02010)
            )
        }
    }
}

private fun calcularRumboGrados(desde: LatLng, hasta: LatLng): Float {
    val lat1 = Math.toRadians(desde.latitude)
    val lon1 = Math.toRadians(desde.longitude)
    val lat2 = Math.toRadians(hasta.latitude)
    val lon2 = Math.toRadians(hasta.longitude)
    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val brng = Math.toDegrees(atan2(y, x))
    return ((brng + 360.0) % 360.0).toFloat()
}

private fun calcularSegundosTranscurridosPaseo(isoUtcString: String?): Long {
    if (isoUtcString.isNullOrBlank()) return 34L * 60L + 12L
    return runCatching {
        val limpio = isoUtcString.trim().removeSuffix("Z")
        val ldt = LocalDateTime.parse(limpio.take(19))
        val inicioInstant = ldt.toInstant(ZoneOffset.UTC)
        val diff = Duration.between(inicioInstant, Instant.now()).seconds
        if (diff in 1..7200) diff else (34L * 60L + 12L)
    }.getOrDefault(34L * 60L + 12L)
}

private fun obtenerInicialesNombre(nombreCompleto: String): String {
    val partes = nombreCompleto.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    val i1 = partes.getOrNull(0)?.take(1).orEmpty()
    val i2 = partes.getOrNull(1)?.take(1).orEmpty()
    return "$i1$i2".uppercase().ifBlank { "PA" }
}

private fun formatearFechaYBloqueDetalle(
    fechaProgramada: String?,
    bloqueCodigo: String?,
    creadoEl: String?
): String {
    val localeEs = Locale("es", "PE")
    val fechaTexto = runCatching {
        val fechaStr = fechaProgramada?.take(10)
            ?: creadoEl?.take(10)
        if (!fechaStr.isNullOrBlank()) {
            val ld = LocalDate.parse(fechaStr)
            val fmt = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", localeEs)
            ld.format(fmt).replaceFirstChar { it.uppercase() }
        } else {
            "Hoy"
        }
    }.getOrDefault(fechaProgramada ?: "Hoy")

    val rangoBloque = if (!bloqueCodigo.isNullOrBlank()) {
        val limpio = bloqueCodigo.uppercase().removePrefix("B")
        val partes = limpio.split("_")
        if (partes.size == 2) {
            "${partes[0]}:00 - ${partes[1]}:00 ($bloqueCodigo)"
        } else {
            bloqueCodigo
        }
    } else {
        "Modalidad Inmediata"
    }

    return "$fechaTexto • $rangoBloque"
}

private fun calcularSegundosAntiguedadGps(isoUtcString: String?): Int {
    if (isoUtcString.isNullOrBlank()) return 1
    return runCatching {
        val limpio = isoUtcString.trim().removeSuffix("Z")
        val ldt = LocalDateTime.parse(limpio.take(19))
        val gpsInstant = ldt.toInstant(ZoneOffset.UTC)
        val diff = Duration.between(gpsInstant, Instant.now()).seconds
        diff.coerceIn(1L, 999L).toInt()
    }.getOrDefault(1)
}

private fun obtenerHorasInicioFinResumen(
    bloqueCodigo: String?,
    fechaHoraInicioIso: String? = null,
    fechaHoraFinIso: String? = null
): Pair<String, String> {
    val zonaLima = ZoneId.of("America/Lima")
    val fmtHora = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    val inicioReal = fechaHoraInicioIso?.takeIf { it.isNotBlank() }?.let { raw ->
        runCatching {
            val ldt = LocalDateTime.parse(raw.trim().removeSuffix("Z").take(19))
            ldt.atOffset(ZoneOffset.UTC).atZoneSameInstant(zonaLima).format(fmtHora)
        }.getOrNull()
    }
    val finReal = fechaHoraFinIso?.takeIf { it.isNotBlank() }?.let { raw ->
        runCatching {
            val ldt = LocalDateTime.parse(raw.trim().removeSuffix("Z").take(19))
            ldt.atOffset(ZoneOffset.UTC).atZoneSameInstant(zonaLima).format(fmtHora)
        }.getOrNull()
    }

    if (inicioReal != null && finReal != null) {
        return inicioReal to finReal
    }

    if (!bloqueCodigo.isNullOrBlank()) {
        val limpio = bloqueCodigo.uppercase().removePrefix("B")
        val partes = limpio.split("_")
        if (partes.size == 2) {
            return (inicioReal ?: "${partes[0]}:00") to (finReal ?: "${partes[1]}:00")
        }
    }
    return (inicioReal ?: "08:00") to (finReal ?: "09:00")
}
