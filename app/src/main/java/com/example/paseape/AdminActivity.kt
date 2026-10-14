package com.example.paseape

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.ActualizarEstadoUsuarioRequest
import com.example.paseape.data.model.ClienteAdminDto
import com.example.paseape.data.model.PaseadorAdminDto
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaseaPETheme {
                AdminScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Paseadores", "Clientes")

    val listaPaseadores = remember { mutableStateListOf<PaseadorAdminDto>() }
    val listaClientes = remember { mutableStateListOf<ClienteAdminDto>() }
    var isLoading by remember { mutableStateOf(true) }

    fun cargarDatosAdmin() {
        coroutineScope.launch {
            isLoading = true
            try {
                val sharedPref = context.getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
                val token = sharedPref.getString("JWT_TOKEN", null)
                    ?: sharedPref.getString("token", null)
                    ?: ""

                val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                Log.d("ADMIN_DEBUG", "Usando token en peticiones: $bearerToken")

                val responsePaseadores = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.listarPaseadoresAdmin(bearerToken)
                }

                val responseClientes = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.listarClientesAdmin(bearerToken)
                }

                if (responsePaseadores.response != null) {
                    listaPaseadores.clear()
                    listaPaseadores.addAll(responsePaseadores.response)
                }

                if (responseClientes.response != null) {
                    listaClientes.clear()
                    listaClientes.addAll(responseClientes.response)
                }

            } catch (e: Exception) {
                Log.e("ADMIN_ERROR", "Error al cargar datos de admin: ${e.localizedMessage}", e)
                Toast.makeText(context, "Error al cargar datos: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        cargarDatosAdmin()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Regresar",
                    tint = PrimaryDark
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Panel de Administrador",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = "Gestión de usuarios y validaciones",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PrimaryDark
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PrimaryDark)
            } else {
                if (selectedTab == 0) {
                    if (listaPaseadores.isEmpty()) {
                        Text("No hay paseadores registrados", color = Color.Gray)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(listaPaseadores) { paseador ->
                                AdminPaseadorCardReal(paseador = paseador)
                            }
                        }
                    }
                } else {
                    if (listaClientes.isEmpty()) {
                        Text("No hay clientes registrados", color = Color.Gray)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(listaClientes) { cliente ->
                                AdminClienteCardReal(
                                    cliente = cliente,
                                    onCambiarEstadoClick = { uuid, nuevoEstadoId ->
                                        coroutineScope.launch {
                                            try {
                                                val sharedPref = context.getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
                                                val token = sharedPref.getString("JWT_TOKEN", null) ?: sharedPref.getString("token", null) ?: ""
                                                val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

                                                val requestBody = ActualizarEstadoUsuarioRequest(usuarioEstadoId = nuevoEstadoId)

                                                val response = withContext(Dispatchers.IO) {
                                                    RetrofitClient.apiService.actualizarEstadoUsuario(bearerToken, uuid, requestBody)
                                                }

                                                if (response.statusCode == 200) {
                                                    Toast.makeText(context, "Estado actualizado con éxito", Toast.LENGTH_SHORT).show()
                                                    cargarDatosAdmin()
                                                } else {
                                                    Toast.makeText(context, "No se pudo actualizar el estado", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Log.e("ADMIN_ERROR", "Error al cambiar estado: ${e.localizedMessage}")
                                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPaseadorCardReal(paseador: PaseadorAdminDto) {
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${paseador.nombres ?: ""} ${paseador.apellidos ?: ""}".trim(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = "Distrito: ${paseador.distrito ?: "No especificado"}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val estado = paseador.estadoVerificacion ?: "PENDIENTE"
                    val isAprobado = estado == "APROBADO"
                    Icon(
                        imageVector = if (isAprobado) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Estado",
                        tint = if (isAprobado) Color(0xFF2E7D32) else Color(0xFFED6C02),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = estado,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAprobado) Color(0xFF2E7D32) else Color(0xFFED6C02)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminClienteCardReal(
    cliente: ClienteAdminDto,
    onCambiarEstadoClick: (String, Int) -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showProfileDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${cliente.nombres ?: ""} ${cliente.apellidos ?: ""}".trim(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = cliente.correo ?: "Sin correo",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toca para ver perfil",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
            val estadoUser = cliente.estadoUsuario ?: "ACTIVO"
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (estadoUser == "ACTIVO") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ) {
                Text(
                    text = estadoUser,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (estadoUser == "ACTIVO") Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }

    // Diálogo de Perfil del Cliente
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = "Perfil del Cliente",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Nombres y Apellidos:", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "${cliente.nombres} ${cliente.apellidos}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Correo electrónico:", fontSize = 12.sp, color = Color.Gray)
                    Text(text = cliente.correo ?: "No registrado", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Estado actual:", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = cliente.estadoUsuario ?: "ACTIVO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cliente.estadoUsuario == "ACTIVO") Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            },
            confirmButton = {
                val esActivo = cliente.estadoUsuario == "ACTIVO"
                Button(
                    onClick = {
                        showProfileDialog = false
                        showConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esActivo) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                ) {
                    Text(text = if (esActivo) "Inactivar Usuario" else "Activar Usuario", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de confirmación para cambiar estado
    if (showConfirmDialog) {
        val nuevoEstadoId = if (cliente.estadoUsuario == "ACTIVO") 2 else 1
        val textoAccion = if (cliente.estadoUsuario == "ACTIVO") "inactivar" else "activar"

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar acción") },
            text = { Text("¿Estás seguro de que deseas $textoAccion a este usuario?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    if (cliente.uuid != null) {
                        onCambiarEstadoClick(cliente.uuid, nuevoEstadoId)
                    }
                }) {
                    Text("Sí, confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}