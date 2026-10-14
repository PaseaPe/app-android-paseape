package com.example.paseape

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.MascotaResponse
import com.example.paseape.data.model.UserProfileResponse
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.profile.ProfileScreen
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark
import kotlinx.coroutines.launch

class DashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaseaPETheme {
                DashboardMainScreen(
                    onPaseadoresClick = {
                        val intent = Intent(
                            this@DashboardActivity,
                            PaseadoresActivity::class.java
                        )
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardMainScreen(
    onPaseadoresClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // DATOS DE PRUEBA TEMPORALES
    val mockProfile = remember {
        UserProfileResponse(
            nombres = "Diego Alonso",
            apellidos = "Aparicio Vela",
            correo = "diego.cliente@example.com",
            telefono = "+51981234567",
            tipoUsuarioId = 1,
            direccionReferencia = "Calle Las Begonias 450, Dpto 302",
            distritoId = 31,
            contactoEmergenciaNombre = "Carlos Vela",
            contactoEmergenciaTelefono = "+51987654320",
            notasAdicionales = "Edificio con recepción las 24 horas",
            mascotas = listOf(
                MascotaResponse(
                    nombre = "Bobby",
                    tipoMascotaId = 1,
                    tipoRazaId = 4,
                    tipoGeneroMascotaId = 1,
                    tipoTamanoMascotaId = 3,
                    tipoNivelEnergiaId = 3,
                    edadAnos = 2,
                    edadMeses = 4,
                    pesoKg = 24.50,
                    esterilizado = 1,
                    sociableConPerros = 1,
                    sociableConPersonas = 1,
                    precaucionesMedicas = "Alergia cutánea estacional",
                    fotoUrl = "https://images.unsplash.com/photo-1543466835-00a7907e9de1"
                ),
                MascotaResponse(
                    nombre = "Luna",
                    tipoMascotaId = 1,
                    tipoRazaId = 10,
                    tipoGeneroMascotaId = 2,
                    tipoTamanoMascotaId = 2,
                    tipoNivelEnergiaId = 2,
                    edadAnos = 1,
                    edadMeses = 6,
                    pesoKg = 7.20,
                    esterilizado = 0,
                    sociableConPerros = 1,
                    sociableConPersonas = 1,
                    precaucionesMedicas = "Ninguna",
                    fotoUrl = "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e"
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
    ) {

        // CONTENIDO PRINCIPAL SEGÚN LA PESTAÑA SELECCIONADA
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> DashboardHomeContent(onPaseadoresClick = onPaseadoresClick)
                3 -> ProfileScreen(
                    userProfile = mockProfile,
                    onLogout = {
                        scope.launch {
                            try {
                                val sharedPref = context.getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
                                val token = sharedPref.getString("JWT_TOKEN", "") ?: ""

                                // LOG 1: Verificar que el token no esté vacío antes de enviarlo
                                Log.d("LOGOUT_DEBUG", "Token recuperado para enviar: $token")

                                if (token.isNotEmpty()) {
                                    val response = RetrofitClient.apiService.logout("Bearer $token")

                                    // LOG 2: Ver el código de respuesta del servidor
                                    Log.d("LOGOUT_DEBUG", "Código de respuesta del servidor: ${response.code()}")

                                    if (response.isSuccessful) {
                                        Log.d("LOGOUT", "Cierre de sesión exitoso en el backend")
                                    } else {
                                        Log.e("LOGOUT", "El servidor rechazó el logout. Código: ${response.code()}")
                                    }
                                } else {
                                    Log.w("LOGOUT", "No hay token guardado localmente")
                                }
                            } catch (e: Exception) {
                                Log.e("LOGOUT", "Excepción al llamar al logout: ${e.message}")
                            } finally {
                                // Limpia las preferencias
                                val sharedPref = context.getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
                                sharedPref.edit().clear().apply()

                                // Opcional: Redirigir al LoginActivity si lo deseas
                                val intent = Intent(context, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
                )
                else -> DashboardHomeContent(onPaseadoresClick = onPaseadoresClick)
            }
        }

        // BARRA DE NAVEGACIÓN INFERIOR
        BottomNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { index ->
                if (index == 1) {
                    onPaseadoresClick()
                } else {
                    selectedTab = index
                }
            }
        )
    }
}

// =====================================================
// CONTENIDO DE LA PESTAÑA INICIO
// =====================================================

@Composable
fun DashboardHomeContent(
    onPaseadoresClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // CABECERA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú",
                    tint = PrimaryDark
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.perritologoapp),
                    contentDescription = "Logo PaseaPE",
                    modifier = Modifier.height(48.dp),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    tint = PrimaryDark
                )
            }
        }

        // BUSCADOR
        OutlinedTextField(
            value = "",
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = "Buscar servicios", color = Color.Gray) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = { Icon(imageVector = Icons.Default.GridView, contentDescription = "Categorías") },
            singleLine = true,
            shape = RoundedCornerShape(25.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = Color.LightGray,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // CATEGORÍAS
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip(text = "Paseo", selected = true)
            CategoryChip(text = "Cuidado", selected = false)
            CategoryChip(text = "Adiestramiento", selected = false)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // BANNER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(40.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.perritobanner),
                contentDescription = "Encuentra un paseador",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Encuentra un paseador", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(text = "cerca de ti", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Paseadores verificados listos para cuidar a tu mascota.", color = Color.White, fontSize = 10.sp)
                }

                Button(
                    onClick = onPaseadoresClick,
                    modifier = Modifier.width(125.dp).height(38.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(text = "Buscar ahora", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RECOMENDACIÓN
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE8D8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🐾", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Recomendación para ti", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    Text(text = "Encuentra el paseo ideal para tu mascota", fontSize = 10.sp, color = Color.Gray)
                }

                IconButton(onClick = onPaseadoresClick) {
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Ver paseadores", tint = PrimaryDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // RECOMENDADOS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Recomendado para ti", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
            TextButton(onClick = onPaseadoresClick) {
                Text(text = "Ver todo", fontSize = 12.sp, color = PrimaryDark)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecommendedCard(
                modifier = Modifier.weight(1f),
                image = R.drawable.perrito3,
                title = "Paseo individual",
                description = "Paseo para tu mascota"
            )

            RecommendedCard(
                modifier = Modifier.weight(1f),
                image = R.drawable.perrito4,
                title = "Paseo grupal",
                description = "Diversión y compañía"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// =====================================================
// BARRA DE NAVEGACIÓN Y COMPONENTES
// =====================================================

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem(
                icon = Icons.Default.Home,
                text = "Inicio",
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )

            BottomItem(
                icon = Icons.Default.Search,
                text = "Paseadores",
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )

            BottomItem(
                icon = Icons.Default.DateRange,
                text = "Mis Paseos",
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )

            BottomItem(
                icon = Icons.Default.Person,
                text = "Perfil",
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
fun BottomItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (selected) PrimaryDark else Color.Gray,
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = text,
            fontSize = 9.sp,
            color = if (selected) PrimaryDark else Color.Gray
        )
    }
}

@Composable
fun CategoryChip(text: String, selected: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) PrimaryDark else Color.White,
        modifier = Modifier.border(
            width = 1.dp,
            color = if (selected) PrimaryDark else Color.LightGray,
            shape = RoundedCornerShape(20.dp)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 11.sp,
            color = if (selected) Color.White else Color.DarkGray
        )
    }
}

@Composable
fun RecommendedCard(
    modifier: Modifier,
    image: Int,
    title: String,
    description: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Image(
                painter = painterResource(id = image),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = description, fontSize = 9.sp, color = Color.Gray)
            }
        }
    }
}