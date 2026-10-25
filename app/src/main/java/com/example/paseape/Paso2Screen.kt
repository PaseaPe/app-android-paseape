package com.example.paseape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso2Screen(
    viewModel: ReservaPaseoViewModel,
    authHeader: String = "",
    userUuid: String? = null,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val naranja = Color(0xFFE8702A)

    val precio30 = viewModel.formatearPrecio(viewModel.calcularCostoDuracion(30))
    val precio60 = viewModel.formatearPrecio(viewModel.calcularCostoDuracion(60))
    val precio90 = viewModel.formatearPrecio(viewModel.calcularCostoDuracion(90))
    val precio120 = viewModel.formatearPrecio(viewModel.calcularCostoDuracion(120))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de la Reserva", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    TextButton(onClick = onNext) {
                        Text(
                            text = "Ver Paso 3 ➔",
                            color = naranja,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total (${viewModel.cantidadMascotasSeleccionadas} mascota(s) • ${viewModel.duracionMinutos} min):",
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "${viewModel.totalEstimadoFormateado} PEN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = naranja),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Revisar Resumen de Solicitud", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
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
            Text("Paso 2 de 3: Mascotas, Duración y Pago", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // 1. SELECCIÓN DE MASCOTAS REALES CONECTADAS A LA API
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿Quién saldrá a pasear?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (authHeader.isNotBlank()) {
                    IconButton(
                        onClick = { viewModel.cargarDatosCliente(authHeader, userUuid) },
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
            }
            Text(
                text = "Puedes seleccionar una o más de tus mascotas registradas:",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (viewModel.cargandoMascotas) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = naranja,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cargando tus mascotas...", fontSize = 12.sp, color = Color.Gray)
                }
            } else if (viewModel.mascotasCliente.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(viewModel.mascotasCliente) { mascota ->
                        val uuid = mascota.uuid.orEmpty()
                        val isSelected = viewModel.mascotasSeleccionadasUuids.contains(uuid)
                        val infoRaza = buildString {
                            append(mascota.tipoRazaDescripcion ?: mascota.tipoTamanoMascotaDescripcion ?: "Mascota")
                            if (mascota.pesoKg != null && mascota.pesoKg > 0) {
                                append(" • ${mascota.pesoKg} kg")
                            }
                        }
                        val energia = mascota.tipoNivelEnergiaDescripcion ?: "Activa"
                        MascotaCard(
                            nombre = mascota.nombre ?: "Mascota",
                            raza = infoRaza,
                            energia = energia,
                            isSelected = isSelected,
                            onClick = { viewModel.toggleMascotaSeleccionada(mascota) }
                        )
                    }
                }
            } else {
                Text(
                    text = "No se encontraron mascotas activas en tu cuenta. Registra tu mascota en 'Mi Perfil'.",
                    fontSize = 12.sp,
                    color = Color(0xFFB45309)
                )
            }

            // 2. DURACIÓN DEL SERVICIO
            Spacer(modifier = Modifier.height(20.dp))
            Text("Duración del servicio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DuracionCard(
                    30,
                    "Media hora básica",
                    precio30,
                    viewModel.duracionMinutos == 30,
                    Modifier.weight(1f)
                ) { viewModel.duracionMinutos = 30 }

                DuracionCard(
                    60,
                    "1 bloque horario",
                    precio60,
                    viewModel.duracionMinutos == 60,
                    Modifier.weight(1f),
                    "Recomendado"
                ) { viewModel.duracionMinutos = 60 }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DuracionCard(
                    90,
                    "2 bloques contiguos",
                    precio90,
                    viewModel.duracionMinutos == 90,
                    Modifier.weight(1f)
                ) { viewModel.duracionMinutos = 90 }

                DuracionCard(
                    120,
                    "Paseo largo (2h)",
                    precio120,
                    viewModel.duracionMinutos == 120,
                    Modifier.weight(1f)
                ) { viewModel.duracionMinutos = 120 }
            }

            // 3. MÉTODO DE PAGO (HU19)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Método de pago", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetodoPagoReservaCard(
                    titulo = "📱 Yape / Plin",
                    subtitulo = "Pago digital rápido",
                    isSelected = viewModel.tipoMetodoPagoId == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.tipoMetodoPagoId = 1 }
                )
                MetodoPagoReservaCard(
                    titulo = "💵 Efectivo",
                    subtitulo = "Pago directo al paseador",
                    isSelected = viewModel.tipoMetodoPagoId == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.tipoMetodoPagoId = 2 }
                )
            }

            // 4. DIRECCIÓN DE RECOJO
            Spacer(modifier = Modifier.height(20.dp))
            Text("Dirección de recojo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = viewModel.direccionRecojo,
                onValueChange = {
                    viewModel.direccionRecojo = it
                    viewModel.errorEnvio = null
                },
                label = { Text("Dirección en Lima *") },
                placeholder = { Text("Ej. Calle Las Begonias 450, Dpto 302") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = viewModel.referenciaRecojo,
                onValueChange = { viewModel.referenciaRecojo = it },
                label = { Text("Referencia para el paseador") },
                placeholder = { Text("Ej. Tocar timbre 302, frente al parque") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MetodoPagoReservaCard(
    titulo: String,
    subtitulo: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val naranja = Color(0xFFE8702A)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFFFF4EC) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) naranja else Color.LightGray
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = titulo,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) naranja else Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitulo,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun MascotaCard(nombre: String, raza: String, energia: String, isSelected: Boolean, onClick: () -> Unit) {
    val naranja = Color(0xFFE8702A)
    Box(
        modifier = Modifier
            .width(138.dp)
            .background(if (isSelected) Color(0xFFFFF8F3) else Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, if (isSelected) naranja else Color.LightGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(nombre.take(1).uppercase(), fontWeight = FontWeight.Bold, color = naranja)
            Text(
                text = nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = raza,
                fontSize = 9.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(color = Color(0xFFFFE8D8), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = energia,
                    fontSize = 9.sp,
                    color = naranja,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun DuracionCard(minutos: Int, desc: String, precio: String, isSelected: Boolean, modifier: Modifier, tag: String? = null, onClick: () -> Unit) {
    val naranja = Color(0xFFE8702A)
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, if (isSelected) naranja else Color.LightGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            if (tag != null) {
                Surface(color = Color(0xFFFFE8D8), shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.End)) {
                    Text(tag, fontSize = 8.sp, color = naranja, modifier = Modifier.padding(4.dp))
                }
            }
            Text("$minutos min", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(precio, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) naranja else Color.Black)
        }
    }
}
