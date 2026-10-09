package com.example.paseape

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.PaseadoresResponse
// import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark

class PaseadoresActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaseaPETheme {
                PaseadoresScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaseadoresScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    /*
    // =========================================================================
    // TODO: DESCOMENTAR ESTE BLOQUE CUANDO EL ENDPOINT EN SPRING BOOT ESTÉ LISTO
    // =========================================================================
    var listaPaseadoresApi by remember { mutableStateOf<List<PaseadorResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // Caso A: Si la API retorna directamente un PaseadoresResponse (objeto contenedor)
            // val responseContainer: PaseadoresResponse = RetrofitClient.apiService.obtenerPaseadores()
            // listaPaseadoresApi = responseContainer.paseadores

            // Caso B: Si la API retorna List<PaseadorResponse> directamente
            // listaPaseadoresApi = RetrofitClient.apiService.obtenerPaseadores()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al conectar con la API: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }
    */

    // DATOS DE PRUEBA DE ROBERTO GÓMEZ SALAS (MOCK DATA)
    val listaPaseadores = remember {
        listOf(
            PaseadoresResponse(
                nombres = "Roberto",
                apellidos = "Gómez Salas",
                correo = "roberto.walker@example.com",
                telefono = "+51944556677",
                tipoUsuarioId = 2,
                tipoDocumentoId = 1,
                numeroDocumento = "45678912",
                antecedentesPolicialesUrl = "https://storage.paseape.com/docs/cert-45678912.pdf",
                experienciaAnos = 4,
                biografia = "Entrenador certificado y paseador enfocado en razas medianas y grandes con más de 4 años cuidando mascotas.",
                tarifaHoraPen = 25.00,
                distritoCoberturaId = 21
            )
        )
    }

    val paseadoresFiltrados = listaPaseadores.filter {
        it.nombres.contains(searchQuery, ignoreCase = true) ||
                it.apellidos.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Paseadores Disponibles",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = PrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = PrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundOffWhite
                )
            )
        },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Buscar por nombre o distrito...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color.Gray
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PrimaryDark,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de Paseadores
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(paseadoresFiltrados) { paseador ->
                    PaseadorCardMejorada(
                        paseador = paseador,
                        onVerPerfilClick = {
                            Toast.makeText(context, "Perfil de ${paseador.nombres}", Toast.LENGTH_SHORT).show()
                        },
                        onReservarClick = {
                            Toast.makeText(context, "Reservar con ${paseador.nombres}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PaseadorCardMejorada(
    paseador: PaseadoresResponse,
    onVerPerfilClick: () -> Unit,
    onReservarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fila Principal: Foto/Iniciales + Nombre + Precio
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar con insignia
                Box {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${paseador.nombres.take(1)}${paseador.apellidos.take(1)}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    // Badge Verificado
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = Color(0xFF10B981),
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Nombre, Calificación e Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${paseador.nombres} ${paseador.apellidos}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Calificación y Opiniones
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Estrella",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.9",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = " (12 reseñas)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Bloque de Precio
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "S/ ${"%.2f".format(paseador.tarifaHoraPen)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryDark
                    )
                    Text(
                        text = "por hora",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Etiquetas (Chips)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipInfo(text = "${paseador.experienciaAnos} años exp.")
                if (paseador.antecedentesPolicialesUrl != null) {
                    ChipInfo(text = "Antecedentes OK", icon = Icons.Default.Security)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Biografía
            Text(
                text = paseador.biografia,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de Acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onVerPerfilClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryDark
                    )
                ) {
                    Text("Ver perfil", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onReservarClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryDark
                    )
                ) {
                    Text("Reservar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChipInfo(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 11.sp,
                color = Color(0xFF334155),
                fontWeight = FontWeight.Medium
            )
        }
    }
}