package com.example.paseape.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.paseape.data.model.MascotaResponse
import com.example.paseape.data.model.UserProfileResponse
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark

@Composable
fun ProfileScreen(
    userProfile: UserProfileResponse,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ENCABEZADO PERFIL DUEÑO
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(PrimaryDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${userProfile.nombres.take(1)}${userProfile.apellidos.take(1)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${userProfile.nombres} ${userProfile.apellidos}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    Text(
                        text = userProfile.correo,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // SECCIÓN MASCOTAS
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Pets, contentDescription = null, tint = PrimaryDark)
                Text(
                    text = "Mis Mascotas (${userProfile.mascotas.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }
        }

        item {
            if (userProfile.mascotas.isEmpty()) {
                Text("No tienes mascotas registradas.", color = Color.Gray)
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(userProfile.mascotas) { mascota ->
                        MascotaCard(mascota = mascota)
                    }
                }
            }
        }

        // DETALLES DE CONTACTO Y DIRECCIÓN
        item {
            Text(
                text = "Información Personal y Dirección",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(
                        icon = Icons.Default.Call,
                        label = "Teléfono",
                        value = userProfile.telefono ?: "No especificado"
                    )
                    InfoRow(
                        icon = Icons.Default.Home,
                        label = "Dirección de Referencia",
                        value = userProfile.direccionReferencia ?: "Sin dirección"
                    )
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Contacto de Emergencia",
                        value = "${userProfile.contactoEmergenciaNombre ?: "N/A"} (${userProfile.contactoEmergenciaTelefono ?: "N/A"})"
                    )
                    userProfile.notasAdicionales?.let {
                        InfoRow(
                            icon = Icons.Default.Info,
                            label = "Notas Adicionales",
                            value = it
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MascotaCard(mascota: MascotaResponse) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = mascota.fotoUrl,
                    contentDescription = mascota.nombre,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = mascota.nombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    Text(
                        text = "${mascota.edadAnos} años, ${mascota.edadMeses} meses",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${mascota.pesoKg} kg",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Precauciones Médicas:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Text(
                text = mascota.precaucionesMedicas ?: "Ninguna",
                fontSize = 13.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (mascota.esterilizado == 1) BadgeChip("Esterilizado")
                if (mascota.sociableConPerros == 1) BadgeChip("Sociable 🐶")
            }
        }
    }
}

@Composable
fun BadgeChip(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PrimaryDark.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
        }
    }
}