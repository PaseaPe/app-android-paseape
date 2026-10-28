package com.example.paseape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.BloqueHorarioParamDto
import com.example.paseape.data.model.DiaSemanaParamDto
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark
import com.example.paseape.ui.theme.PrimaryOrange

private val NaranjaPersonalizado = lerp(PrimaryOrange, Color.Black, 0.18f)

private enum class AccionSemana {
    LUNES_A_VIERNES,
    FINES_DE_SEMANA,
    LIMPIAR
}

private val TextoGris = Color(0xFF64748B)
private val BordeGris = Color(0xFFE2E8F0)
private val FondoCeldaVacia = Color(0xFFF1F5F9)
private val RojoEliminar = Color(0xFFD32F2F)
private val HoraColumnWidth = 68.dp
private val SlotHeight = 32.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisponibilidadScreen(
    token: String,
    viewModel: DisponibilidadViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var slotSeleccionado by remember { mutableStateOf<SlotKey?>(null) }
    var accionSemanaPendiente by remember { mutableStateOf<AccionSemana?>(null) }

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
                        text = "Mi Disponibilidad Semanal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                },
                actions = {
                    val state = uiState
                    if (state is DisponibilidadUiState.Success) {
                        val estadoTexto = when {
                            state.isSaving -> "Guardando…"
                            state.hayCambiosSinGuardar -> "Sin guardar"
                            else -> null
                        }
                        if (estadoTexto != null) {
                            Surface(
                                color = PrimaryOrange.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = estadoTexto,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryOrange
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundOffWhite
                )
            )
        },
        bottomBar = {
            DisponibilidadBottomBar(
                state = uiState as? DisponibilidadUiState.Success,
                onGuardar = { viewModel.guardarCambios() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DisponibilidadUiState.Loading -> {
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

                is DisponibilidadUiState.Error -> {
                    DisponibilidadErrorContent(
                        message = state.message,
                        onRetry = { viewModel.cargar(token, force = true) }
                    )
                }

                is DisponibilidadUiState.Success -> {
                    DisponibilidadContenido(
                        state = state,
                        onAjustarCapacidad = { delta -> viewModel.ajustarCapacidadBase(delta) },
                        onLunesAViernes = { accionSemanaPendiente = AccionSemana.LUNES_A_VIERNES },
                        onFinesDeSemana = { accionSemanaPendiente = AccionSemana.FINES_DE_SEMANA },
                        onLimpiar = { accionSemanaPendiente = AccionSemana.LIMPIAR },
                        onSlotVacioClick = { slot -> viewModel.activarSlot(slot) },
                        onSlotMarcadoClick = { slot -> slotSeleccionado = slot }
                    )
                }
            }
        }
    }

    val state = uiState
    if (state is DisponibilidadUiState.Success && slotSeleccionado != null) {
        val slot = slotSeleccionado!!
        EditorCupoBottomSheet(
            state = state,
            slot = slot,
            onDismiss = { slotSeleccionado = null },
            onAjustarCupo = { delta ->
                val cupoActual = state.slots[slot] ?: state.capacidadBase
                viewModel.actualizarCupo(slot, cupoActual + delta)
            },
            onQuitarBloque = {
                viewModel.quitarSlot(slot)
                slotSeleccionado = null
            }
        )
    }

    accionSemanaPendiente?.let { accion ->
        val titulo = when (accion) {
            AccionSemana.LUNES_A_VIERNES -> "Marcar lunes a viernes"
            AccionSemana.FINES_DE_SEMANA -> "Marcar fines de semana"
            AccionSemana.LIMPIAR -> "Limpiar semana"
        }
        val detalle = when (accion) {
            AccionSemana.LUNES_A_VIERNES ->
                "Se activarán los bloques vacíos de lunes a viernes con el cupo base. " +
                    "Los bloques personalizados se mantienen y los cambios se guardan " +
                    "al pulsar Guardar Cambios."
            AccionSemana.FINES_DE_SEMANA ->
                "Se activarán los bloques vacíos de sábado y domingo con el cupo base. " +
                    "Los bloques personalizados se mantienen y los cambios se guardan " +
                    "al pulsar Guardar Cambios."
            AccionSemana.LIMPIAR ->
                "Se desmarcarán todos los bloques de tu disponibilidad. ¿Deseas continuar?"
        }
        val etiqueta = when (accion) {
            AccionSemana.LUNES_A_VIERNES,
            AccionSemana.FINES_DE_SEMANA -> "Marcar"
            AccionSemana.LIMPIAR -> "Limpiar"
        }

        AlertDialog(
            onDismissRequest = { accionSemanaPendiente = null },
            title = {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            },
            text = {
                Text(
                    text = detalle,
                    fontSize = 14.sp,
                    color = TextoGris
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (accion) {
                            AccionSemana.LUNES_A_VIERNES -> viewModel.marcarLunesAViernes()
                            AccionSemana.FINES_DE_SEMANA -> viewModel.marcarFinesDeSemana()
                            AccionSemana.LIMPIAR -> viewModel.limpiarSemana()
                        }
                        accionSemanaPendiente = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = PrimaryOrange
                    )
                ) {
                    Text(etiqueta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accionSemanaPendiente = null }) {
                    Text("Cancelar", color = PrimaryDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DisponibilidadContenido(
    state: DisponibilidadUiState.Success,
    onAjustarCapacidad: (Int) -> Unit,
    onLunesAViernes: () -> Unit,
    onFinesDeSemana: () -> Unit,
    onLimpiar: () -> Unit,
    onSlotVacioClick: (SlotKey) -> Unit,
    onSlotMarcadoClick: (SlotKey) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        CapacidadBaseCard(
            capacidadBase = state.capacidadBase,
            onAjustar = onAjustarCapacidad
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccionChip(
                texto = "Lunes a Viernes",
                onClick = onLunesAViernes
            )
            AccionChip(
                texto = "Fines de Semana",
                onClick = onFinesDeSemana
            )
            AccionChip(
                texto = "Limpiar Semana",
                onClick = onLimpiar
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Disponibilidad por bloque",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryDark
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeyendaItem(color = PrimaryOrange, texto = "Base")
            Spacer(modifier = Modifier.width(10.dp))
            LeyendaItem(color = NaranjaPersonalizado, texto = "Personalizado")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.estaVacia) {
            Text(
                text = "Aún no tienes bloques activos. Toca una celda para activarla con " +
                    "el cupo base y pulsa Guardar Cambios.",
                fontSize = 11.sp,
                color = TextoGris,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        DisponibilidadGrid(
            dias = state.diasSemana,
            bloques = state.bloquesHorario,
            slots = state.slots,
            capacidadBase = state.capacidadBase,
            onSlotVacioClick = onSlotVacioClick,
            onSlotMarcadoClick = onSlotMarcadoClick
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CapacidadBaseCard(
    capacidadBase: Int,
    onAjustar: (Int) -> Unit
) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PrimaryOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Capacidad Base por Bloque",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Cupo que se asigna al activar un bloque",
                    fontSize = 12.sp,
                    color = TextoGris,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            CupoStepper(
                valor = capacidadBase,
                onAjustar = onAjustar
            )
        }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) PrimaryOrange else TextoGris.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CupoStepper(
    valor: Int,
    onAjustar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = CircleShape,
        border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepButton(
                icon = Icons.Default.Remove,
                contentDescription = "Disminuir cupo",
                enabled = valor > 1,
                onClick = { onAjustar(-1) }
            )
            Text(
                text = "$valor mascotas",
                modifier = Modifier.padding(horizontal = 4.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark,
                maxLines = 1
            )
            StepButton(
                icon = Icons.Default.Add,
                contentDescription = "Aumentar cupo",
                enabled = valor < 6,
                onClick = { onAjustar(1) }
            )
        }
    }
}

@Composable
private fun AccionChip(
    texto: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(PrimaryOrange.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = PrimaryOrange.copy(alpha = 0.65f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryOrange,
            maxLines = 1
        )
    }
}

@Composable
private fun LeyendaItem(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = texto,
            fontSize = 11.sp,
            color = TextoGris
        )
    }
}

@Composable
private fun DisponibilidadGrid(
    dias: List<DiaSemanaParamDto>,
    bloques: List<BloqueHorarioParamDto>,
    slots: Map<SlotKey, Int>,
    capacidadBase: Int,
    onSlotVacioClick: (SlotKey) -> Unit,
    onSlotMarcadoClick: (SlotKey) -> Unit
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
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hora",
                    modifier = Modifier.width(HoraColumnWidth),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextoGris
                )
                dias.forEach { dia ->
                    Text(
                        text = diaCorto(dia.descripcion),
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            bloques.forEach { bloque ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rangoHorario(bloque.descripcion),
                        modifier = Modifier.width(HoraColumnWidth),
                        fontSize = 10.sp,
                        color = TextoGris,
                        maxLines = 1
                    )

                    dias.forEach { dia ->
                        val slot = SlotKey(
                            diaSemanaId = dia.id,
                            bloqueHorarioId = bloque.id
                        )
                        val cupo = slots[slot]
                        val esBase = cupo != null && cupo == capacidadBase

                        SlotCell(
                            cupo = cupo,
                            esBase = esBase,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (cupo == null) {
                                    onSlotVacioClick(slot)
                                } else {
                                    onSlotMarcadoClick(slot)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotCell(
    cupo: Int?,
    esBase: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorFondo = when {
        cupo == null -> FondoCeldaVacia
        esBase -> PrimaryOrange
        else -> NaranjaPersonalizado
    }
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .height(SlotHeight)
            .clip(shape)
            .background(colorFondo)
            .then(
                if (cupo == null) {
                    Modifier.border(1.dp, BordeGris, shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (cupo != null) {
            Text(
                text = cupo.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DisponibilidadBottomBar(
    state: DisponibilidadUiState.Success?,
    onGuardar: () -> Unit
) {
    val puedeGuardar = state != null &&
        state.hayCambiosSinGuardar &&
        !state.isSaving

    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state != null) {
                Button(
                    onClick = onGuardar,
                    enabled = puedeGuardar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        contentColor = Color.White,
                        disabledContainerColor = PrimaryOrange.copy(alpha = 0.35f),
                        disabledContentColor = Color.White
                    )
                ) {
                    if (state.isSaving && state.hayCambiosSinGuardar) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Guardar Cambios",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = "Cambios aplican para reservas futuras en Lima Metropolitana",
                fontSize = 11.sp,
                color = TextoGris,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorCupoBottomSheet(
    state: DisponibilidadUiState.Success,
    slot: SlotKey,
    onDismiss: () -> Unit,
    onAjustarCupo: (Int) -> Unit,
    onQuitarBloque: () -> Unit
) {
    val dia = state.diasSemana.firstOrNull { it.id == slot.diaSemanaId }
    val bloque = state.bloquesHorario.firstOrNull { it.id == slot.bloqueHorarioId }
    val cupoActual = state.slots[slot] ?: state.capacidadBase
    val subtitulo = listOfNotNull(
        dia?.descripcion?.let(::capitalizar),
        bloque?.descripcion?.let(::rangoHorario)
    ).joinToString(" · ")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Cupo del bloque",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )

            if (subtitulo.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitulo,
                    fontSize = 13.sp,
                    color = TextoGris
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Máximo de mascotas por hora",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextoGris
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CupoStepper(
                    valor = cupoActual,
                    onAjustar = onAjustarCupo
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onQuitarBloque,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = RojoEliminar.copy(alpha = 0.06f),
                    contentColor = RojoEliminar
                ),
                border = BorderStroke(
                    1.dp,
                    RojoEliminar.copy(alpha = 0.6f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Quitar bloque",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DisponibilidadErrorContent(
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
                    text = "No pudimos cargar tu disponibilidad",
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

/** Deriva "06:00 - 07:00" desde el código "B06_07" del catálogo. */
private fun rangoHorario(codigo: String): String {
    val coincidencia = Regex("B(\\d{2})_(\\d{2})", RegexOption.IGNORE_CASE)
        .find(codigo.trim())
    return if (coincidencia != null) {
        val inicio = coincidencia.groupValues[1]
        val fin = coincidencia.groupValues[2]
        "$inicio:00 - $fin:00"
    } else {
        codigo
    }
}

/** "LUNES" -> "Lun", "MIÉRCOLES" -> "Mié". */
private fun diaCorto(descripcion: String): String {
    val recortado = descripcion.trim().take(3).lowercase()
    return recortado.replaceFirstChar { it.uppercase() }
}

/** "LUNES" -> "Lunes". */
private fun capitalizar(texto: String): String {
    val limpio = texto.trim().lowercase()
    return limpio.replaceFirstChar { it.uppercase() }
}
