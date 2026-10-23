package com.example.paseape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
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

private val OrangeReducido = lerp(PrimaryOrange, Color.Black, 0.18f)

private val TextoGris = Color(0xFF64748B)
private val BordeGris = Color(0xFFE2E8F0)
private val FondoCeldaVacia = Color(0xFFF1F5F9)
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
    var mostrarConfirmacionLimpiar by remember { mutableStateOf(false) }

    LaunchedEffect(token) {
        viewModel.cargar(token)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val resultado = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = if (message.retry) "Reintentar" else null,
                withDismissAction = !message.retry
            )
            if (message.retry && resultado == SnackbarResult.ActionPerformed) {
                viewModel.cargar(token, force = true)
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
                    if (state is DisponibilidadUiState.Success &&
                        state.sinGuardar &&
                        !state.isSaving
                    ) {
                        Surface(
                            color = PrimaryOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Sin guardar",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundOffWhite
                )
            )
        },
        bottomBar = {
            val state = uiState
            if (state is DisponibilidadUiState.Success) {
                DisponibilidadBottomBar(
                    state = state,
                    onDescartar = { viewModel.descartarCambios() },
                    onGuardar = { viewModel.guardar(token) }
                )
            }
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
                        enabled = !state.isSaving,
                        onAjustarCapacidad = { delta -> viewModel.ajustarCapacidadBase(delta) },
                        onLunesAViernes = { viewModel.marcarLunesAViernes() },
                        onFinesDeSemana = { viewModel.marcarFinesDeSemana() },
                        onLimpiar = { mostrarConfirmacionLimpiar = true },
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
            enabled = !state.isSaving,
            onDismiss = { slotSeleccionado = null },
            onSeleccionarCupo = { cupo ->
                viewModel.actualizarCupo(slot, cupo)
                slotSeleccionado = null
            },
            onQuitarBloque = {
                viewModel.quitarSlot(slot)
                slotSeleccionado = null
            }
        )
    }

    if (mostrarConfirmacionLimpiar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionLimpiar = false },
            title = {
                Text(
                    text = "Limpiar semana",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            },
            text = {
                Text(
                    text = "Se desmarcarán todos los bloques de tu disponibilidad. ¿Deseas continuar?",
                    fontSize = 14.sp,
                    color = TextoGris
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.limpiarSemana()
                        mostrarConfirmacionLimpiar = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Limpiar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionLimpiar = false }) {
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
    enabled: Boolean,
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
            enabled = enabled,
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
                enabled = enabled,
                onClick = onLunesAViernes
            )
            AccionChip(
                texto = "Fines de Semana",
                enabled = enabled,
                onClick = onFinesDeSemana
            )
            AccionChip(
                texto = "Limpiar Semana",
                enabled = enabled,
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
            LeyendaItem(color = OrangeReducido, texto = "Personalizado")
        }

        Spacer(modifier = Modifier.height(8.dp))

        DisponibilidadGrid(
            dias = state.diasSemana,
            bloques = state.bloquesHorario,
            slots = state.slots,
            capacidadBase = state.capacidadBase,
            enabled = enabled,
            onSlotVacioClick = onSlotVacioClick,
            onSlotMarcadoClick = onSlotMarcadoClick
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CapacidadBaseCard(
    capacidadBase: Int,
    enabled: Boolean,
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
                    text = "Cupo predeterminado de canes por hora",
                    fontSize = 12.sp,
                    color = TextoGris,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
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
                        contentDescription = "Disminuir capacidad base",
                        enabled = enabled && capacidadBase > 1,
                        onClick = { onAjustar(-1) }
                    )
                    Text(
                        text = "$capacidadBase perros",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        maxLines = 1
                    )
                    StepButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Aumentar capacidad base",
                        enabled = enabled && capacidadBase < 6,
                        onClick = { onAjustar(1) }
                    )
                }
            }
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
private fun AccionChip(
    texto: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(PrimaryOrange.copy(alpha = if (enabled) 0.08f else 0.04f))
            .border(
                width = 1.dp,
                color = PrimaryOrange.copy(alpha = if (enabled) 0.65f else 0.3f),
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) PrimaryOrange else PrimaryOrange.copy(alpha = 0.4f),
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
    enabled: Boolean,
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
                            enabled = enabled,
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
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorFondo = when {
        cupo == null -> FondoCeldaVacia
        esBase -> PrimaryOrange
        else -> OrangeReducido
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
            .clickable(enabled = enabled, onClick = onClick),
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
    state: DisponibilidadUiState.Success,
    onDescartar: () -> Unit,
    onGuardar: () -> Unit
) {
    val ayuda = when {
        state.estaVacia && state.slotsGuardados.isEmpty() -> "Selecciona al menos un bloque"
        state.sinGuardar -> "Cambios sin guardar"
        else -> null
    }
    val puedeDescartar = state.sinGuardar && !state.isSaving

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
            if (ayuda != null) {
                Text(
                    text = ayuda,
                    modifier = Modifier.padding(bottom = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (state.estaVacia) TextoGris else PrimaryOrange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDescartar,
                    enabled = puedeDescartar,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    border = BorderStroke(
                        1.dp,
                        PrimaryOrange.copy(alpha = if (puedeDescartar) 0.55f else 0.25f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryOrange,
                        disabledContentColor = PrimaryOrange.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "Descartar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onGuardar,
                    enabled = state.puedeGuardar,
                    modifier = Modifier
                        .weight(1.8f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        contentColor = Color.White,
                        disabledContainerColor = PrimaryOrange.copy(alpha = 0.35f),
                        disabledContentColor = Color.White
                    )
                ) {
                    if (state.isSaving) {
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
            }

            Spacer(modifier = Modifier.height(6.dp))

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
    enabled: Boolean,
    onDismiss: () -> Unit,
    onSeleccionarCupo: (Int) -> Unit,
    onQuitarBloque: () -> Unit
) {
    val dia = state.diasSemana.firstOrNull { it.id == slot.diaSemanaId }
    val bloque = state.bloquesHorario.firstOrNull { it.id == slot.bloqueHorarioId }
    val cupoActual = state.slots[slot]
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
                text = "Máximo de perros por hora",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextoGris
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..6).forEach { cupo ->
                    CupoOpcion(
                        cupo = cupo,
                        seleccionado = cupoActual == cupo,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onSeleccionarCupo(cupo) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onQuitarBloque,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
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
private fun CupoOpcion(
    cupo: Int,
    seleccionado: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (seleccionado) PrimaryOrange else FondoCeldaVacia)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cupo.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (seleccionado) Color.White else PrimaryDark
        )
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
