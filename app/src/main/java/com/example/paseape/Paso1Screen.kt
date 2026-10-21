package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
fun Paso1Screen(
    viewModel: ReservaPaseoViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val naranja = Color(0xFFE8702A)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil del Paseador", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = naranja),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Continuar con este horario (${viewModel.horarioSeleccionado})", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fecha y horarios según America/Lima (UTC-5)",
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFF0F0F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("RG", fontWeight = FontWeight.Bold, color = naranja)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(viewModel.paseadorNombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("✓ Verificado", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("S/ ${viewModel.tarifaBaseHora}0 por hora", color = naranja, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Entrenador certificado y paseador enfocado en razas medianas y grandes en San Borja.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Selecciona el día", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val dias = listOf("Mar 19", "Mié 20", "Jue 21", "Vie 22", "Sáb 23", "Dom 24", "Lun 25")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dias.forEach { dia ->
                    val isSelected = dia == viewModel.diaSeleccionado
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 54.dp)
                            .background(if (isSelected) naranja else Color.White, RoundedCornerShape(10.dp))
                            .border(1.dp, if (isSelected) naranja else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { viewModel.diaSeleccionado = dia },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dia.split(" ")[0], fontSize = 10.sp, color = if (isSelected) Color.White else Color.Gray)
                            Text(dia.split(" ")[1], fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Bloques de 1 hora disponibles", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            BloqueHorarioCard("07:00 - 08:00", "Disponible", "4 cupos libres", false, true) { viewModel.horarioSeleccionado = "07:00 - 08:00" }
            Spacer(modifier = Modifier.height(8.dp))
            BloqueHorarioCard("08:00 - 09:00", "Horario seleccionado", "3 cupos libres", true, true) { viewModel.horarioSeleccionado = "08:00 - 09:00" }
            Spacer(modifier = Modifier.height(8.dp))
            BloqueHorarioCard("09:00 - 10:00", "Sin cupos disponibles", "Completo", false, false) {}
        }
    }
}

@Composable
fun BloqueHorarioCard(hora: String, sub: String, cupos: String, isSelected: Boolean, isAvailable: Boolean, onClick: () -> Unit) {
    val naranja = Color(0xFFE8702A)
    val bg = if (isSelected) naranja else if (!isAvailable) Color(0xFFEEEEEE) else Color.White
    val border = if (isSelected) naranja else if (!isAvailable) Color.Transparent else Color(0xFF4CAF50)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(enabled = isAvailable, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(hora, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else if (!isAvailable) Color.Gray else Color.Black)
                Text(sub, fontSize = 11.sp, color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color.White.copy(alpha = 0.2f) else if (!isAvailable) Color.LightGray else Color(0xFFE8F5E9)
            ) {
                Text(
                    text = cupos,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    color = if (isSelected) Color.White else if (!isAvailable) Color.Gray else Color(0xFF2E7D32)
                )
            }
        }
    }
}