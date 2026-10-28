package com.example.paseape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.paseape.data.model.ActualizarPerfilReqDto
import com.example.paseape.data.model.CrearMascotaRequest
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.MascotaData
import com.example.paseape.data.model.UsuarioClienteData
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.CardSurface
import com.example.paseape.ui.theme.PrimaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    distritos: List<DistritoParamDto> = emptyList(),
    onRetry: () -> Unit,
    onLogoutClick: () -> Unit = {},
    onLogout: () -> Unit = onLogoutClick,
    onBack: () -> Unit = {},
    onGuardarPerfil: (ActualizarPerfilReqDto) -> Unit = {},
    onCrearMascota: (CrearMascotaRequest) -> Unit = {},
    onMascotaClick: (MascotaData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mi Perfil",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = PrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = PrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundOffWhite
                )
            )
        },
        containerColor = BackgroundOffWhite,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ProfileUiState.Loading -> {
                    ProfileLoadingContent()
                }

                is ProfileUiState.Error -> {
                    ProfileErrorContent(
                        errorMessage = uiState.message,
                        onRetry = onRetry,
                        onLogoutClick = onLogoutClick
                    )
                }

                is ProfileUiState.Success -> {
                    ProfileSuccessContent(
                        usuario = uiState.data,
                        distritos = distritos,
                        onLogout = onLogoutClick,
                        onGuardarPerfil = onGuardarPerfil,
                        onCrearMascota = onCrearMascota,
                        onMascotaClick = onMascotaClick
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = PrimaryDark,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando perfil...",
                color = Color.Gray,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No pudimos cargar tu perfil",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reintentar",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onLogoutClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cerrar sesión e iniciar nuevamente",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSuccessContent(
    usuario: UsuarioClienteData,
    distritos: List<DistritoParamDto> = emptyList(),
    onLogout: () -> Unit,
    onGuardarPerfil: (ActualizarPerfilReqDto) -> Unit,
    onCrearMascota: (CrearMascotaRequest) -> Unit,
    onMascotaClick: (MascotaData) -> Unit
) {
    val detalle = usuario.detalleCliente
    val mascotas = detalle?.mascotas ?: emptyList()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddPetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // ENCABEZADO: INFORMACIÓN DEL USUARIO CLIENTE (Alineado con Foto de Perfil)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val fotoPerfilUrl = usuario.fotoUrlEfectiva
                    val inicialNombre = usuario.nombres?.firstOrNull()?.toString() ?: "U"
                    val inicialApellido = usuario.apellidos?.firstOrNull()?.toString() ?: ""
                    val avatarText = "$inicialNombre$inicialApellido".uppercase()

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                if (!fotoPerfilUrl.isNullOrBlank()) Color(0xFFF1F5F9) else PrimaryDark
                            )
                            .border(2.dp, PrimaryDark.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!fotoPerfilUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = fotoPerfilUrl,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.logoperro),
                                error = painterResource(id = R.drawable.logoperro)
                            )
                        } else {
                            Text(
                                text = avatarText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val nombreCompleto = listOfNotNull(usuario.nombres, usuario.apellidos)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { "Cliente PaseaPE" }

                    Text(
                        text = nombreCompleto,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        textAlign = TextAlign.Center
                    )

                    if (!usuario.correo.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = usuario.correo,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        usuario.tipoUsuarioDescripcion?.let { tipo ->
                            ProfileBadgeChip(text = tipo, color = PrimaryDark)
                        }
                        usuario.usuarioEstadoDescripcion?.let { estado ->
                            ProfileBadgeChip(text = estado, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }

        // CARD: DATOS GENERALES DEL CLIENTE (CON BOTÓN "EDITAR DATOS")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Datos Generales y Contacto",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                TextButton(onClick = { showEditProfileDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = PrimaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Editar datos",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileInfoRow(
                        icon = Icons.Default.Call,
                        label = "Teléfono",
                        value = usuario.telefono?.ifBlank { null } ?: "No especificado"
                    )

                    ProfileInfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "Distrito",
                        value = detalle?.distritoDescripcion?.ifBlank { null } ?: "No especificado"
                    )

                    ProfileInfoRow(
                        icon = Icons.Default.Home,
                        label = "Dirección de Referencia",
                        value = detalle?.direccionReferencia?.ifBlank { null } ?: "Sin dirección registrada"
                    )

                    val emergenciaNombre = detalle?.contactoEmergenciaNombre?.ifBlank { null }
                    val emergenciaTelefono = detalle?.contactoEmergenciaTelefono?.ifBlank { null }
                    val contactoEmergencia = when {
                        emergenciaNombre != null && emergenciaTelefono != null -> "$emergenciaNombre ($emergenciaTelefono)"
                        emergenciaNombre != null -> emergenciaNombre
                        emergenciaTelefono != null -> emergenciaTelefono
                        else -> "No registrado"
                    }

                    ProfileInfoRow(
                        icon = Icons.Default.Person,
                        label = "Contacto de Emergencia",
                        value = contactoEmergencia
                    )

                    detalle?.notasAdicionales?.takeIf { it.isNotBlank() }?.let { notas ->
                        ProfileInfoRow(
                            icon = Icons.Default.Info,
                            label = "Notas Adicionales",
                            value = notas
                        )
                    }
                }
            }
        }

        // SECCIÓN: LISTADO REACTIVO DE MASCOTAS (CON BOTÓN "+ AGREGAR MASCOTA")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = PrimaryDark
                    )
                    Text(
                        text = "Mis Mascotas (${mascotas.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                }

                Button(
                    onClick = { showAddPetDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Agregar mascota",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (mascotas.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logoperro),
                            contentDescription = "Sin mascotas",
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No tienes mascotas registradas.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(
                items = mascotas,
                key = { it.uuid ?: it.id?.toString() ?: it.nombre ?: it.hashCode().toString() }
            ) { mascota ->
                MascotaItemCard(
                    mascota = mascota,
                    onClick = { onMascotaClick(mascota) }
                )
            }
        }
    }

    if (showEditProfileDialog) {
        EditarPerfilDialog(
            usuario = usuario,
            distritos = distritos,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { req ->
                showEditProfileDialog = false
                onGuardarPerfil(req)
            }
        )
    }

    if (showAddPetDialog) {
        AgregarMascotaDialog(
            usuarioUuid = usuario.uuid ?: "",
            onDismiss = { showAddPetDialog = false },
            onConfirm = { req ->
                showAddPetDialog = false
                onCrearMascota(req)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilDialog(
    usuario: UsuarioClienteData,
    distritos: List<DistritoParamDto> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (ActualizarPerfilReqDto) -> Unit
) {
    val detalle = usuario.detalleCliente

    var nombres by remember { mutableStateOf(usuario.nombres ?: "") }
    var apellidos by remember { mutableStateOf(usuario.apellidos ?: "") }
    var telefono by remember { mutableStateOf(usuario.telefono ?: "") }
    var fotoPerfilUrl by remember { mutableStateOf(usuario.fotoUrlEfectiva.orEmpty()) }
    var direccion by remember { mutableStateOf(detalle?.direccionReferencia ?: "") }
    var contactoEmergenciaNombre by remember { mutableStateOf(detalle?.contactoEmergenciaNombre ?: "") }
    var contactoEmergenciaTelefono by remember { mutableStateOf(detalle?.contactoEmergenciaTelefono ?: "") }
    var notasAdicionales by remember { mutableStateOf(detalle?.notasAdicionales ?: "") }

    val currentDistritoId = detalle?.distritoId
    val currentDistritoDesc = detalle?.distritoDescripcion
    var selectedDistrito by remember(distritos, currentDistritoId, currentDistritoDesc) {
        mutableStateOf(
            distritos.find { it.id == currentDistritoId }
                ?: distritos.find { it.descripcion.equals(currentDistritoDesc, ignoreCase = true) }
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Editar Perfil",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )

                OutlinedTextField(
                    value = nombres,
                    onValueChange = { nombres = it },
                    label = { Text("Nombres") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    label = { Text("Apellidos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = fotoPerfilUrl,
                    onValueChange = { fotoPerfilUrl = it },
                    label = { Text("Foto de perfil URL (Opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Selector de Distrito
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { if (distritos.isNotEmpty()) dropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDistrito?.descripcion
                            ?: currentDistritoDesc?.takeIf { it.isNotBlank() }
                            ?: if (distritos.isEmpty()) "Cargando distritos..." else "Selecciona tu distrito",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Distrito") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = distritos.isNotEmpty()
                            ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        distritos.forEach { distrito ->
                            DropdownMenuItem(
                                text = { Text(distrito.descripcion, fontSize = 13.sp) },
                                onClick = {
                                    selectedDistrito = distrito
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección de referencia") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = contactoEmergenciaNombre,
                    onValueChange = { contactoEmergenciaNombre = it },
                    label = { Text("Nombre de contacto de emergencia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = contactoEmergenciaTelefono,
                    onValueChange = { contactoEmergenciaTelefono = it },
                    label = { Text("Teléfono de contacto de emergencia") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = notasAdicionales,
                    onValueChange = { notasAdicionales = it },
                    label = { Text("Notas adicionales") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val req = ActualizarPerfilReqDto(
                                nombres = nombres.trim().ifBlank { null },
                                apellidos = apellidos.trim().ifBlank { null },
                                telefono = telefono.trim().ifBlank { null },
                                fotoPerfilUrl = fotoPerfilUrl.trim().ifBlank { null },
                                distritoId = selectedDistrito?.id,
                                direccionReferencia = direccion.trim().ifBlank { null },
                                contactoEmergenciaNombre = contactoEmergenciaNombre.trim().ifBlank { null },
                                contactoEmergenciaTelefono = contactoEmergenciaTelefono.trim().ifBlank { null },
                                notasAdicionales = notasAdicionales.trim().ifBlank { null }
                            )
                            onConfirm(req)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        enabled = nombres.isNotBlank() && apellidos.isNotBlank()
                    ) {
                        Text(text = "Guardar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MascotaItemCard(
    mascota: MascotaData,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABECERA CON FOTO, NOMBRE Y RAZA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val fotoUrl = mascota.fotoUrl?.takeIf { it.isNotBlank() }
                if (fotoUrl != null) {
                    AsyncImage(
                        model = fotoUrl,
                        contentDescription = mascota.nombre,
                        placeholder = painterResource(id = R.drawable.logoperro),
                        error = painterResource(id = R.drawable.logoperro),
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurface),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logoperro),
                        contentDescription = mascota.nombre,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurface)
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mascota.nombre ?: "Mascota",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        mascota.tipoMascotaDescripcion?.takeIf { it.isNotBlank() }?.let { tipo ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryDark.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = tipo.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val raza = mascota.tipoRazaDescripcion
                        ?: "Raza no especificada"
                    Text(
                        text = raza,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )

                    mascota.tipoGeneroMascotaDescripcion?.takeIf { it.isNotBlank() }?.let { genero ->
                        Text(
                            text = genero,
                            fontSize = 12.sp,
                            color = PrimaryDark.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(10.dp))

            // GRILLA / FILAS DE DATOS: EDAD, PESO, TAMAÑO, ENERGÍA
            val anos = mascota.edadAnos ?: 0
            val meses = mascota.edadMeses ?: 0
            val edadTexto = when {
                anos > 0 && meses > 0 -> "$anos años, $meses meses"
                anos > 0 -> "$anos años"
                meses > 0 -> "$meses meses"
                else -> "Edad no especificada"
            }
            val pesoTexto = mascota.pesoKg?.let { "$it kg" } ?: "Peso no especificado"
            val tamanoTexto = mascota.tipoTamanoMascotaDescripcion ?: "Tamaño no especificado"
            val energiaTexto = mascota.tipoNivelEnergiaDescripcion ?: "Nivel no especificado"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Edad:", fontSize = 11.sp, color = Color.Gray)
                    Text(text = edadTexto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Peso:", fontSize = 11.sp, color = Color.Gray)
                    Text(text = pesoTexto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Tamaño:", fontSize = 11.sp, color = Color.Gray)
                    Text(text = tamanoTexto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Nivel de Energía:", fontSize = 11.sp, color = Color.Gray)
                    Text(text = energiaTexto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
                }
            }

            // CHIPS DE CARACTERÍSTICAS (ESTERILIZADO, SOCIABILIDAD)
            val tieneChips = (mascota.esterilizado != null) || (mascota.sociableConPerros == 1) || (mascota.sociableConPersonas == 1)
            if (tieneChips) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (mascota.esterilizado == 1) {
                        ProfileFeatureChip(text = "Esterilizado", isPositive = true)
                    } else if (mascota.esterilizado == 0) {
                        ProfileFeatureChip(text = "No esterilizado", isPositive = false)
                    }

                    if (mascota.sociableConPerros == 1) {
                        ProfileFeatureChip(text = "Sociable perros 🐶", isPositive = true)
                    }

                    if (mascota.sociableConPersonas == 1) {
                        ProfileFeatureChip(text = "Sociable personas 👥", isPositive = true)
                    }
                }
            }

            // PRECAUCIONES MÉDICAS
            mascota.precaucionesMedicas?.takeIf { it.isNotBlank() && !it.equals("ninguna", ignoreCase = true) }?.let { precauciones ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Precauciones: $precauciones",
                            fontSize = 12.sp,
                            color = Color(0xFFBF360C),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileFeatureChip(text: String, isPositive: Boolean) {
    val backgroundColor = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFECEFF1)
    val contentColor = if (isPositive) Color(0xFF2E7D32) else Color.DarkGray

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProfileBadgeChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CardSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryDark,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )
        }
    }
}
