package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark

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
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Paseadores", "Clientes")

    // Datos simulados de prueba (luego los conectarás con tu BD / API)
    var listaPaseadores = remember {
        mutableStateListOf(
            AdminPaseador("1", "Mateo Flores", "Magdalena", "Pendiente"),
            AdminPaseador("2", "Valeria Castillo", "San Miguel", "Verificado"),
            AdminPaseador("3", "Daniel Rojas", "Pueblo Libre", "Pendiente")
        )
    }

    var listaClientes = remember {
        mutableStateListOf(
            AdminCliente("1", "Carlos Mendoza", "Miraflores", "Activo"),
            AdminCliente("2", "Lucía Fernández", "San Isidro", "Inactivo")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
    ) {
        // Cabecera
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

        // Pestañas (Tabs) para alternar entre Paseadores y Clientes
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

        // Contenido según la pestaña seleccionada
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            if (selectedTab == 0) {
                // Lista de Paseadores con estado de verificación
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(listaPaseadores) { paseador ->
                        AdminPaseadorCard(
                            paseador = paseador,
                            onVerifyClick = {
                                val index = listaPaseadores.indexOf(paseador)
                                if (index != -1) {
                                    listaPaseadores[index] = paseador.copy(
                                        estado = if (paseador.estado == "Verificado") "Pendiente" else "Verificado"
                                    )
                                }
                            }
                        )
                    }
                }
            } else {
                // Lista de Clientes registrados
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(listaClientes) { cliente ->
                        AdminClienteCard(cliente = cliente)
                    }
                }
            }
        }
    }
}

// Modelos de datos temporales para la vista de administrador
data class AdminPaseador(val id: String, val nombre: String, val distrito: String, val estado: String)
data class AdminCliente(val id: String, val nombre: String, val distrito: String, val estado: String)

@Composable
fun AdminPaseadorCard(
    paseador: AdminPaseador,
    onVerifyClick: () -> Unit
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paseador.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = "Distrito: ${paseador.distrito}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isVerificado = paseador.estado == "Verificado"
                    Icon(
                        imageVector = if (isVerificado) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Estado",
                        tint = if (isVerificado) Color(0xFF2E7D32) else Color(0xFFED6C02),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = paseador.estado,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isVerificado) Color(0xFF2E7D32) else Color(0xFFED6C02)
                    )
                }
            }

            // Botón de verificar / cambiar estado al costado
            Button(
                onClick = onVerifyClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (paseador.estado == "Verificado") Color.LightGray else PrimaryDark
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (paseador.estado == "Verificado") "Desverificar" else "Verificar",
                    fontSize = 12.sp,
                    color = if (paseador.estado == "Verificado") Color.DarkGray else Color.White
                )
            }
        }
    }
}

@Composable
fun AdminClienteCard(cliente: AdminCliente) {
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
            Column {
                Text(
                    text = cliente.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = "Distrito: ${cliente.distrito}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9)
            ) {
                Text(
                    text = cliente.estado,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}