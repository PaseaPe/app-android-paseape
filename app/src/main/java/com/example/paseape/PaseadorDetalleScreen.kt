package com.example.paseape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.paseape.data.model.DisponibilidadPaseadorDto
import com.example.paseape.data.model.UsuarioPaseadorDetalleDto
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaseadorDetalleScreen(
    uiState: PaseadorDetalleUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onReservarClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Perfil del Paseador",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
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
        bottomBar = {
            // Botón inferior fijo para contratar o reservar servicio
            if (uiState is PaseadorDetalleUiState.Success) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onReservarClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF97316) // Naranja principal del mockup
                            )
                        ) {
                            Text(
                                text = "Contactar / Reservar Paseo",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
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
                    PaseadorDetalleContent(
                        detalle = uiState.detalle,
                        disponibilidad = uiState.disponibilidad
                    )
                }
            }
        }
    }
}

@Composable
fun PaseadorDetalleContent(
    detalle: UsuarioPaseadorDetalleDto,
    disponibilidad: DisponibilidadPaseadorDto? = null
) {
    val paseador = detalle.paseador
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. TARJETA CABECERA (Perfil, Ubicación y Badges)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circular
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
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
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Nombre completo
                    Text(
                        text = detalle.nombreCompleto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Ubicación / Distrito
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = (paseador?.distritoCoberturaDescripcion ?: "LIMA").uppercase(),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges de Estado y Verificación
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val verificacion = paseador?.estadoVerificacionDescripcion ?: "PENDIENTE"
                        Surface(
                            color = Color(0xFFFFEDD5),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "VERIFICACIÓN: $verificacion".uppercase(),
                                color = Color(0xFFC2410C),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        val estadoUsuario = detalle.usuarioEstadoDescripcion ?: "ACTIVO"
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = estadoUsuario.uppercase(),
                                color = Color(0xFF166534),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ID / UUID sutil
                    Text(
                        text = "ID ${detalle.id ?: 1} · UUID ${detalle.hashCode().toString().take(8)}...",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // ==========================================
        // 2. TARJETA MÉTRICAS (Tarifa, Experiencia, Paseos)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tarifa
                val tarifa = paseador?.tarifaHoraPen ?: 0.0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "S/ %.2f", tarifa),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tarifa / hora",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(30.dp),
                    color = Color(0xFFE2E8F0)
                )

                // Experiencia
                val exp = paseador?.experienciaAnos ?: 0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$exp años",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Experiencia",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(30.dp),
                    color = Color(0xFFE2E8F0)
                )

                // Paseos y Calificación
                val paseos = paseador?.paseosCompletados ?: 0
                val calificacion = paseador?.calificacionPromedio ?: 0.0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$paseos paseos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⭐ ${String.format(Locale.US, "%.1f", calificacion)}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. TARJETA DOCUMENTACIÓN DE CONFIANZA
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Documentación de Confianza",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tipo de documento",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                val tipoDocNombre = when (paseador?.tipoDocumentoId) {
                    1 -> "DNI"
                    2 -> "Carné de Extranjería"
                    3 -> "Pasaporte"
                    else -> "DNI"
                }
                Text(
                    text = tipoDocNombre,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Número de documento",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = paseador?.numeroDocumento?.takeIf { it.isNotBlank() } ?: "No registrado",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Contacto",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                val correoVal = detalle.correo ?: paseador?.correo ?: "correo@example.com"
                val telVal = detalle.telefono ?: paseador?.telefono ?: "+51 900000000"
                Text(
                    text = "$correoVal\n$telVal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
            }
        }

        // ==========================================
        // 4. TARJETA SOBRE EL PASEADOR
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sobre el paseador",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = paseador?.biografia?.ifBlank { null }
                        ?: "Entrenador certificado y paseador enfocado en razas medianas y grandes.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
            }
        }

        // ==========================================
        // 5. TARJETA DISPONIBILIDAD Y CUPOS (SPRINT 2)
        // ==========================================
        val slotsActivos = disponibilidad?.slots?.filter { it.activo == true }.orEmpty()
        if (slotsActivos.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Horarios y Disponibilidad",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val capacidadBase = disponibilidad?.maxMascotasPredeterminado ?: 3
                    Text(
                        text = "Capacidad base: hasta $capacidadBase mascotas por turno",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val diasAgrupados = slotsActivos.groupBy { it.diaSemanaDescripcion ?: "SEMANAL" }

                    diasAgrupados.forEach { (dia, slotsDelDia) ->
                        Text(
                            text = dia.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            slotsDelDia.forEach { slot ->
                                val inicio = slot.horaInicio?.take(5) ?: ""
                                val fin = slot.horaFin?.take(5) ?: ""
                                val textoHora = if (inicio.isNotBlank() && fin.isNotBlank()) "$inicio-$fin" else slot.bloqueCodigo.orEmpty()
                                val libre = slot.disponible == true && slot.estaLleno != true

                                Surface(
                                    color = if (libre) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (libre) Color(0xFF93C5FD) else Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = textoHora,
                                        fontSize = 11.sp,
                                        color = if (libre) Color(0xFF1E40AF) else Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
