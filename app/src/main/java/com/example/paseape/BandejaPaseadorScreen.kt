package com.example.paseape

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.SessionManager
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark
import com.example.paseape.ui.theme.PrimaryOrange
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

private val TextoGris = Color(0xFF64748B)
private val BordeGris = Color(0xFFE2E8F0)
private val RojoEliminar = Color(0xFFD32F2F)
private val VerdeAceptar = Color(0xFF2E7D32)
private val FondoPendiente = Color(0xFFFFF3E0)
private val TextoPendiente = Color(0xFFE65100)
private val FondoAceptada = Color(0xFFE8F5E9)
private val FondoEnCurso = Color(0xFFE3F2FD)
private val TextoEnCurso = Color(0xFF1565C0)
private val FondoRechazada = Color(0xFFFDECEA)
private val FondoFinalizada = Color(0xFFF1F5F9)

/**
 * Bandeja "Gestión de Paseos" del paseador: 4 pestañas con contadores sobre los
 * endpoints existentes de `paseador/solicitudes`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandejaPaseadorScreen(
    token: String,
    viewModel: BandejaPaseadorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val accionesEnCurso by viewModel.accionesEnCurso.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Insignia numérica de notificaciones no leídas (misma fuente que el dashboard del cliente)
    val unreadCount by NotificacionesViewModel.unreadCount.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            NotificacionesViewModel.sincronizarContadorRemoto(
                context = context,
                authHeader = SessionManager.getAuthHeader(context),
                userUuid = SessionManager.getUserUuid(context)
            )
            delay(15_000L)
        }
    }

    var tabActivo by remember { mutableStateOf(TabPaseos.INMEDIATOS) }
    var solicitudDescartar by remember { mutableStateOf<SolicitudPaseoResDto?>(null) }
    var solicitudRechazar by remember { mutableStateOf<SolicitudPaseoResDto?>(null) }
    var motivoRechazo by remember { mutableStateOf("") }

    LaunchedEffect(token) {
        viewModel.cargar(token)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val reintentar = message.retry
            val resultado = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = if (reintentar != null) "Reintentar" else null,
                withDismissAction = reintentar == null
            )
            if (reintentar != null && resultado == SnackbarResult.ActionPerformed) {
                reintentar()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gestión de Paseos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refrescarTodo() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar paseos",
                            tint = PrimaryDark
                        )
                    }
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(context, NotificacionesActivity::class.java)
                            )
                        }
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = PrimaryOrange,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = PrimaryDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundOffWhite
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val tabs = (uiState as? PaseosUiState.Success)?.tabs.orEmpty()
            PaseosTabsRow(
                tabActivo = tabActivo,
                tabs = tabs,
                onTabSelected = { tab -> tabActivo = tab }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is PaseosUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryOrange,
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    is PaseosUiState.Error -> {
                        PaseosErrorContent(
                            message = state.message,
                            onRetry = { viewModel.cargar(token, force = true) }
                        )
                    }

                    is PaseosUiState.Success -> {
                        PaseosTabContent(
                            tab = tabActivo,
                            data = state.tabs[tabActivo],
                            acciones = accionesEnCurso,
                            onReintentar = { viewModel.refrescarTodo() },
                            onCargarMas = { viewModel.cargarMas(tabActivo) },
                            onAceptar = { solicitud ->
                                val uuid = solicitud.uuid
                                if (uuid != null) {
                                    when (tabActivo) {
                                        TabPaseos.INMEDIATOS -> viewModel.tomarInmediata(uuid)
                                        TabPaseos.MIS_PASEOS -> viewModel.aceptarProgramada(uuid)
                                        TabPaseos.EN_CURSO,
                                        TabPaseos.HISTORIAL -> Unit
                                    }
                                }
                            },
                            onSecundario = { solicitud ->
                                when (tabActivo) {
                                    TabPaseos.INMEDIATOS -> {
                                        solicitudDescartar = solicitud
                                    }

                                    TabPaseos.MIS_PASEOS -> {
                                        motivoRechazo = ""
                                        solicitudRechazar = solicitud
                                    }

                                    TabPaseos.EN_CURSO,
                                    TabPaseos.HISTORIAL -> Unit
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    solicitudDescartar?.let { solicitud ->
        AlertDialog(
            onDismissRequest = { solicitudDescartar = null },
            title = {
                Text(
                    text = "Descartar solicitud",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            },
            text = {
                Text(
                    text = "Esta solicitud dejará de mostrarse para ti. " +
                        "Otros paseadores podrán seguir viéndola.",
                    fontSize = 14.sp,
                    color = TextoGris
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        solicitud.uuid?.let { viewModel.descartarInmediata(it) }
                        solicitudDescartar = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = PrimaryOrange
                    )
                ) {
                    Text("Descartar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { solicitudDescartar = null }) {
                    Text("Cancelar", color = PrimaryDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    solicitudRechazar?.let { solicitud ->
        AlertDialog(
            onDismissRequest = { solicitudRechazar = null },
            title = {
                Text(
                    text = "Rechazar paseo",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Indica el motivo del rechazo. Es opcional y el cliente " +
                            "podrá verlo.",
                        fontSize = 14.sp,
                        color = TextoGris
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = motivoRechazo,
                        onValueChange = { nuevo ->
                            if (nuevo.length <= MAX_MOTIVO_RECHAZO) {
                                motivoRechazo = nuevo
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Motivo del rechazo (opcional)") },
                        minLines = 2,
                        maxLines = 4,
                        supportingText = {
                            Text(
                                text = "${motivoRechazo.length}/$MAX_MOTIVO_RECHAZO",
                                fontSize = 11.sp,
                                color = TextoGris
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryOrange,
                            focusedLabelColor = PrimaryOrange,
                            cursorColor = PrimaryOrange
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        solicitud.uuid?.let { viewModel.rechazarProgramada(it, motivoRechazo) }
                        solicitudRechazar = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = RojoEliminar
                    )
                ) {
                    Text("Rechazar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { solicitudRechazar = null }) {
                    Text("Cancelar", color = PrimaryDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PaseosTabsRow(
    tabActivo: TabPaseos,
    tabs: Map<TabPaseos, TabPaseosData>,
    onTabSelected: (TabPaseos) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Reloj vivo mientras haya inmediatas: descuenta las vencidas del contador
        val hayInmediatas = tabs[TabPaseos.INMEDIATOS]?.items?.isNotEmpty() == true
        val ahora = recordarAhora(hayInmediatas)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabPaseos.entries.forEach { tab ->
                val data = tabs[tab]
                val expiradas = if (tab == TabPaseos.INMEDIATOS) {
                    data?.items?.count { solicitudExpirada(it, ahora) } ?: 0
                } else {
                    0
                }
                val total = ((data?.totalElements ?: 0L) - expiradas).coerceAtLeast(0L)
                val etiqueta = if (total > 0L) {
                    "${tab.etiquetaUI} ($total)"
                } else {
                    tab.etiquetaUI
                }
                val activa = tab == tabActivo

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = etiqueta,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activa) PrimaryOrange else TextoGris,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (activa) PrimaryOrange else Color.Transparent)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BordeGris)
        )
    }
}

@Composable
private fun PaseosTabContent(
    tab: TabPaseos,
    data: TabPaseosData?,
    acciones: Map<String, AccionPaseo>,
    onReintentar: () -> Unit,
    onCargarMas: () -> Unit,
    onAceptar: (SolicitudPaseoResDto) -> Unit,
    onSecundario: (SolicitudPaseoResDto) -> Unit
) {
    if (data == null || (data.cargando && data.items.isEmpty() && data.error == null)) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = PrimaryOrange,
                strokeWidth = 3.dp
            )
        }
        return
    }

    if (data.error != null && data.items.isEmpty()) {
        PaseosErrorContent(
            message = data.error,
            onRetry = onReintentar
        )
        return
    }

    if (data.items.isEmpty()) {
        PaseosEmptyContent(tab = tab)
        return
    }

    // Las solicitudes inmediatas vencidas (5 min) desaparecen automáticamente
    val ahora = recordarAhora(tab == TabPaseos.INMEDIATOS)
    val itemsVisibles = if (tab == TabPaseos.INMEDIATOS) {
        data.items.filterNot { solicitudExpirada(it, ahora) }
    } else {
        data.items
    }

    if (itemsVisibles.isEmpty()) {
        PaseosEmptyContent(tab = tab)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = itemsVisibles,
            key = { solicitud -> solicitud.uuid ?: solicitud.hashCode() }
        ) { solicitud ->
            SolicitudPaseoCard(
                solicitud = solicitud,
                tab = tab,
                accionEnCurso = solicitud.uuid?.let { acciones[it] },
                onAceptar = { onAceptar(solicitud) },
                onSecundario = { onSecundario(solicitud) }
            )
        }

        if (data.hasNext) {
            item(key = "cargar_mas") {
                OutlinedButton(
                    onClick = onCargarMas,
                    enabled = !data.cargandoMas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PrimaryOrange.copy(alpha = 0.06f),
                        contentColor = PrimaryOrange
                    )
                ) {
                    if (data.cargandoMas) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PrimaryOrange,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Cargar más",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaseosEmptyContent(tab: TabPaseos) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.EventAvailable,
                contentDescription = null,
                tint = TextoGris,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensajeVacio(tab),
                fontSize = 14.sp,
                color = TextoGris,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SolicitudPaseoCard(
    solicitud: SolicitudPaseoResDto,
    tab: TabPaseos,
    accionEnCurso: AccionPaseo?,
    onAceptar: () -> Unit,
    onSecundario: () -> Unit
) {
    val mascotas = solicitud.mascotas.orEmpty()
    val mascota = mascotas.firstOrNull()
    val nombreMascota = mascota?.nombre?.trim()?.takeIf { it.isNotBlank() } ?: "Mascota"
    val inicial = nombreMascota.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val raza = mascota?.raza?.trim()?.takeIf { it.isNotBlank() }
    val tamano = mascota?.tamano?.trim()?.takeIf { it.isNotBlank() }
    val subtituloMascota = when {
        mascotas.size > 1 -> "${mascotas.size} mascotas" +
            (tamano?.let { " · $it" } ?: "")

        raza != null && tamano != null -> "$raza • $tamano"
        raza != null -> raza
        tamano != null -> tamano
        else -> "Mascota"
    }
    val cliente = solicitud.clienteNombreCompleto?.trim()?.takeIf { it.isNotBlank() }
    val direccion = listOfNotNull(
        solicitud.direccionRecojo?.trim()?.takeIf { it.isNotBlank() },
        solicitud.distritoDescripcion?.trim()?.takeIf { it.isNotBlank() }
    ).joinToString(", ")
    val monto = formatearMonto(solicitud.montoTotal)

    val estado = solicitud.estadoId
    val muestraAcciones = solicitud.uuid != null && when {
        tab == TabPaseos.INMEDIATOS && estado == BandejaPaseadorViewModel.ESTADO_BUSCANDO -> true
        tab == TabPaseos.MIS_PASEOS && estado == BandejaPaseadorViewModel.ESTADO_PENDIENTE -> true
        else -> false
    }
    val textoSecundario = if (tab == TabPaseos.INMEDIATOS) "Descartar" else "Rechazar"
    val borde = if (tab == TabPaseos.INMEDIATOS) {
        BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.5f))
    } else {
        null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = borde
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inicial,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nombreMascota,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtituloMascota,
                        fontSize = 12.sp,
                        color = TextoGris,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                EstadoBadge(estado = estado, descripcion = solicitud.estadoDescripcion)
            }

            if (cliente != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Cliente: $cliente",
                    fontSize = 14.sp,
                    color = PrimaryDark
                )
            }

            if (direccion.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = direccion,
                        fontSize = 12.sp,
                        color = TextoGris,
                        lineHeight = 16.sp
                    )
                }
            }

            // Fecha y hora del paseo (programado: fecha + bloque; inmediato: creado el)
            textoFechaHoraPaseo(
                fechaProgramada = solicitud.fechaProgramada,
                bloqueHorarioCodigo = solicitud.bloqueHorarioCodigo,
                creadoEl = solicitud.creadoEl
            )?.let { fechaHora ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = fechaHora,
                    fontSize = 12.sp,
                    color = TextoGris
                )
            }

            // Contador de los 5 minutos máximos de una solicitud inmediata
            if (tab == TabPaseos.INMEDIATOS && estado == BandejaPaseadorViewModel.ESTADO_BUSCANDO) {
                Spacer(modifier = Modifier.height(6.dp))
                ContadorVencimiento(horaVencimiento = solicitud.horaVencimiento)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = textoVigencia(solicitud),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    fontSize = 12.sp,
                    color = TextoGris,
                    lineHeight = 16.sp
                )

                if (monto != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Monto total",
                            fontSize = 12.sp,
                            color = TextoGris
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = monto,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }
                }
            }

            if (muestraAcciones) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSecundario,
                        enabled = accionEnCurso == null,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RojoEliminar.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = RojoEliminar.copy(alpha = 0.06f),
                            contentColor = RojoEliminar,
                            disabledContainerColor = RojoEliminar.copy(alpha = 0.03f),
                            disabledContentColor = RojoEliminar.copy(alpha = 0.45f)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (accionEnCurso == AccionPaseo.DESCARTAR ||
                            accionEnCurso == AccionPaseo.RECHAZAR
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = RojoEliminar,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = textoSecundario,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = onAceptar,
                        enabled = accionEnCurso == null,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VerdeAceptar,
                            contentColor = Color.White,
                            disabledContainerColor = VerdeAceptar.copy(alpha = 0.45f),
                            disabledContentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (accionEnCurso == AccionPaseo.TOMAR ||
                            accionEnCurso == AccionPaseo.ACEPTAR
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Aceptar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoBadge(
    estado: Int?,
    descripcion: String?
) {
    val fondo = when (estado) {
        BandejaPaseadorViewModel.ESTADO_PENDIENTE -> FondoPendiente
        BandejaPaseadorViewModel.ESTADO_ACEPTADA,
        BandejaPaseadorViewModel.ESTADO_BUSCANDO -> FondoAceptada

        BandejaPaseadorViewModel.ESTADO_EN_CURSO -> FondoEnCurso
        BandejaPaseadorViewModel.ESTADO_RECHAZADA,
        BandejaPaseadorViewModel.ESTADO_CANCELADA,
        BandejaPaseadorViewModel.ESTADO_VENCIDA -> FondoRechazada

        else -> FondoFinalizada
    }
    val colorTexto = when (estado) {
        BandejaPaseadorViewModel.ESTADO_PENDIENTE -> TextoPendiente
        BandejaPaseadorViewModel.ESTADO_ACEPTADA,
        BandejaPaseadorViewModel.ESTADO_BUSCANDO -> VerdeAceptar

        BandejaPaseadorViewModel.ESTADO_EN_CURSO -> TextoEnCurso
        BandejaPaseadorViewModel.ESTADO_RECHAZADA,
        BandejaPaseadorViewModel.ESTADO_CANCELADA,
        BandejaPaseadorViewModel.ESTADO_VENCIDA -> RojoEliminar

        else -> TextoGris
    }
    val etiqueta = descripcion?.trim()?.takeIf { it.isNotBlank() }
        ?: etiquetaEstado(estado)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = etiqueta,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colorTexto,
            maxLines = 1
        )
    }
}

@Composable
private fun PaseosErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "No pudimos cargar los paseos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TextoGris,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Reintentar",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun mensajeVacio(tab: TabPaseos): String = when (tab) {
    TabPaseos.INMEDIATOS -> "No hay solicitudes inmediatas en tu distrito por ahora."
    TabPaseos.MIS_PASEOS -> "Aún no tienes paseos aceptados."
    TabPaseos.EN_CURSO -> "No tienes paseos en curso."
    TabPaseos.HISTORIAL -> "Tu historial de paseos está vacío."
}

private fun textoVigencia(solicitud: SolicitudPaseoResDto): String {
    return when (solicitud.estadoId) {
        BandejaPaseadorViewModel.ESTADO_PENDIENTE -> "Pendiente de confirmación"
        BandejaPaseadorViewModel.ESTADO_ACEPTADA -> "Paseo confirmado"
        BandejaPaseadorViewModel.ESTADO_EN_CURSO -> "En progreso"
        BandejaPaseadorViewModel.ESTADO_BUSCANDO -> tiempoDesdeCreacion(solicitud.creadoEl)
        BandejaPaseadorViewModel.ESTADO_RECHAZADA,
        BandejaPaseadorViewModel.ESTADO_CANCELADA,
        BandejaPaseadorViewModel.ESTADO_FINALIZADA,
        BandejaPaseadorViewModel.ESTADO_VENCIDA -> {
            val estado = solicitud.estadoDescripcion?.trim()?.takeIf { it.isNotBlank() }
                ?: etiquetaEstado(solicitud.estadoId)
            "Estado: $estado"
        }

        else -> solicitud.estadoDescripcion?.trim()?.takeIf { it.isNotBlank() }
            ?.let { "Estado: $it" }
            ?: "Solicitud de paseo"
    }
}

/**
 * "Solicitado hace X min" a partir de `creado_el`.
 * El backend guarda `creado_el` en UTC, así que se compara contra UTC para no
 * desfasar el resultado con la hora local del dispositivo.
 */
private fun tiempoDesdeCreacion(creadoEl: String?): String {
    val texto = creadoEl?.trim()
    if (texto.isNullOrBlank()) return "Solicitud inmediata"

    val creado = runCatching { LocalDateTime.parse(texto).toInstant(ZoneOffset.UTC) }.getOrNull()
        ?: return "Solicitud inmediata"
    val minutos = runCatching {
        Duration.between(creado, Instant.now()).toMinutes()
    }.getOrNull() ?: return "Solicitud inmediata"

    return when {
        minutos < 1L -> "Solicitado hace menos de 1 min"
        minutos == 1L -> "Solicitado hace 1 min"
        else -> "Solicitado hace $minutos min"
    }
}

private fun etiquetaEstado(estado: Int?): String = when (estado) {
    BandejaPaseadorViewModel.ESTADO_PENDIENTE -> "PENDIENTE"
    BandejaPaseadorViewModel.ESTADO_ACEPTADA -> "ACEPTADA"
    BandejaPaseadorViewModel.ESTADO_RECHAZADA -> "RECHAZADA"
    BandejaPaseadorViewModel.ESTADO_CANCELADA -> "CANCELADA"
    BandejaPaseadorViewModel.ESTADO_EN_CURSO -> "EN CURSO"
    BandejaPaseadorViewModel.ESTADO_FINALIZADA -> "FINALIZADA"
    BandejaPaseadorViewModel.ESTADO_VENCIDA -> "VENCIDA"
    BandejaPaseadorViewModel.ESTADO_BUSCANDO -> "BUSCANDO"
    else -> "SIN ESTADO"
}

private fun formatearMonto(monto: Double?): String? {
    val numero = monto ?: return null
    return String.format(Locale.US, "S/ %.2f PEN", numero)
}

private const val MAX_MOTIVO_RECHAZO = 500
