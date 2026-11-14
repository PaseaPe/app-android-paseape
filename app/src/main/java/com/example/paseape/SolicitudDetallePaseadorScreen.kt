package com.example.paseape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark
import com.example.paseape.ui.theme.PrimaryOrange
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TextoGrisDetalle = Color(0xFF64748B)
private val BordeGrisDetalle = Color(0xFFE2E8F0)
private val RojoDetalle = Color(0xFFD32F2F)
private val VerdeDetalle = Color(0xFF2E7D32)
private val NaranjaSuave = Color(0xFFFFF3E0)
private val NaranjaTexto = Color(0xFFE65100)
private val VerdeSuave = Color(0xFFE8F5E9)
private val AzulSuave = Color(0xFFE3F2FD)
private val AzulTexto = Color(0xFF1565C0)
private val RojoSuave = Color(0xFFFDECEA)

private const val ESTADO_PENDIENTE = 1
private const val ESTADO_ACEPTADA = 2
private const val ESTADO_BUSCANDO = 8
private const val MOTIVO_CANCELACION_MINIMO = 10

private val FORMATO_HORA_DETALLE = DateTimeFormatter.ofPattern("HH:mm")

/** Mascota enriquecida con el detalle de `GET /mascotas/mascota/{uuid}`. */
data class MascotaDetalleItem(
    val uuid: String?,
    val nombre: String,
    val raza: String?,
    val tamano: String?,
    val precaucionesMedicas: String?,
    val fotoUrl: String?,
    val edadAnos: Int? = null,
    val edadMeses: Int? = null,
    val pesoKg: Double? = null,
    val nivelEnergia: String? = null,
    val sociablePerros: Boolean = false,
    val sociablePersonas: Boolean = false,
    val esterilizado: Boolean = false
)

sealed interface SolicitudDetalleUiState {

    data object Loading : SolicitudDetalleUiState

    data class Error(val message: String) : SolicitudDetalleUiState

    data class Success(
        val solicitud: SolicitudPaseoResDto,
        val mascotas: List<MascotaDetalleItem>
    ) : SolicitudDetalleUiState
}

/**
 * Detalle de una solicitud de paseo del lado del paseador.
 *
 * - BUSCANDO (inmediata): permite descartar o tomar la solicitud.
 * - PENDIENTE: permite aceptar o rechazar (con motivo opcional).
 * - ACEPTADA: muestra "Iniciar paseo" y "Cancelar".
 *
 * Los datos se arman solo con rutas del API: detalle de la solicitud (o la
 * lista de disponibles si aún está en BUSCANDO), cliente para el teléfono y
 * ficha de cada mascota para foto y precauciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudDetallePaseadorScreen(
    uiState: SolicitudDetalleUiState,
    accionEnCurso: AccionPaseo?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAceptar: () -> Unit,
    onRechazar: (String) -> Unit,
    onIniciar: () -> Unit,
    onCancelar: (String) -> Unit,
    onTomar: () -> Unit,
    onDescartar: () -> Unit,
    onAbrirMapa: (String) -> Unit
) {
    var mostrarRechazo by remember { mutableStateOf(false) }
    var mostrarCancelar by remember { mutableStateOf(false) }

    val solicitud = (uiState as? SolicitudDetalleUiState.Success)?.solicitud
    val estado = solicitud?.estadoId
    val esInmediata = estado == ESTADO_BUSCANDO
    val ahora = recordarAhora(esInmediata)
    val expirada = solicitud != null && solicitudExpirada(solicitud, ahora)
    val muestraAcciones = estado == ESTADO_PENDIENTE ||
        estado == ESTADO_ACEPTADA ||
        estado == ESTADO_BUSCANDO

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Detalle de solicitud",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                        solicitud?.codigoReserva?.trim()?.takeIf { it.isNotBlank() }?.let { codigo ->
                            Text(
                                text = codigo,
                                fontSize = 11.sp,
                                color = TextoGrisDetalle
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (solicitud != null && muestraAcciones) {
                BarraAccionesDetalle(
                    estado = estado,
                    expirada = expirada,
                    accionEnCurso = accionEnCurso,
                    onAceptar = onAceptar,
                    onRechazar = { mostrarRechazo = true },
                    onIniciar = onIniciar,
                    onCancelar = { mostrarCancelar = true },
                    onTomar = onTomar,
                    onDescartar = onDescartar
                )
            }
        },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        when (uiState) {
            SolicitudDetalleUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryOrange)
            }

            is SolicitudDetalleUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.message,
                    fontSize = 14.sp,
                    color = TextoGrisDetalle,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Reintentar", fontWeight = FontWeight.Bold)
                }
            }

            is SolicitudDetalleUiState.Success -> ContenidoDetalle(
                modifier = Modifier.padding(paddingValues),
                solicitud = uiState.solicitud,
                mascotas = uiState.mascotas,
                expirada = expirada,
                onAbrirMapa = onAbrirMapa
            )
        }
    }

    if (mostrarRechazo) {
        MotivoDialog(
            titulo = "Rechazar solicitud",
            descripcion = "Puedes indicar un motivo (opcional). El cliente verá la solicitud rechazada.",
            textoConfirmar = "Rechazar",
            colorConfirmar = RojoDetalle,
            requiereMinimo = false,
            onDismiss = { mostrarRechazo = false },
            onConfirmar = { motivo ->
                mostrarRechazo = false
                onRechazar(motivo)
            }
        )
    }

    if (mostrarCancelar) {
        MotivoDialog(
            titulo = "Cancelar paseo",
            descripcion = "Indica el motivo de la cancelación (mínimo $MOTIVO_CANCELACION_MINIMO caracteres). Se notificará al cliente.",
            textoConfirmar = "Cancelar paseo",
            colorConfirmar = RojoDetalle,
            requiereMinimo = true,
            onDismiss = { mostrarCancelar = false },
            onConfirmar = { motivo ->
                mostrarCancelar = false
                onCancelar(motivo)
            }
        )
    }
}

@Composable
private fun ContenidoDetalle(
    modifier: Modifier,
    solicitud: SolicitudPaseoResDto,
    mascotas: List<MascotaDetalleItem>,
    expirada: Boolean,
    onAbrirMapa: (String) -> Unit
) {
    val direccion = listOfNotNull(
        solicitud.direccionRecojo?.trim()?.takeIf { it.isNotBlank() },
        solicitud.distritoDescripcion?.trim()?.takeIf { it.isNotBlank() }
    ).joinToString(", ")
    val referencia = solicitud.referenciaDireccion?.trim()?.takeIf { it.isNotBlank() }
    val esInmediata = solicitud.estadoId == ESTADO_BUSCANDO

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeroSolicitudCard(solicitud = solicitud, mascotas = mascotas)
        }

        item {
            ClienteCard(
                nombre = solicitud.clienteNombreCompleto?.trim()?.takeIf { it.isNotBlank() },
                telefono = solicitud.clienteTelefono?.trim()?.takeIf { it.isNotBlank() },
                direccion = direccion,
                referencia = referencia,
                onAbrirMapa = onAbrirMapa
            )
        }

        item {
            HorarioCard(solicitud = solicitud, esInmediata = esInmediata)
        }

        if (mascotas.isNotEmpty()) {
            item {
                SeccionTitulo(
                    icon = Icons.Default.Pets,
                    texto = if (mascotas.size == 1) "Mascota" else "Mascotas (${mascotas.size})"
                )
            }
            items(mascotas.size) { index ->
                MascotaDetalleCard(mascota = mascotas[index])
            }
        }

        if (expirada) {
            item {
                Surface(
                    color = RojoSuave,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "La solicitud expiró. Ya no es posible tomarla.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RojoDetalle,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

/** Tarjeta principal: foto de la mascota, estado y monto. */
@Composable
private fun HeroSolicitudCard(
    solicitud: SolicitudPaseoResDto,
    mascotas: List<MascotaDetalleItem>
) {
    val monto = formatearMontoDetalle(solicitud.montoTotal)
    val primera = mascotas.firstOrNull()
    val nombre = when {
        mascotas.isEmpty() -> mascotaFallback(solicitud)
        mascotas.size == 1 -> primera?.nombre ?: "Mascota"
        else -> "${primera?.nombre ?: "Mascota"} y ${mascotas.size - 1} más"
    }
    val subtitulo = if (mascotas.size == 1 && primera != null) {
        listOfNotNull(primera.raza, primera.tamano).joinToString(" · ")
    } else {
        "${mascotas.size} mascotas"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarMascota(
                    fotoUrl = primera?.fotoUrl,
                    nombre = nombre,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitulo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitulo,
                            fontSize = 12.sp,
                            color = TextoGrisDetalle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    EstadoBadgeDetalle(
                        estado = solicitud.estadoId,
                        descripcion = solicitud.estadoDescripcion
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total",
                        fontSize = 11.sp,
                        color = TextoGrisDetalle
                    )
                    Text(
                        text = monto ?: "—",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                }
            }

            val chips = listOfNotNull(
                solicitud.tipoDuracionPaseoDescripcion?.trim()?.takeIf { it.isNotBlank() },
                solicitud.tipoMetodoPagoDescripcion?.trim()?.takeIf { it.isNotBlank() },
                solicitud.tipoModalidadPaseoDescripcion?.trim()?.takeIf { it.isNotBlank() }
            )
            if (chips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { chip ->
                        InfoChip(texto = chip)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClienteCard(
    nombre: String?,
    telefono: String?,
    direccion: String,
    referencia: String?,
    onAbrirMapa: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inicialesCliente(nombre),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Cliente",
                        fontSize = 11.sp,
                        color = TextoGrisDetalle
                    )
                    Text(
                        text = nombre ?: "Cliente PaseaPe",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    if (telefono != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = TextoGrisDetalle,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = telefono,
                                fontSize = 13.sp,
                                color = TextoGrisDetalle
                            )
                        }
                    }
                }
            }

            if (direccion.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BordeGrisDetalle)
                Spacer(modifier = Modifier.height(14.dp))
                SeccionTitulo(icon = Icons.Default.Place, texto = "Dirección de recojo")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = direccion,
                    fontSize = 14.sp,
                    color = PrimaryDark,
                    lineHeight = 20.sp
                )
                if (referencia != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Referencia: $referencia",
                        fontSize = 12.sp,
                        color = TextoGrisDetalle
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onAbrirMapa(direccion) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PrimaryOrange.copy(alpha = 0.06f),
                        contentColor = PrimaryOrange
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Abrir en Google Maps",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HorarioCard(
    solicitud: SolicitudPaseoResDto,
    esInmediata: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SeccionTitulo(icon = Icons.Default.Schedule, texto = "Horario del paseo")
            Spacer(modifier = Modifier.height(8.dp))

            if (!esInmediata) {
                val fecha = formatearFechaPaseo(solicitud.fechaProgramada)
                val bloque = formatearBloqueHorario(solicitud.bloqueHorarioCodigo)
                Text(
                    text = listOfNotNull(fecha, bloque).joinToString(" · ")
                        .ifBlank { "Por confirmar" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            } else {
                val horaSolicitud = fechaHoraUtcALocal(solicitud.creadoEl)
                    ?.format(FORMATO_HORA_DETALLE)
                Text(
                    text = "Ahora${horaSolicitud?.let { " · solicitado a las $it" }.orEmpty()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                ContadorVencimiento(horaVencimiento = solicitud.horaVencimiento)
            }

            solicitud.tipoMetodoPagoDescripcion?.trim()?.takeIf { it.isNotBlank() }?.let { metodo ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = TextoGrisDetalle,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Pago: $metodo",
                        fontSize = 12.sp,
                        color = TextoGrisDetalle
                    )
                }
            }
        }
    }
}

@Composable
private fun MascotaDetalleCard(mascota: MascotaDetalleItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarMascota(
                    fotoUrl = mascota.fotoUrl,
                    nombre = mascota.nombre,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mascota.nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitulo = listOfNotNull(mascota.raza, mascota.tamano).joinToString(" · ")
                    if (subtitulo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitulo,
                            fontSize = 12.sp,
                            color = TextoGrisDetalle
                        )
                    }
                }
            }

            val datos = datosMascota(mascota)
            if (datos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    datos.forEach { InfoChip(texto = it) }
                }
            }

            mascota.precaucionesMedicas
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("ninguna", ignoreCase = true) }
                ?.let { precauciones ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Precauciones médicas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8D6E00)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = precauciones,
                                fontSize = 13.sp,
                                color = PrimaryDark,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun BarraAccionesDetalle(
    estado: Int?,
    expirada: Boolean,
    accionEnCurso: AccionPaseo?,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onIniciar: () -> Unit,
    onCancelar: () -> Unit,
    onTomar: () -> Unit,
    onDescartar: () -> Unit
) {
    val habilitado = accionEnCurso == null && !expirada

    Surface(
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (expirada) {
                Text(
                    text = "La solicitud expiró. Ya no es posible tomarla.",
                    fontSize = 12.sp,
                    color = RojoDetalle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            when (estado) {
                ESTADO_PENDIENTE -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonSecundarioDetalle(
                        texto = "Rechazar",
                        cargando = accionEnCurso == AccionPaseo.RECHAZAR,
                        habilitado = habilitado,
                        onClick = onRechazar,
                        modifier = Modifier.weight(1f)
                    )
                    BotonPrincipalDetalle(
                        texto = "Aceptar",
                        cargando = accionEnCurso == AccionPaseo.ACEPTAR,
                        habilitado = habilitado,
                        onClick = onAceptar,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                ESTADO_ACEPTADA -> Column {
                    BotonPrincipalDetalle(
                        texto = "Iniciar paseo",
                        cargando = accionEnCurso == AccionPaseo.INICIAR,
                        habilitado = habilitado,
                        onClick = onIniciar,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BotonSecundarioDetalle(
                        texto = "Cancelar paseo",
                        cargando = accionEnCurso == AccionPaseo.CANCELAR,
                        habilitado = habilitado,
                        onClick = onCancelar,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ESTADO_BUSCANDO -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonSecundarioDetalle(
                        texto = "Descartar",
                        cargando = accionEnCurso == AccionPaseo.DESCARTAR,
                        habilitado = habilitado,
                        onClick = onDescartar,
                        modifier = Modifier.weight(1f)
                    )
                    BotonPrincipalDetalle(
                        texto = "Tomar solicitud",
                        cargando = accionEnCurso == AccionPaseo.TOMAR,
                        habilitado = habilitado,
                        onClick = onTomar,
                        modifier = Modifier.weight(1.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonPrincipalDetalle(
    texto: String,
    cargando: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VerdeDetalle,
            contentColor = Color.White,
            disabledContainerColor = VerdeDetalle.copy(alpha = 0.45f),
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(texto, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BotonSecundarioDetalle(
    texto: String,
    cargando: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, RojoDetalle.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = RojoDetalle.copy(alpha = 0.06f),
            contentColor = RojoDetalle,
            disabledContainerColor = RojoDetalle.copy(alpha = 0.03f),
            disabledContentColor = RojoDetalle.copy(alpha = 0.45f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = RojoDetalle,
                strokeWidth = 2.dp
            )
        } else {
            Text(texto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MotivoDialog(
    titulo: String,
    descripcion: String,
    textoConfirmar: String,
    colorConfirmar: Color,
    requiereMinimo: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    val motivoLimpio = motivo.trim()
    val puedeConfirmar = !requiereMinimo || motivoLimpio.length >= MOTIVO_CANCELACION_MINIMO

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(descripcion, fontSize = 13.sp, color = TextoGrisDetalle)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    placeholder = { Text("Motivo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = BordeGrisDetalle
                    ),
                    supportingText = if (requiereMinimo) {
                        {
                            Text(
                                text = "${motivoLimpio.length}/$MOTIVO_CANCELACION_MINIMO caracteres",
                                fontSize = 11.sp,
                                color = if (puedeConfirmar) VerdeDetalle else TextoGrisDetalle
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(motivoLimpio) },
                enabled = puedeConfirmar
            ) {
                Text(
                    text = textoConfirmar,
                    fontWeight = FontWeight.Bold,
                    color = if (puedeConfirmar) colorConfirmar else TextoGrisDetalle
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Volver", color = TextoGrisDetalle)
            }
        },
        containerColor = Color.White
    )
}

@Composable
private fun SeccionTitulo(icon: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryOrange,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextoGrisDetalle
        )
    }
}

@Composable
private fun InfoChip(texto: String) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AvatarMascota(
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )
        }
    }
}

@Composable
private fun EstadoBadgeDetalle(estado: Int?, descripcion: String?) {
    val (fondo, texto, etiqueta) = when (estado) {
        ESTADO_PENDIENTE -> Triple(NaranjaSuave, NaranjaTexto, "Pendiente")
        ESTADO_ACEPTADA -> Triple(VerdeSuave, VerdeDetalle, "Aceptada")
        ESTADO_BUSCANDO -> Triple(AzulSuave, AzulTexto, "Buscando paseador")
        else -> Triple(
            Color(0xFFF1F5F9),
            TextoGrisDetalle,
            descripcion?.trim()?.takeIf { it.isNotBlank() } ?: "—"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = etiqueta,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = texto
        )
    }
}

private fun datosMascota(mascota: MascotaDetalleItem): List<String> {
    val datos = mutableListOf<String>()

    val edad = when {
        mascota.edadAnos != null && mascota.edadAnos > 0 && mascota.edadMeses != null && mascota.edadMeses > 0 ->
            "${mascota.edadAnos}a ${mascota.edadMeses}m"
        mascota.edadAnos != null && mascota.edadAnos > 0 -> "${mascota.edadAnos} años"
        mascota.edadMeses != null && mascota.edadMeses > 0 -> "${mascota.edadMeses} meses"
        else -> null
    }
    edad?.let(datos::add)

    mascota.pesoKg?.takeIf { it > 0 }?.let { peso ->
        datos.add(String.format(Locale.US, "%.1f kg", peso))
    }
    mascota.nivelEnergia?.trim()?.takeIf { it.isNotBlank() }?.let { datos.add("Energía $it") }
    if (mascota.sociablePerros) datos.add("Sociable con perros")
    if (mascota.sociablePersonas) datos.add("Sociable con personas")
    if (mascota.esterilizado) datos.add("Esterilizado")

    return datos
}

private fun inicialesCliente(nombre: String?): String {
    val partes = nombre
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.filter { it.isNotBlank() }
        .orEmpty()
    return when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes.first().first().uppercaseChar().toString()
        else -> "${partes.first().first().uppercaseChar()}${partes[1].first().uppercaseChar()}"
    }
}

private fun mascotaFallback(solicitud: SolicitudPaseoResDto): String =
    solicitud.mascotas?.firstOrNull()?.nombre?.trim()?.takeIf { it.isNotBlank() } ?: "Mascota"

private fun formatearMontoDetalle(monto: Double?): String? {
    if (monto == null) return null
    return "S/ " + String.format(Locale.US, "%.2f", monto)
}
