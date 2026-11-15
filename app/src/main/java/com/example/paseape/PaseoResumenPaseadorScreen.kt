package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.paseape.data.model.SolicitudMascotaResDto
import com.example.paseape.data.model.SolicitudPaseoRecorridoResDto
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark
import com.example.paseape.ui.theme.PrimaryOrange
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val GrisResumen = Color(0xFF64748B)
private val VerdeResumen = Color(0xFF16A34A)
private val VerdeSuaveResumen = Color(0xFFE7F8ED)
private val VerdeTextoResumen = Color(0xFF166534)
private val NaranjaSuaveResumen = Color(0xFFFFF3E0)
private val EstrellaResumen = Color(0xFFF59E0B)

private val ZONA_LIMA_RESUMEN = ZoneId.of("America/Lima")
private val FORMATO_HORA_RESUMEN = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

/**
 * Estado de la pantalla "Servicio Concluido". Solo contiene datos que devuelve
 * el backend: el detalle de la solicitud y el recorrido consolidado.
 */
data class PaseoResumenUiState(
    val solicitud: SolicitudPaseoResDto? = null,
    val recorrido: SolicitudPaseoRecorridoResDto? = null,
    val puntosRuta: List<LatLng> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

/**
 * Resumen post-paseo: confirmación, liquidación, métricas reales, mapa con la
 * ruta consolidada, entrega de mascotas y el promedio del paseador.
 */
@Composable
fun PaseoResumenPaseadorScreen(
    state: PaseoResumenUiState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit
) {
    Scaffold(
        topBar = { BarraSuperiorResumen() },
        bottomBar = {
            if (!state.cargando && state.error == null) {
                BarraVolverBandeja(onVolver = onVolver)
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
                ErrorResumen(
                    mensaje = state.error,
                    onReintentar = onReintentar,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                val solicitud = state.solicitud
                val duracionSegundos = duracionSegundosResumen(state)
                val distancia = state.recorrido?.distanciaMetros

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BannerExito(
                        direccion = solicitud?.direccionRecojo,
                        distrito = solicitud?.distritoDescripcion
                    )
                    TarjetaResumen(
                        monto = solicitud?.montoTotal,
                        distanciaTexto = formatearDistancia(distancia),
                        duracionTexto = formatearDuracion(duracionSegundos),
                        mascotasTexto = cantidadMascotas(solicitud),
                        velocidadTexto = formatearVelocidad(state.recorrido?.velocidadPromedioKmh)
                    )
                    TarjetaRecorrido(
                        puntos = state.puntosRuta,
                        totalPuntos = state.recorrido?.coordenadasTotalPuntos
                    )
                    TarjetaEntrega(
                        direccion = solicitud?.direccionRecojo,
                        distrito = solicitud?.distritoDescripcion,
                        horaFin = horaLocalTexto(solicitud?.fechaHoraFinReal),
                        mascotas = solicitud?.mascotas.orEmpty()
                    )
                    TarjetaRating(calificacion = solicitud?.paseadorCalificacionEfectiva)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun BarraSuperiorResumen() {
    Surface(color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Servicio Concluido",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(VerdeResumen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Paseo concluido",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun BannerExito(direccion: String?, distrito: String?) {
    val lugar = listOfNotNull(
        direccion?.trim()?.takeIf { it.isNotBlank() },
        distrito?.trim()?.takeIf { it.isNotBlank() }
    ).joinToString(", ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VerdeSuaveResumen)
            .border(1.dp, VerdeResumen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = VerdeResumen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "¡Paseo completado con éxito!",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeTextoResumen
            )
            if (lugar.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Entrega de mascotas en $lugar",
                    fontSize = 12.sp,
                    color = VerdeTextoResumen.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TarjetaResumen(
    monto: Double?,
    distanciaTexto: String,
    duracionTexto: String,
    mascotasTexto: String,
    velocidadTexto: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Liquidación del servicio",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = monto?.let { String.format(Locale.US, "S/ %.2f PEN", it) } ?: "—",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetricaResumen(
                    valor = distanciaTexto,
                    etiqueta = "Recorrido",
                    modifier = Modifier.weight(1f)
                )
                MetricaResumen(
                    valor = duracionTexto,
                    etiqueta = "Duración",
                    modifier = Modifier.weight(1f)
                )
                MetricaResumen(
                    valor = mascotasTexto,
                    etiqueta = "Mascotas",
                    modifier = Modifier.weight(1f)
                )
                MetricaResumen(
                    valor = velocidadTexto,
                    etiqueta = "Vel. prom.",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricaResumen(
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = valor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryOrange,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = etiqueta,
            fontSize = 10.sp,
            color = GrisResumen,
            maxLines = 1
        )
    }
}

@Composable
private fun TarjetaRecorrido(
    puntos: List<LatLng>,
    totalPuntos: Int?
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
                    text = "Recorrido del paseo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark,
                    modifier = Modifier.weight(1f)
                )
                val puntosGps = totalPuntos ?: puntos.size
                if (puntosGps > 0) {
                    Text(
                        text = "$puntosGps puntos GPS",
                        fontSize = 11.sp,
                        color = GrisResumen
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp)
                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
            ) {
                if (puntos.isEmpty()) {
                    MapaSinRecorrido()
                } else {
                    MapaRecorrido(puntos = puntos)
                }
            }
        }
    }
}

@Composable
private fun MapaRecorrido(puntos: List<LatLng>) {
    val mapView = rememberMapViewWithLifecycle()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.getMapAsync { map ->
                if (googleMap != null) return@getMapAsync

                map.uiSettings.apply {
                    isZoomControlsEnabled = false
                    isMapToolbarEnabled = false
                    isMyLocationButtonEnabled = false
                    isCompassEnabled = false
                }

                map.addPolyline(
                    PolylineOptions()
                        .addAll(puntos)
                        .width(12f)
                        .color(PrimaryOrange.toArgb())
                        .geodesic(true)
                        .startCap(RoundCap())
                        .endCap(RoundCap())
                        .jointType(JointType.ROUND)
                )

                map.addMarker(
                    MarkerOptions()
                        .position(puntos.first())
                        .title("Inicio del paseo")
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN
                            )
                        )
                )
                map.addMarker(
                    MarkerOptions()
                        .position(puntos.last())
                        .title("Fin del paseo")
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            )
                        )
                )

                map.setOnMapLoadedCallback {
                    if (puntos.size == 1) {
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(puntos.first(), 16f)
                        )
                    } else {
                        val bounds = LatLngBounds.builder().apply {
                            puntos.forEach { include(it) }
                        }.build()
                        runCatching {
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds, 120)
                            )
                        }
                    }
                }

                googleMap = map
            }
        }
    )
}

@Composable
private fun MapaSinRecorrido() {
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
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sin datos GPS de la ruta",
            fontSize = 13.sp,
            color = GrisResumen,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TarjetaEntrega(
    direccion: String?,
    distrito: String?,
    horaFin: String?,
    mascotas: List<SolicitudMascotaResDto>
) {
    val lugar = listOfNotNull(
        direccion?.trim()?.takeIf { it.isNotBlank() },
        distrito?.trim()?.takeIf { it.isNotBlank() }
    ).joinToString(", ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Entrega",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )

            if (lugar.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lugar,
                        fontSize = 13.sp,
                        color = PrimaryDark,
                        lineHeight = 18.sp
                    )
                }
            }

            if (horaFin != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NaranjaSuaveResumen)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Fin del paseo: $horaFin",
                        fontSize = 12.sp,
                        color = PrimaryDark
                    )
                }
            }

            if (mascotas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                mascotas.forEachIndexed { indice, mascota ->
                    if (indice > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    FilaMascotaEntregada(mascota = mascota)
                }
            }
        }
    }
}

@Composable
private fun FilaMascotaEntregada(mascota: SolicitudMascotaResDto) {
    val nombre = mascota.nombre?.trim()?.takeIf { it.isNotBlank() } ?: "Mascota"
    val detalle = listOfNotNull(
        mascota.razaEfectiva?.trim()?.takeIf { it.isNotBlank() },
        mascota.tamanoEfectivo?.trim()?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Row(verticalAlignment = Alignment.CenterVertically) {
        AvatarMascotaResumen(
            fotoUrl = mascota.fotoUrlEfectiva,
            nombre = nombre,
            modifier = Modifier.size(42.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombre,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (detalle.isNotBlank()) {
                Text(
                    text = detalle,
                    fontSize = 12.sp,
                    color = GrisResumen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AvatarMascotaResumen(
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )
        }
    }
}

@Composable
private fun TarjetaRating(calificacion: Double?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = EstrellaResumen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Tu rating",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = calificacion?.let {
                        String.format(Locale.US, "%.1f · promedio como paseador", it)
                    } ?: "Aún sin calificaciones",
                    fontSize = 12.sp,
                    color = GrisResumen
                )
            }
        }
    }
}

@Composable
private fun BarraVolverBandeja(onVolver: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 12.dp) {
        Button(
            onClick = onVolver,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
        ) {
            Text(
                text = "Volver a la bandeja",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ErrorResumen(
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
            color = GrisResumen,
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

private fun cantidadMascotas(solicitud: SolicitudPaseoResDto?): String {
    val cantidad = solicitud?.mascotas?.size ?: 0
    return if (cantidad > 0) cantidad.toString() else "—"
}

private fun formatearDistancia(metros: Double?): String {
    if (metros == null || metros <= 0.0) return "—"
    return if (metros >= 1000.0) {
        String.format(Locale.US, "%.2f km", metros / 1000.0)
    } else {
        String.format(Locale.US, "%.0f m", metros)
    }
}

private fun formatearDuracion(segundos: Long?): String {
    if (segundos == null || segundos <= 0) return "—"
    val horas = segundos / 3600
    val minutos = (segundos % 3600) / 60
    return if (horas > 0) {
        String.format(Locale.US, "%dh %02dm", horas, minutos)
    } else {
        "$minutos min"
    }
}

private fun formatearVelocidad(kmh: Double?): String {
    if (kmh == null || kmh <= 0.0) return "—"
    return String.format(Locale.US, "%.1f km/h", kmh)
}

/** Duración real del recorrido; si no hay traza, se calcula con las horas del paseo. */
private fun duracionSegundosResumen(state: PaseoResumenUiState): Long? {
    val delRecorrido = state.recorrido?.duracionSegundos?.takeIf { it > 0 }?.toLong()
    if (delRecorrido != null) return delRecorrido

    val inicio = parseUtcMillis(state.solicitud?.fechaHoraInicioReal) ?: return null
    val fin = parseUtcMillis(state.solicitud?.fechaHoraFinReal) ?: return null
    return ((fin - inicio) / 1000).takeIf { it > 0 }
}

private fun parseUtcMillis(isoUtc: String?): Long? {
    val texto = isoUtc?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        LocalDateTime.parse(texto.removeSuffix("Z").take(19))
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    }.getOrNull()
}

/** El backend guarda las horas en UTC; se muestran en hora de Lima. */
private fun horaLocalTexto(isoUtc: String?): String? {
    val texto = isoUtc?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        LocalDateTime.parse(texto.removeSuffix("Z").take(19))
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(ZONA_LIMA_RESUMEN)
            .format(FORMATO_HORA_RESUMEN)
    }.getOrNull()
}
