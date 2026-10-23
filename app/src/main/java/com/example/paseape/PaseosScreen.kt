package com.example.paseape

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.MascotaData
import com.example.paseape.data.model.SolicitudPaseoResDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaseosScreen(
    viewModel: PaseosViewModel,
    authHeader: String = "",
    userUuid: String? = null,
    abrirInmediataInicial: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val naranja = Color(0xFFFF6B00)
    val azulOscuro = Color(0xFF1E293B)

    // 0: Activos, 1: Pendientes, 2: Historial, 3: Solicitud Inmediata (Modo InDrive)
    var selectedTab by remember { mutableIntStateOf(if (abrirInmediataInicial) 3 else 0) }
    val pendientesCount = 1 + viewModel.solicitudesInmediatasCreadas.size
    val tabs = listOf(
        "Activos (1)",
        "Pendientes ($pendientesCount)",
        "Historial (1)",
        "⚡ Inmediato"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 3) "Paseo Inmediato (InDrive)" else "Mis Paseos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    if (selectedTab != 3) {
                        Button(
                            onClick = { selectedTab = 3 },
                            colors = ButtonDefaults.buttonColors(containerColor = naranja),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pedir Ahora",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // =========================================================================
            // BARRA DE PESTAÑAS (TABS)
            // =========================================================================
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) naranja else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Banner de éxito tras crear una solicitud inmediata
            viewModel.exitoSolicitudMensaje?.let { mensajeExito ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = mensajeExito,
                            color = Color(0xFF065F46),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.exitoSolicitudMensaje = null }) {
                            Text("OK", color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // =========================================================================
            // CONTENIDO POR PESTAÑA
            // =========================================================================
            if (selectedTab == 3) {
                FormularioSolicitudInmediata(
                    viewModel = viewModel,
                    authHeader = authHeader,
                    userUuid = userUuid,
                    onSolicitudCreada = { nueva ->
                        Toast.makeText(
                            context,
                            "Solicitud ${nueva.codigoReserva ?: ""} enviada a los paseadores del distrito",
                            Toast.LENGTH_LONG
                        ).show()
                        selectedTab = 1 // Pasar a la pestaña Pendientes para ver la solicitud creada
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tarjeta rápida de acceso a Solicitud Inmediata tipo InDrive
                    item {
                        BannerAccesoSolicitudInmediata(
                            onClick = { selectedTab = 3 }
                        )
                    }

                    when (selectedTab) {
                        0 -> item { CardPaseoEnCurso() }
                        1 -> {
                            // Primero mostramos las solicitudes inmediatas REALES creadas en el backend
                            items(viewModel.solicitudesInmediatasCreadas) { solicitudReal ->
                                CardSolicitudInmediataReal(
                                    solicitud = solicitudReal,
                                    formatearMoneda = { viewModel.formatearMoneda(it) }
                                )
                            }
                            // Y debajo la tarjeta de ejemplo (Mockup mientras HU21 GET esté pendiente)
                            item { CardPaseoPendiente() }
                        }
                        2 -> item { CardPaseoHistorial() }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// =========================================================================
// BANNER DE ACCESO A SOLICITUD INMEDIATA (MODO INDRIVE)
// =========================================================================

@Composable
fun BannerAccesoSolicitudInmediata(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4EC)),
        border = BorderStroke(1.dp, Color(0xFFFFB074)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "¿Necesitas un paseo ahora mismo?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Propón tu tarifa por hora (estilo InDrive) y los paseadores cercanos podrán tomar tu solicitud al instante.",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color(0xFFFF6B00),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Ofertar ➔",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// =========================================================================
// FORMULARIO SOLICITUD INMEDIATA (HU15 - CONECTADO AL BACKEND)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioSolicitudInmediata(
    viewModel: PaseosViewModel,
    authHeader: String,
    userUuid: String?,
    onSolicitudCreada: (SolicitudPaseoResDto) -> Unit
) {
    val naranja = Color(0xFFFF6B00)
    val azulOscuro = Color(0xFF1E293B)
    var dropdownDistritoExpandido by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera explicativa
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = azulOscuro),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFFB074)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Solicitud Inmediata • Modalidad Tipo Taxi",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Elige tus mascotas, la duración y propón la tarifa por hora que deseas pagar. Los paseadores verificados de tu distrito verán tu oferta en tiempo real.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 1. SELECCIÓN DE MASCOTAS REALES (N:M)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. ¿Qué mascotas saldrán a pasear?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = azulOscuro
                        )
                        IconButton(
                            onClick = { viewModel.inicializarDatosCliente(authHeader, userUuid) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recargar mascotas",
                                tint = naranja,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (viewModel.cargandoDatosCliente) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = naranja,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Cargando tus mascotas registradas...", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else if (viewModel.mascotasCliente.isEmpty()) {
                        Text(
                            text = "No encontramos mascotas activas en tu perfil. Registra una mascota en la sección 'Mi Perfil' para solicitar un paseo.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309)
                        )
                    } else {
                        Text(
                            text = "Puedes seleccionar una o varias mascotas (${viewModel.mascotasSeleccionadasUuids.size} seleccionada(s)):",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(viewModel.mascotasCliente) { mascota ->
                                val uuid = mascota.uuid.orEmpty()
                                val seleccionada = viewModel.mascotasSeleccionadasUuids.contains(uuid)
                                MascotaInmediataItem(
                                    mascota = mascota,
                                    isSelected = seleccionada,
                                    onClick = { viewModel.toggleMascotaSeleccionada(uuid) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. PROPÓN TU TARIFA POR HORA (ESTILO INDRIVE)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "2. Propón tu tarifa por hora (Modo InDrive)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = azulOscuro,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ajusta cuánto ofreces pagar por hora de paseo por mascota:",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Controles tipo InDrive: [-5] [-1]  [ S/ XX.XX ]  [+1] [+5]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.ajustarTarifaHora(-5.0) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("-5", fontWeight = FontWeight.Bold, color = azulOscuro)
                        }

                        OutlinedButton(
                            onClick = { viewModel.ajustarTarifaHora(-1.0) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("-1", fontWeight = FontWeight.Bold, color = azulOscuro)
                        }

                        OutlinedTextField(
                            value = viewModel.tarifaHoraInput,
                            onValueChange = { viewModel.actualizarTarifaDesdeTexto(it) },
                            modifier = Modifier.width(125.dp),
                            singleLine = true,
                            prefix = { Text("S/ ", fontWeight = FontWeight.Bold, color = naranja) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                color = naranja
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedButton(
                            onClick = { viewModel.ajustarTarifaHora(1.0) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+1", fontWeight = FontWeight.Bold, color = naranja)
                        }

                        OutlinedButton(
                            onClick = { viewModel.ajustarTarifaHora(5.0) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+5", fontWeight = FontWeight.Bold, color = naranja)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tarifa ofertada: ${viewModel.formatearMoneda(viewModel.tarifaHoraPen)} / hora",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        // 3. DURACIÓN DEL PASEO
        item {
            val p30 = viewModel.formatearMoneda(viewModel.calcularCostoPorDuracion(30))
            val p60 = viewModel.formatearMoneda(viewModel.calcularCostoPorDuracion(60))
            val p90 = viewModel.formatearMoneda(viewModel.calcularCostoPorDuracion(90))
            val p120 = viewModel.formatearMoneda(viewModel.calcularCostoPorDuracion(120))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Duración del paseo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = azulOscuro
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DuracionCard(
                            minutos = 30,
                            desc = "Paseo corto",
                            precio = p30,
                            isSelected = viewModel.tipoDuracionPaseoId == 1,
                            modifier = Modifier.weight(1f)
                        ) { viewModel.tipoDuracionPaseoId = 1 }

                        DuracionCard(
                            minutos = 60,
                            desc = "1 hora estándar",
                            precio = p60,
                            isSelected = viewModel.tipoDuracionPaseoId == 2,
                            modifier = Modifier.weight(1f),
                            tag = "Popular"
                        ) { viewModel.tipoDuracionPaseoId = 2 }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DuracionCard(
                            minutos = 90,
                            desc = "1 hora y media",
                            precio = p90,
                            isSelected = viewModel.tipoDuracionPaseoId == 3,
                            modifier = Modifier.weight(1f)
                        ) { viewModel.tipoDuracionPaseoId = 3 }

                        DuracionCard(
                            minutos = 120,
                            desc = "Paseo largo (2h)",
                            precio = p120,
                            isSelected = viewModel.tipoDuracionPaseoId == 4,
                            modifier = Modifier.weight(1f)
                        ) { viewModel.tipoDuracionPaseoId = 4 }
                    }
                }
            }
        }

        // 4. DISTRITO, DIRECCIÓN DE RECOJO Y MÉTODO DE PAGO
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. Ubicación de recojo y Pago",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = azulOscuro
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Selector de Distrito
                    ExposedDropdownMenuBox(
                        expanded = dropdownDistritoExpandido,
                        onExpandedChange = { dropdownDistritoExpandido = !dropdownDistritoExpandido }
                    ) {
                        OutlinedTextField(
                            value = viewModel.distritoSeleccionado?.descripcion ?: "Selecciona un distrito",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Distrito *") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = naranja)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownDistritoExpandido)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownDistritoExpandido,
                            onDismissRequest = { dropdownDistritoExpandido = false }
                        ) {
                            viewModel.distritosDisponibles.forEach { distrito ->
                                DropdownMenuItem(
                                    text = { Text(distrito.descripcion) },
                                    onClick = {
                                        viewModel.distritoSeleccionado = distrito
                                        dropdownDistritoExpandido = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.direccionRecojo,
                        onValueChange = {
                            viewModel.direccionRecojo = it
                            viewModel.errorSolicitudInmediata = null
                        },
                        label = { Text("Dirección exacta de recojo *") },
                        placeholder = { Text("Ej. Av. Larco 345, Dpto 201") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.referenciaDireccion,
                        onValueChange = { viewModel.referenciaDireccion = it },
                        label = { Text("Referencia (Opcional)") },
                        placeholder = { Text("Ej. Portón negro frente al parque") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Método de pago",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = azulOscuro
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetodoPagoChip(
                            titulo = "📱 Yape / Plin",
                            seleccionado = viewModel.tipoMetodoPagoId == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.tipoMetodoPagoId = 1 }
                        )
                        MetodoPagoChip(
                            titulo = "💵 Efectivo",
                            seleccionado = viewModel.tipoMetodoPagoId == 2,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.tipoMetodoPagoId = 2 }
                        )
                    }
                }
            }
        }

        // 5. RESUMEN TOTAL Y BOTÓN ENVIAR AL BACKEND
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total calculado a pagar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = azulOscuro
                            )
                            Text(
                                text = "${viewModel.minutosSeleccionados} min × ${viewModel.cantidadMascotasSeleccionadas} mascota(s) a ${viewModel.formatearMoneda(viewModel.tarifaHoraPen)}/h",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "${viewModel.formatearMoneda(viewModel.montoTotalEstimado)} PEN",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = naranja
                        )
                    }

                    viewModel.errorSolicitudInmediata?.let { msgError ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msgError,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.enviarSolicitudInmediata(
                                authHeader = authHeader,
                                onSuccess = onSolicitudCreada
                            )
                        },
                        enabled = !viewModel.enviandoSolicitudInmediata,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = naranja),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        if (viewModel.enviandoSolicitudInmediata) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Enviando solicitud...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Buscar Paseador Ahora (${viewModel.formatearMoneda(viewModel.montoTotalEstimado)})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun MascotaInmediataItem(
    mascota: MascotaData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val naranja = Color(0xFFFF6B00)
    Box(
        modifier = Modifier
            .width(135.dp)
            .background(
                if (isSelected) Color(0xFFFFF4EC) else Color.White,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = if (isSelected) naranja else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) naranja else Color(0xFFFFE8D8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else naranja,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mascota.nombre ?: "Mascota",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mascota.tipoRazaDescripcion ?: mascota.tipoTamanoMascotaDescripcion ?: "Registrada",
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MetodoPagoChip(
    titulo: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val naranja = Color(0xFFFF6B00)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) Color(0xFFFFF4EC) else Color.White,
        border = BorderStroke(
            width = if (seleccionado) 2.dp else 1.dp,
            color = if (seleccionado) naranja else Color(0xFFCBD5E1)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = titulo,
                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (seleccionado) naranja else Color(0xFF334155)
            )
        }
    }
}

// =========================================================================
// TARJETA DE SOLICITUD INMEDIATA REAL (RESPUESTA DEL BACKEND)
// =========================================================================

@Composable
fun CardSolicitudInmediataReal(
    solicitud: SolicitudPaseoResDto,
    formatearMoneda: (Double) -> String
) {
    val nombresMascotas = solicitud.mascotas
        ?.mapNotNull { it.nombre }
        ?.joinToString(", ")
        .orEmpty()
        .ifBlank { "Mascota(s) seleccionada(s)" }

    val tarifaHora = solicitud.tarifaHoraPenSnapshot ?: 0.0
    val montoTotal = solicitud.montoTotal ?: 0.0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFFFF6B00)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFF6B00),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Paseo Inmediato • $nombresMascotas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = solicitud.codigoReserva ?: "PAS-INM",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = solicitud.estadoDescripcion ?: "BUSCANDO PASEADOR",
                        color = Color(0xFFE65100),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "📍 ${solicitud.direccionRecojo.orEmpty()} (${solicitud.distritoDescripcion ?: "Lima"})",
                fontSize = 13.sp,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⏱ Duración: ${solicitud.tipoDuracionPaseoDescripcion ?: "60 MINUTOS"} • Pago: ${solicitud.tipoMetodoPagoDescripcion ?: "YAPE / PLIN"}",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color(0xFFFFF8F1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tu oferta: ${formatearMoneda(tarifaHora)}/h",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9A3412)
                    )
                    Text(
                        text = "Total: ${formatearMoneda(montoTotal)} PEN",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF6B00)
                    )
                }
            }
        }
    }
}

// =========================================================================
// TARJETAS INDIVIDUALES (MOCKUP PARA ESTADOS CUANDO HU21 ESTÉ PENDIENTE)
// =========================================================================

@Composable
fun CardPaseoEnCurso() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Bobby", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "SOL-202610-0012", color = Color.Gray, fontSize = 12.sp)
                }
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "EN CURSO",
                        color = Color(0xFF1976D2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Paseador: Roberto Gómez • Mascota Bobby", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "• Paseo iniciado a las 08:05 AM / 25 min transcurridos",
                    color = Color(0xFF2563EB),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { /* Acción llamar */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Llamar Paseador", color = Color(0xFFFF6B00))
                }
                Button(
                    onClick = { /* Acción ver mapa */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Ver Mapa en Vivo", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CardPaseoPendiente() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Lucía Vega", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "SOL-202610-0015", color = Color.Gray, fontSize = 12.sp)
                }
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "PENDIENTE",
                        color = Color(0xFFE65100),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Esperando respuesta de Lucía Vega.", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* Acción cancelar */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B00))
            ) {
                Text("Cancelar Solicitud")
            }
        }
    }
}

@Composable
fun CardPaseoHistorial() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Paseo de ayer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "SOL-202610-0009", color = Color.Gray, fontSize = 12.sp)
                }
                Surface(
                    color = Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "FINALIZADO",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Paseo completado ayer con éxito.", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* Acción calificar */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B00))
            ) {
                Text("Calificar servicio ⭐")
            }
        }
    }
}
