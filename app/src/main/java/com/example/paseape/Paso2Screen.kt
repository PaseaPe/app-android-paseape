package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Paso2Screen(
    viewModel: ReservaPaseoViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val naranja = Color(0xFFE8702A)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de la Reserva", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
                    Text(
                        text = "Total estimado: S/ ${viewModel.totalEstimado}0 PEN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = naranja),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Revisar Resumen de Solicitud", color = Color.White)
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
            Text("Paso 1 de 2: Mascota y Duración", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Text("¿Quién saldrá a pasear?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MascotaCard("Bobby", "Golden Retriever • 24.5 kg", "Energía Alta", viewModel.mascotaSeleccionada == "Bobby") { viewModel.mascotaSeleccionada = "Bobby" }
                MascotaCard("Luna", "Schnauzer • 7.2 kg", "Energía Moderada", viewModel.mascotaSeleccionada == "Luna") { viewModel.mascotaSeleccionada = "Luna" }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Duración del servicio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DuracionCard(30, "Media hora básica", "S/ 15.00", viewModel.duracionMinutos == 30, Modifier.weight(1f)) { viewModel.duracionMinutos = 30 }
                DuracionCard(60, "1 bloque", "S/ 25.00", viewModel.duracionMinutos == 60, Modifier.weight(1f), "Recomendado") { viewModel.duracionMinutos = 60 }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DuracionCard(90, "Requiere 2 slots libres", "S/ 37.50", viewModel.duracionMinutos == 90, Modifier.weight(1f)) { viewModel.duracionMinutos = 90 }
                DuracionCard(120, "Paseo largo", "S/ 50.00", viewModel.duracionMinutos == 120, Modifier.weight(1f)) { viewModel.duracionMinutos = 120 }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Dirección de recojo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = viewModel.direccionRecojo,
                onValueChange = { viewModel.direccionRecojo = it },
                label = { Text("Dirección en Lima *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = viewModel.referenciaRecojo,
                onValueChange = { viewModel.referenciaRecojo = it },
                label = { Text("Referencia para el paseador") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun MascotaCard(nombre: String, raza: String, energia: String, isSelected: Boolean, onClick: () -> Unit) {
    val naranja = Color(0xFFE8702A)
    Box(
        modifier = Modifier
            .width(130.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, if (isSelected) naranja else Color.LightGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(nombre.substring(0, 1), fontWeight = FontWeight.Bold, color = naranja)
            Text(nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(raza, fontSize = 9.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(color = Color(0xFFFFE8D8), shape = RoundedCornerShape(8.dp)) {
                Text(energia, fontSize = 9.sp, color = naranja, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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