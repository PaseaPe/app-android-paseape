package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso3Screen(
    viewModel: ReservaPaseoViewModel,
    onBack: () -> Unit,
    onConfirmSuccess: () -> Unit
) {
    val naranja = Color(0xFFE8702A)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmar Solicitud", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
                    Button(
                        onClick = {
                            viewModel.enviarSolicitud {
                                onConfirmSuccess()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = naranja),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Confirmar y Enviar Solicitud", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "El total se muestra en PEN (Soles)",
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumen del paseo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).background(Color(0xFFFFE8D8), CircleShape), contentAlignment = Alignment.Center) {
                            Text("RG", fontSize = 12.sp, color = naranja, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(viewModel.paseadorNombre, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Tarifa base: S/ ${viewModel.tarifaBaseHora}0/h", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).background(Color(0xFFFFE8D8), CircleShape), contentAlignment = Alignment.Center) {
                            Text("B", fontSize = 12.sp, color = naranja, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(viewModel.mascotaSeleccionada, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Golden Retriever • 24.5 kg", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("📅 ${viewModel.diaSeleccionado} / ${viewModel.horarioSeleccionado} (${viewModel.duracionMinutos} min)", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("📍 ${viewModel.direccionRecojo}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Desglose de pago", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tarifa ${viewModel.duracionMinutos} min de paseo", fontSize = 12.sp, color = Color.Gray)
                        Text("S/ ${viewModel.totalEstimado}0", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Comisión de plataforma PaseaPe", fontSize = 12.sp, color = Color.Gray)
                        Text("S/ 0.00", fontSize = 12.sp)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total a Pagar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("S/ ${viewModel.totalEstimado}0 PEN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Antes de confirmar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "El paseador tiene hasta 2 horas antes del inicio para aceptar tu solicitud. El pago se coordina tras la confirmación.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}