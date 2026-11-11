package com.example.paseape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.paseape.data.model.DisponibilidadSlotDto
import com.example.paseape.data.model.MascotaData
import com.example.paseape.ui.theme.PrimaryDark
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso1Screen(
    viewModel: ReservaPaseoViewModel,
    authHeader: String = "",
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val naranja = Color(0xFFE8702A)
    val slotsState by viewModel.slotsUiState.collectAsState()
    var mascotaDetalleSeleccionada by remember { mutableStateOf<MascotaData?>(null) }
    var slotParaVerMascotas by remember { mutableStateOf<DisponibilidadSlotDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Horario del Paseo", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val bloqueActivo = viewModel.bloqueSeleccionado
                    val cantMascotas = (bloqueActivo?.mascotas?.size ?: 0)
                        .coerceAtLeast(bloqueActivo?.mascotasOcupadas ?: 0)

                    if (bloqueActivo != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { slotParaVerMascotas = bloqueActivo },
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, Color(0xFFFDBA74)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    tint = naranja,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (cantMascotas > 0) {
                                        "Mascotas aceptadas en este bloque ($cantMascotas)"
                                    } else {
                                        "Ver mascotas aceptadas de este bloque"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryDark,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Ver drawer",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = naranja
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    val horaBtn = if (bloqueActivo != null) {
                        val ini = bloqueActivo.horaInicio?.take(5).orEmpty()
                        val fin = bloqueActivo.horaFin?.take(5).orEmpty()
                        if (ini.isNotBlank() && fin.isNotBlank()) " ($ini - $fin)" else ""
                    } else ""

                    Button(
                        onClick = onNext,
                        enabled = bloqueActivo != null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = naranja,
                            disabledContainerColor = Color(0xFFCCCCCC)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = if (bloqueActivo != null) "Continuar con este horario$horaBtn" else "Selecciona un horario disponible",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fecha y horarios según America/Lima (UTC-5)",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // TARJETA DE RESUMEN DEL PASEADOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F0F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!viewModel.fotoPerfilUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = viewModel.fotoPerfilUrl,
                                contentDescription = "Foto paseador",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.logoperro)
                            )
                        } else {
                            val iniciales = viewModel.paseadorNombre.split(" ")
                                .filter { it.isNotBlank() }
                                .take(2)
                                .map { it.first() }
                                .joinToString("")
                                .uppercase()
                            Text(
                                text = iniciales.ifBlank { "P" },
                                fontWeight = FontWeight.Bold,
                                color = naranja,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(viewModel.paseadorNombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("✓ Paseador Verificado", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "S/ %.2f por hora", viewModel.tarifaBaseHora),
                            color = naranja,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val bioTexto = viewModel.biografia?.ifBlank { null }
                            ?: "Paseador profesional verificado con cobertura en ${viewModel.distrito ?: "Lima"}."
                        Text(bioTexto, fontSize = 11.sp, color = Color.Gray, maxLines = 2)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Selecciona el día", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // SELECTOR DINÁMICO DE DÍAS (7 DÍAS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                viewModel.diasDisponibles.forEach { dia ->
                    val isSelected = dia.fechaIso == viewModel.diaSeleccionadoIso
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(horizontal = 2.dp)
                            .background(if (isSelected) naranja else Color.White, RoundedCornerShape(10.dp))
                            .border(1.dp, if (isSelected) naranja else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { viewModel.seleccionarDia(dia, authHeader) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dia.etiqueta,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dia.numero,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Bloques de 1 hora disponibles", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // ESTADOS DE CARGA DE SLOTS
            when (val state = slotsState) {
                is SlotsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = naranja, strokeWidth = 3.dp)
                    }
                }

                is SlotsUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.mensaje,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.cargarDisponibilidadFecha(authHeader, viewModel.diaSeleccionadoIso) }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = naranja, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reintentar", color = naranja, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is SlotsUiState.Empty -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sin turnos disponibles",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "El paseador no tiene bloques activos o disponibles para este día. Por favor elige otra fecha.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is SlotsUiState.Success -> {
                    state.slots.forEach { slot ->
                        val ini = slot.horaInicio?.take(5).orEmpty()
                        val fin = slot.horaFin?.take(5).orEmpty()
                        val horaTexto = if (ini.isNotBlank() && fin.isNotBlank()) "$ini - $fin" else slot.bloqueCodigo.orEmpty()

                        val isSelected = viewModel.bloqueSeleccionado != null && (
                                (viewModel.bloqueSeleccionado?.bloqueHorarioId != null &&
                                        viewModel.bloqueSeleccionado?.bloqueHorarioId == slot.bloqueHorarioId) ||
                                (!viewModel.bloqueSeleccionado?.bloqueCodigo.isNullOrBlank() &&
                                        viewModel.bloqueSeleccionado?.bloqueCodigo == slot.bloqueCodigo)
                        )

                        val cupoTexto = when {
                            slot.disponible == true -> {
                                val c = slot.cuposDisponibles ?: 1
                                if (c == 1) "1 cupo libre" else "$c cupos libres"
                            }
                            slot.estaLleno == true || slot.motivoNoDisponible == "SIN_CUPOS" -> "Completo"
                            slot.motivoNoDisponible == "HORARIO_EXPIRADO" -> "Expirado"
                            else -> "No disponible"
                        }

                        val subTexto = when {
                            isSelected -> "Horario seleccionado"
                            slot.disponible == true -> "Disponible"
                            slot.estaLleno == true || slot.motivoNoDisponible == "SIN_CUPOS" -> "Sin cupos disponibles"
                            slot.motivoNoDisponible == "HORARIO_EXPIRADO" -> "Horario expirado hoy"
                            else -> "Turno fuera de horario"
                        }

                        BloqueHorarioCard(
                            hora = horaTexto,
                            sub = subTexto,
                            cupos = cupoTexto,
                            mascotas = slot.mascotas.orEmpty(),
                            mascotasOcupadas = slot.mascotasOcupadas ?: 0,
                            isSelected = isSelected,
                            isAvailable = slot.disponible == true,
                            onClick = { viewModel.seleccionarBloque(slot) },
                            onVerMascotasClick = { slotParaVerMascotas = slot }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // DRAWER / MODAL BOTTOM SHEET CON LAS MASCOTAS DEL BLOQUE
    slotParaVerMascotas?.let { slot ->
        MascotasAceptadasBloqueBottomSheet(
            slot = slot,
            fechaIso = viewModel.diaSeleccionadoIso,
            onDismiss = { slotParaVerMascotas = null },
            onVerFichaCompleta = { mascota ->
                mascotaDetalleSeleccionada = mascota
            }
        )
    }

    mascotaDetalleSeleccionada?.let { mascota ->
        MascotaBloqueDetalleDialog(
            mascota = mascota,
            onDismiss = { mascotaDetalleSeleccionada = null }
        )
    }
}

@Composable
fun BloqueHorarioCard(
    hora: String,
    sub: String,
    cupos: String,
    mascotas: List<MascotaData> = emptyList(),
    mascotasOcupadas: Int = 0,
    isSelected: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit,
    onVerMascotasClick: () -> Unit = {}
) {
    val naranja = Color(0xFFE8702A)
    val bg = if (isSelected) naranja else if (!isAvailable) Color(0xFFF1F5F9) else Color.White
    val border = if (isSelected) naranja else if (!isAvailable) Color.Transparent else Color(0xFF4CAF50)
    val totalEnBloque = mascotas.size.coerceAtLeast(mascotasOcupadas)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(enabled = isAvailable, onClick = onClick)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = hora,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else if (!isAvailable) Color.Gray else Color.Black
                    )
                    Text(
                        text = sub,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Gray
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else if (!isAvailable) Color(0xFFE2E8F0) else Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = cupos,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else if (!isAvailable) Color.Gray else Color(0xFF2E7D32)
                    )
                }
            }

            // BOTÓN PARA RENDERIZAR EL DRAWER / BOTTOM SHEET CON LAS MASCOTAS
            if (isSelected || totalEnBloque > 0 || isAvailable) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.22f) else Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.6f) else Color(0xFFFDBA74)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onVerMascotasClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else naranja,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ver mascotas aceptadas ($totalEnBloque)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else naranja
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotasAceptadasBloqueBottomSheet(
    slot: DisponibilidadSlotDto,
    fechaIso: String,
    onDismiss: () -> Unit,
    onVerFichaCompleta: (MascotaData) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val naranja = Color(0xFFE8702A)
    val mascotas = slot.mascotas.orEmpty()
    val ini = slot.horaInicio?.take(5).orEmpty()
    val fin = slot.horaFin?.take(5).orEmpty()
    val horaTexto = if (ini.isNotBlank() && fin.isNotBlank()) "$ini - $fin" else slot.bloqueCodigo.orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = naranja.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                tint = naranja,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Mascotas en este Turno",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                        Text(
                            text = "Horario: $horaTexto · Fecha: $fechaIso",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Estas mascotas ya tienen paseos aceptados por el paseador en este turno. Revisa sus características para asegurar compatibilidad.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (mascotas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no hay otras mascotas registradas en este turno.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    mascotas.forEach { mascota ->
                        MascotaItemDrawerCard(
                            mascota = mascota,
                            onVerFicha = { onVerFichaCompleta(mascota) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MascotaItemDrawerCard(
    mascota: MascotaData,
    onVerFicha: () -> Unit
) {
    val naranja = Color(0xFFE8702A)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val fotoMascotaUrl = mascota.fotoUrlEfectiva
                if (!fotoMascotaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = fotoMascotaUrl,
                        contentDescription = mascota.nombre,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = naranja.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                tint = naranja,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mascota.nombre ?: "Mascota",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    val razaTamano = listOfNotNull(
                        mascota.tipoRazaDescripcion?.takeIf { it.isNotBlank() },
                        mascota.tipoTamanoMascotaDescripcion?.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (razaTamano.isNotBlank()) {
                        Text(
                            text = razaTamano,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                TextButton(
                    onClick = onVerFicha,
                    colors = ButtonDefaults.textButtonColors(contentColor = naranja)
                ) {
                    Text("Ver ficha", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chips de información rápida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                mascota.tipoNivelEnergiaDescripcion?.takeIf { it.isNotBlank() }?.let { energia ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "Energía: $energia",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val sociablePerros = when (mascota.sociableConPerros) {
                    1 -> "Sociable con perros: Sí"
                    0 -> "Sociable con perros: No"
                    else -> null
                }
                sociablePerros?.let {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (mascota.sociableConPerros == 1) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = it,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (mascota.sociableConPerros == 1) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MascotaBloqueDetalleDialog(
    mascota: MascotaData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = Color(0xFFE8702A),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mascota.nombre ?: "Detalle de Mascota",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetalleMascotaItem("Raza", mascota.tipoRazaDescripcion ?: "No especificada")
                DetalleMascotaItem("Tamaño", mascota.tipoTamanoMascotaDescripcion ?: "No especificado")
                DetalleMascotaItem("Género", mascota.tipoGeneroMascotaDescripcion ?: "No especificado")
                DetalleMascotaItem("Nivel de energía", mascota.tipoNivelEnergiaDescripcion ?: "No especificado")
                val edadTexto = buildString {
                    val anos = mascota.edadAnos ?: 0
                    val meses = mascota.edadMeses ?: 0
                    if (anos > 0) append("$anos año(s) ")
                    if (meses > 0) append("$meses mes(es)")
                }.trim().ifBlank { "No especificada" }
                DetalleMascotaItem("Edad", edadTexto)
                mascota.pesoKg?.let {
                    DetalleMascotaItem("Peso", String.format(Locale.US, "%.1f kg", it))
                }
                DetalleMascotaItem(
                    "Sociable con perros",
                    if (mascota.sociableConPerros == 1) "Sí" else "No"
                )
                DetalleMascotaItem(
                    "Sociable con personas",
                    if (mascota.sociableConPersonas == 1) "Sí" else "No"
                )
                if (!mascota.precaucionesMedicas.isNullOrBlank()) {
                    DetalleMascotaItem("Precauciones médicas", mascota.precaucionesMedicas)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontWeight = FontWeight.Bold, color = PrimaryDark)
            }
        }
    )
}

@Composable
private fun DetalleMascotaItem(label: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = valor,
            fontSize = 12.sp,
            color = Color(0xFF1E293B),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}
