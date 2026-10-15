package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.paseape.data.model.UsuarioPaseadorDetalleDto
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaseadorDetalleScreen(
    uiState: PaseadorDetalleUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalle del Paseador",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = PrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is PaseadorDetalleUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryDark,
                            strokeWidth = 3.dp
                        )
                    }
                }

                is PaseadorDetalleUiState.Error -> {
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
                                Text(
                                    text = "Error al obtener detalle",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = uiState.message,
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = onRetry,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reintentar", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                is PaseadorDetalleUiState.Success -> {
                    PaseadorDetalleContent(detalle = uiState.detalle)
                }
            }
        }
    }
}

@Composable
fun PaseadorDetalleContent(
    detalle: UsuarioPaseadorDetalleDto
) {
    val paseador = detalle.paseador
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ==========================================
        // 1. SECCIÓN CABECERA: Perfil y Contacto
        // ==========================================
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
                // Foto de perfil o avatar con placeholder
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .border(2.dp, PrimaryDark.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!detalle.fotoPerfilUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = detalle.fotoPerfilUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.logoperro),
                            error = painterResource(id = R.drawable.logoperro)
                        )
                    } else {
                        val inicial1 = detalle.nombres?.take(1).orEmpty()
                        val inicial2 = detalle.apellidos?.take(1).orEmpty()
                        val iniciales = "$inicial1$inicial2".uppercase()
                        Text(
                            text = iniciales.ifBlank { "P" },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nombre Completo
                Text(
                    text = detalle.nombreCompleto,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Estado de Usuario (Badge)
                val estadoUsuario = detalle.usuarioEstadoDescripcion ?: "ACTIVO"
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = estadoUsuario.uppercase(),
                        color = Color(0xFF2E7D32),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Datos de Contacto
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilaContacto(
                        icon = Icons.Default.Email,
                        label = "Correo electrónico",
                        valor = detalle.correo ?: paseador?.correo ?: "No registrado"
                    )

                    FilaContacto(
                        icon = Icons.Default.Phone,
                        label = "Teléfono",
                        valor = detalle.telefono ?: paseador?.telefono ?: "No registrado"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 2. SECCIÓN DATOS DEL PASEADOR
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = PrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Datos del Paseador",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Biografía
                Text(
                    text = "Biografía",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = paseador?.biografia?.ifBlank { null }
                        ?: "Sin biografía registrada.",
                    fontSize = 14.sp,
                    color = Color(0xFF334155),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Atributos en Grid / Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tarifa por Hora
                    val tarifa = paseador?.tarifaHoraPen ?: 0.0
                    ItemDetalleAtributo(
                        modifier = Modifier.weight(1f),
                        titulo = "Tarifa por hora",
                        valor = String.format(Locale.US, "S/ %.2f / h", tarifa),
                        destacado = true
                    )

                    // Años de Experiencia
                    val exp = paseador?.experienciaAnos ?: 0
                    ItemDetalleAtributo(
                        modifier = Modifier.weight(1f),
                        titulo = "Experiencia",
                        valor = "$exp años"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Distrito de Cobertura
                    ItemDetalleAtributo(
                        modifier = Modifier.weight(1f),
                        titulo = "Distrito cobertura",
                        valor = paseador?.distritoCoberturaDescripcion?.uppercase() ?: "NO ESPECIFICADO",
                        icono = Icons.Default.LocationOn
                    )

                    // Estado de Verificación
                    val verificacion = paseador?.estadoVerificacionDescripcion ?: "PENDIENTE"
                    ItemDetalleAtributo(
                        modifier = Modifier.weight(1f),
                        titulo = "Verificación",
                        valor = verificacion.uppercase(),
                        icono = Icons.Default.CheckCircle,
                        iconoColor = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Calificación Promedio
                    val calificacion = paseador?.calificacionPromedio ?: 0.0
                    ItemDetalleAtributo(
                        modifier = Modifier.weight(1f),
                        titulo = "Calificación",
                        valor = String.format(Locale.US, "%.1f ⭐", calificacion)
                    )

                    // Paseos Completados
                    val paseos = paseador?.paseosCompletados ?: 0
                    ItemDetalleAtributo(
                        modifier = Modifier.weight(1f),
                        titulo = "Paseos realizados",
                        valor = "$paseos completados"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FilaContacto(
    icon: ImageVector,
    label: String,
    valor: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
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

        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
            Text(
                text = valor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
fun ItemDetalleAtributo(
    modifier: Modifier = Modifier,
    titulo: String,
    valor: String,
    icono: ImageVector? = null,
    iconoColor: Color = Color(0xFF64748B),
    destacado: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = iconoColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = valor,
                    fontSize = 13.sp,
                    fontWeight = if (destacado) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (destacado) PrimaryDark else Color(0xFF1E293B)
                )
            }
        }
    }
}
