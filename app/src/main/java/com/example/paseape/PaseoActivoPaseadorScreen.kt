package com.example.paseape

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark
import com.example.paseape.ui.theme.PrimaryOrange
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TextoGrisActivo = Color(0xFF64748B)
private val BordeGrisActivo = Color(0xFFE2E8F0)
private val VerdeActivo = Color(0xFF16A34A)
private val VerdeSuaveActivo = Color(0xFFE7F8ED)
private val AmbarSuaveActivo = Color(0xFFFFF8E1)
private val AmbarTextoActivo = Color(0xFF8D6E00)
private val NaranjaSuaveActivo = Color(0xFFFFF3E0)

private val ZONA_LIMA = ZoneId.of("America/Lima")
private val FORMATO_HORA_ACTIVO = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

/**
 * Estado de la pantalla "Paseo Activo" del paseador. Todo se arma con el
 * detalle de la solicitud y con la ubicación que publica el servicio en primer
 * plano del paseo.
 */
data class PaseoActivoUiState(
    val solicitud: SolicitudPaseoResDto? = null,
    val mascotas: List<MascotaDetalleItem> = emptyList(),
    val inicioMillis: Long? = null,
    val transcurridoSegundos: Long = 0L,
    val duracionMinutos: Int? = null,
    val ubicacion: LatLng? = null,
    val precisionMetros: Double? = null,
    val gpsActivo: Boolean = false,
    val permisoUbicacion: Boolean = false,
    val cargando: Boolean = true,
    val error: String? = null,
    val finalizando: Boolean = false
)

/**
 * Pantalla de paseo en curso del paseador: tiempo transcurrido, estado del GPS,
 * mascotas a cargo y mapa con la ubicación actual como único punto (sin
 * recorrido ni velocidad). Permite finalizar el paseo.
 */
@Composable
fun PaseoActivoPaseadorScreen(
    state: PaseoActivoUiState,
    onBack: () -> Unit,
    onSolicitarPermiso: () -> Unit,
    onFinalizar: () -> Unit,
    onReintentar: () -> Unit
) {
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BarraSuperiorPaseoActivo(
                codigoReserva = state.solicitud?.codigoReserva,
                enVivo = !state.cargando && state.error == null,
                onBack = onBack
            )
        },
        bottomBar = {
            if (!state.cargando && state.error == null) {
                BarraFinalizarPaseo(
                    finalizando = state.finalizando,
                    onFinalizar = { mostrarConfirmacion = true }
                )
            }
        },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        when {
            state.cargando -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange, strokeWidth = 3.dp)
                }
            }

            state.error != null -> {
                ErrorPaseoActivo(
                    mensaje = state.error,
                    onReintentar = onReintentar,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TarjetaTiempo(state = state)
                    TarjetaGps(state = state, onSolicitarPermiso = onSolicitarPermiso)
                    TarjetaMascotas(mascotas = state.mascotas)
                    TarjetaMapa(
                        ubicacion = state.ubicacion,
                        permisoUbicacion = state.permisoUbicacion,
                        onSolicitarPermiso = onSolicitarPermiso
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = {
                Text(
                    text = "Finalizar paseo",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            },
            text = {
                Text(
                    text = "¿Confirmas que terminaste el paseo? El cliente verá el " +
                        "recorrido y el resumen al cerrarlo.",
                    fontSize = 14.sp,
                    color = TextoGrisActivo
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacion = false
                        onFinalizar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(text = "Finalizar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) {
                    Text(text = "Seguir paseando", color = TextoGrisActivo)
                }
            }
        )
    }
}

@Composable
private fun BarraSuperiorPaseoActivo(
    codigoReserva: String?,
    enVivo: Boolean,
    onBack: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = PrimaryDark
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Paseo Activo",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                val subtitulo = codigoReserva?.trim()?.takeIf { it.isNotBlank() }
                if (subtitulo != null) {
                    Text(
                        text = "Reserva $subtitulo",
                        fontSize = 12.sp,
                        color = TextoGrisActivo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (enVivo) {
                LiveBadge()
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(VerdeSuaveActivo)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(VerdeActivo)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "EN VIVO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeActivo,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun TarjetaTiempo(state: PaseoActivoUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFF8A50), PrimaryOrange)
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column {
            Text(
                text = "TIEMPO TRANSCURRIDO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatearTiempo(state.transcurridoSegundos),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))

            val partes = buildList {
                state.duracionMinutos?.takeIf { it > 0 }?.let { add("Duración: $it min") }
                horaLocalTexto(state.solicitud?.fechaHoraInicioReal)?.let { add("Inicio $it") }
            }
            Text(
                text = partes.joinToString("  ·  ").ifBlank { "Paseo en curso" },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun TarjetaGps(
    state: PaseoActivoUiState,
    onSolicitarPermiso: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.gpsActivo && state.ubicacion != null) VerdeActivo
                            else Color(0xFFF59E0B)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            !state.permisoUbicacion -> "Ubicación desactivada"
                            state.ubicacion != null -> "GPS activo"
                            else -> "Buscando señal GPS"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    Text(
                        text = when {
                            !state.permisoUbicacion ->
                                "Necesitamos tu ubicación para compartir el paseo."

                            state.ubicacion != null ->
                                "Compartiendo tu posición con el cliente."

                            else -> "Acércate a un lugar abierto y espera un momento."
                        },
                        fontSize = 12.sp,
                        color = TextoGrisActivo
                    )
                }

                val precision = state.precisionMetros
                if (state.ubicacion != null && precision != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "±${precision.toInt()} m",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextoGrisActivo
                        )
                    }
                }
            }

            if (!state.permisoUbicacion) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSolicitarPermiso,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Permitir ubicación", fontWeight = FontWeight.Bold)
                }
            }

            val lugar = listOfNotNull(
                state.solicitud?.distritoDescripcion?.trim()?.takeIf { it.isNotBlank() },
                state.solicitud?.direccionRecojo?.trim()?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")

            if (lugar.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NaranjaSuaveActivo)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = lugar,
                        fontSize = 12.sp,
                        color = PrimaryDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaMascotas(mascotas: List<MascotaDetalleItem>) {
    if (mascotas.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (mascotas.size == 1) {
                        "1 mascota a tu cuidado"
                    } else {
                        "${mascotas.size} mascotas a tu cuidado"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            mascotas.forEachIndexed { indice, mascota ->
                if (indice > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .height(1.dp)
                            .background(BordeGrisActivo)
                    )
                }
                FilaMascotaActiva(mascota = mascota)
            }
        }
    }
}

@Composable
private fun FilaMascotaActiva(mascota: MascotaDetalleItem) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarMascotaActiva(
                fotoUrl = mascota.fotoUrl,
                nombre = mascota.nombre,
                modifier = Modifier.size(46.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mascota.nombre,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detalle = listOfNotNull(
                    mascota.raza?.trim()?.takeIf { it.isNotBlank() },
                    mascota.tamano?.trim()?.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (detalle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = detalle,
                        fontSize = 12.sp,
                        color = TextoGrisActivo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            mascota.nivelEnergia
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { energia ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFEFF6FF))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = energia,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
        }

        mascota.precaucionesMedicas
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("ninguna", ignoreCase = true) }
            ?.let { precauciones ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmbarSuaveActivo)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = AmbarTextoActivo,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = precauciones,
                        fontSize = 12.sp,
                        color = AmbarTextoActivo,
                        lineHeight = 16.sp
                    )
                }
            }
    }
}

@Composable
private fun TarjetaMapa(
    ubicacion: LatLng?,
    permisoUbicacion: Boolean,
    onSolicitarPermiso: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ubicación en vivo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark,
                    modifier = Modifier.weight(1f)
                )
                LiveBadge()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
            ) {
                when {
                    !permisoUbicacion -> MapaSinPermiso(onSolicitarPermiso = onSolicitarPermiso)
                    ubicacion == null -> MapaEsperandoSenal()
                    else -> MapaUbicacionActual(ubicacion = ubicacion)
                }
            }
        }
    }
}

@Composable
private fun MapaUbicacionActual(ubicacion: LatLng) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    val icono = remember(context) { crearPuntoUbicacion(context) }

    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var marcador by remember { mutableStateOf<Marker?>(null) }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.getMapAsync { map ->
                if (googleMap == null) {
                    map.uiSettings.apply {
                        isZoomControlsEnabled = false
                        isMapToolbarEnabled = false
                        isMyLocationButtonEnabled = false
                        isCompassEnabled = false
                    }
                    googleMap = map
                }
            }
        }
    )

    LaunchedEffect(googleMap, ubicacion) {
        val map = googleMap ?: return@LaunchedEffect
        val actual = marcador
        if (actual == null) {
            marcador = map.addMarker(
                MarkerOptions()
                    .position(ubicacion)
                    .icon(icono)
                    .anchor(0.5f, 0.5f)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 17f))
        } else {
            actual.position = ubicacion
            map.animateCamera(CameraUpdateFactory.newLatLng(ubicacion))
        }
    }
}

@Composable
private fun MapaSinPermiso(onSolicitarPermiso: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Activa tu ubicación para ver el mapa",
            fontSize = 13.sp,
            color = TextoGrisActivo,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onSolicitarPermiso,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Permitir ubicación", color = PrimaryOrange, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MapaEsperandoSenal() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = PrimaryOrange,
            strokeWidth = 2.dp,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Obteniendo tu ubicación...",
            fontSize = 13.sp,
            color = TextoGrisActivo
        )
    }
}

@Composable
private fun BarraFinalizarPaseo(
    finalizando: Boolean,
    onFinalizar: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 12.dp) {
        Button(
            onClick = onFinalizar,
            enabled = !finalizando,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryOrange,
                disabledContainerColor = PrimaryOrange.copy(alpha = 0.55f)
            )
        ) {
            if (finalizando) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Finalizando...", fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Finalizar paseo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ErrorPaseoActivo(
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = mensaje,
            fontSize = 14.sp,
            color = TextoGrisActivo,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onReintentar,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Reintentar", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AvatarMascotaActiva(
    fotoUrl: String?,
    nombre: String,
    modifier: Modifier = Modifier
) {
    if (fotoUrl != null) {
        AsyncImage(
            model = fotoUrl,
            contentDescription = "Foto de $nombre",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(PrimaryOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )
        }
    }
}

/** Punto naranja con borde blanco para la ubicación actual del paseador. */
private fun crearPuntoUbicacion(context: android.content.Context): BitmapDescriptor {
    val densidad = context.resources.displayMetrics.density
    val tamano = (30 * densidad).toInt().coerceAtLeast(48)
    val bitmap = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centro = tamano / 2f

    val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawCircle(centro, centro, centro - 1f, borde)

    val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(255, 87, 34)
    }
    canvas.drawCircle(centro, centro, centro - (5 * densidad), relleno)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun formatearTiempo(segundosTotales: Long): String {
    val segundos = segundosTotales.coerceAtLeast(0)
    val horas = segundos / 3600
    val minutos = (segundos % 3600) / 60
    val resto = segundos % 60
    return if (horas > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", horas, minutos, resto)
    } else {
        String.format(Locale.US, "%02d:%02d", minutos, resto)
    }
}

/** El backend guarda `fecha_hora_inicio_real` en UTC; se muestra en hora de Lima. */
private fun horaLocalTexto(isoUtc: String?): String? {
    val texto = isoUtc?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        val limpio = texto.removeSuffix("Z").take(19)
        LocalDateTime.parse(limpio)
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(ZONA_LIMA)
            .format(FORMATO_HORA_ACTIVO)
    }.getOrNull()
}
