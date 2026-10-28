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
    tabInicial: Int = 1,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val naranja = Color(0xFFFF6B00)
    val azulOscuro = Color(0xFF1E293B)

    // Si se abrió desde el inicio como "Paseo Inmediato", mostrar como ventana independiente
    var modoVentanaInmediata by remember { mutableStateOf(abrirInmediataInicial) }

    // 0: Activos, 1: Pendientes, 2: Historial (Mis Paseos sin la pestaña Inmediato)
    var selectedTab by remember { mutableIntStateOf(tabInicial.coerceIn(0, 2)) }
    var solicitudParaCancelar by remember { mutableStateOf<SolicitudPaseoResDto?>(null) }

    if (modoVentanaInmediata) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Paseo Inmediato",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
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
                        modoVentanaInmediata = false
                        selectedTab = 1
                    }
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Paseos",
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
                    IconButton(onClick = { viewModel.cargarBandejaCliente(authHeader) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar bandeja",
                            tint = azulOscuro
                        )
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
            // Oculta las solicitudes inmediatas cuyos 5 minutos ya vencieron y
            // descuéntalas del contador de la pestaña.
            val pendientes = viewModel.solicitudesPendientes
            val hayInmediatas = pendientes.any { it.estadoId == ESTADO_SOLICITUD_BUSCANDO }
            val ahora = recordarAhora(hayInmediatas)
            val pendientesVigentes = if (hayInmediatas) {
                pendientes.filterNot { solicitudExpirada(it, ahora) }
            } else {
                pendientes
            }
            val expiradasOcultas = pendientes.size - pendientesVigentes.size

            val tabs = listOf(
                "Activos (${viewModel.totalActivos})",
                "Pendientes (${(viewModel.totalPendientes - expiradasOcultas).coerceAtLeast(0L)})",
                "Historial (${viewModel.totalHistorial})"
            )

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

            // Banner de éxito tras crear o cancelar una solicitud
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
            // CONTENIDO POR PESTAÑA (ACTIVOS, PENDIENTES, HISTORIAL)
            // =========================================================================
            val listaActual = when (selectedTab) {
                0 -> viewModel.solicitudesActivas
                1 -> pendientesVigentes
                else -> viewModel.solicitudesHistorial
            }

            val mensajeVacio = when (selectedTab) {
                0 -> "No tienes paseos en curso en este momento."
                1 -> "No tienes solicitudes pendientes ni aceptadas por iniciar."
                else -> "Aún no tienes paseos registrados en tu historial."
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.cargandoBandeja && listaActual.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = naranja)
                        }
                    }
                } else if (viewModel.errorBandeja != null && listaActual.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = viewModel.errorBandeja ?: "Error al cargar la bandeja",
                                    color = Color(0xFFDC2626),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(onClick = { viewModel.cargarBandejaCliente(authHeader) }) {
                                    Text("Reintentar", color = naranja)
                                }
                            }
                        }
                    }
                } else if (listaActual.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = mensajeVacio,
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(listaActual, key = { it.uuid ?: it.codigoReserva ?: it.hashCode().toString() }) { solicitudReal ->
                        val permiteCancelar = selectedTab == 1 &&
                            (solicitudReal.estadoId == 1 || solicitudReal.estadoId == 2 || solicitudReal.estadoId == 8)
                        CardSolicitudInmediataReal(
                            solicitud = solicitudReal,
                            formatearMoneda = { viewModel.formatearMoneda(it) },
                            permiteCancelar = permiteCancelar,
                            cancelando = viewModel.cancelandoUuid == solicitudReal.uuid,
                            onCancelarClick = { solicitudParaCancelar = solicitudReal }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    solicitudParaCancelar?.let { solicitud ->
        DialogoCancelarSolicitudCliente(
            solicitud = solicitud,
            cancelando = viewModel.cancelandoUuid == solicitud.uuid,
            onDismiss = { solicitudParaCancelar = null },
            onConfirmar = { motivo ->
                val uuid = solicitud.uuid.orEmpty()
                viewModel.cancelarSolicitud(
                    authHeader = authHeader,
                    solicitudUuid = uuid,
                    motivo = motivo,
                    onSuccess = {
                        solicitudParaCancelar = null
                        Toast.makeText(context, "Solicitud cancelada exitosamente", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
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
                            text = "Solicitud Inmediata ",
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
                        text = "2. Propón tu tarifa por hora",
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
// TARJETA DE SOLICITUD DE PASEO REAL (RESPUESTA DEL BACKEND - HU21)
// =========================================================================

@Composable
fun CardSolicitudInmediataReal(
    solicitud: SolicitudPaseoResDto,
    formatearMoneda: (Double) -> String,
    permiteCancelar: Boolean = false,
    cancelando: Boolean = false,
    onCancelarClick: () -> Unit = {}
) {
    val nombresMascotas = solicitud.mascotas
        ?.mapNotNull { it.nombre }
        ?.joinToString(", ")
        .orEmpty()
        .ifBlank { "Mascota(s) seleccionada(s)" }

    val esProgramado = solicitud.tipoModalidadPaseoId == 1 ||
        solicitud.tipoModalidadPaseoDescripcion?.equals("PROGRAMADO", ignoreCase = true) == true
    val tituloModalidad = if (esProgramado) "Paseo Programado" else "Paseo Inmediato"
    val tarifaHora = solicitud.tarifaHoraPenSnapshot ?: 0.0
    val montoTotal = solicitud.montoTotal ?: 0.0

    val (badgeBg, badgeTextColor) = when (solicitud.estadoId) {
        2 -> Color(0xFFECFDF5) to Color(0xFF047857) // ACEPTADA
        5 -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8) // EN_CURSO
        6 -> Color(0xFFF0FDF4) to Color(0xFF15803D) // FINALIZADA
        3, 4, 7 -> Color(0xFFFEF2F2) to Color(0xFFB91C1C) // RECHAZADA, CANCELADA, VENCIDA
        else -> Color(0xFFFFF3E0) to Color(0xFFE65100) // PENDIENTE / BUSCANDO
    }

    val borderColor = when (solicitud.estadoId) {
        2 -> Color(0xFF10B981)
        5 -> Color(0xFF3B82F6)
        3, 4, 7 -> Color(0xFFE2E8F0)
        6 -> Color(0xFFCBD5E1)
        else -> Color(0xFFFF6B00)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                            imageVector = if (esProgramado) Icons.Default.Pets else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFF6B00),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$tituloModalidad • $nombresMascotas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = solicitud.codigoReserva ?: "PAS-SOL",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = solicitud.estadoDescripcion ?: if (esProgramado) "PENDIENTE" else "BUSCANDO PASEADOR",
                        color = badgeTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!solicitud.paseadorNombreCompleto.isNullOrBlank()) {
                Text(
                    text = "👤 Paseador: ${solicitud.paseadorNombreCompleto}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else if (!esProgramado && solicitud.estadoId == 8) {
                Text(
                    text = "🔎 Buscando un paseador disponible en tu distrito...",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Fecha y hora del paseo (programado: fecha + bloque; inmediato: creado el)
            textoFechaHoraPaseo(
                fechaProgramada = solicitud.fechaProgramada,
                bloqueHorarioCodigo = solicitud.bloqueHorarioCodigo,
                creadoEl = solicitud.creadoEl
            )?.let { fechaHora ->
                Text(
                    text = fechaHora,
                    fontSize = 12.sp,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Contador de los 5 minutos máximos de una solicitud inmediata
            if (!esProgramado && solicitud.estadoId == ESTADO_SOLICITUD_BUSCANDO) {
                ContadorVencimiento(horaVencimiento = solicitud.horaVencimiento)
                Spacer(modifier = Modifier.height(6.dp))
            }

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
                        text = if (esProgramado) "Tarifa: ${formatearMoneda(tarifaHora)}/h" else "Tu oferta: ${formatearMoneda(tarifaHora)}/h",
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

            if (permiteCancelar && !solicitud.uuid.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCancelarClick,
                    enabled = !cancelando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    if (cancelando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFFDC2626),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancelando...")
                    } else {
                        Text("Cancelar Solicitud", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoCancelarSolicitudCliente(
    solicitud: SolicitudPaseoResDto,
    cancelando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (String?) -> Unit
) {
    val requiereMotivoObligatorio = solicitud.estadoId == 2 // ACEPTADA requiere >= 10 caracteres según backend
    var motivo by remember { mutableStateOf("") }
    var errorValidacion by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!cancelando) onDismiss() },
        title = {
            Text(
                text = "Cancelar solicitud ${solicitud.codigoReserva.orEmpty()}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column {
                Text(
                    text = if (requiereMotivoObligatorio) {
                        "Este paseo ya fue aceptado por el paseador. Por favor, ingresa el motivo de cancelación (mínimo 10 caracteres):"
                    } else {
                        "¿Estás seguro de que deseas cancelar esta solicitud de paseo?"
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = {
                        motivo = it
                        errorValidacion = null
                    },
                    label = {
                        Text(if (requiereMotivoObligatorio) "Motivo de cancelación *" else "Motivo (opcional)")
                    },
                    placeholder = { Text("Ej. Surgió un imprevisto y no estaré en casa") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                errorValidacion?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limpio = motivo.trim()
                    if (requiereMotivoObligatorio && limpio.length < 10) {
                        errorValidacion = "El motivo es obligatorio y debe tener al menos 10 caracteres."
                        return@Button
                    }
                    onConfirmar(limpio.ifBlank { null })
                },
                enabled = !cancelando,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("Sí, cancelar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !cancelando
            ) {
                Text("Volver")
            }
        }
    )
}
