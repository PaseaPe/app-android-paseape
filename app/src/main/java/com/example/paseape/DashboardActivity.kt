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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark

class DashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userUuid = intent.getStringExtra("EXTRA_USER_UUID")
            ?: intent.getStringExtra("user_uuid")
            ?: intent.getStringExtra("USER_UUID")
            ?: getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE).getString("user_uuid", null)
            ?: getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE).getString("user_uuid", null)

        setContent {
            PaseaPETheme {
                DashboardMainScreen(
                    onPaseadoresClick = {
                        val intent = Intent(
                            this@DashboardActivity,
                            PaseadoresActivity::class.java
                        )
                        startActivity(intent)
                    },
                    onProfileClick = {
                        val currentUuid = intent.getStringExtra("EXTRA_USER_UUID")
                            ?: intent.getStringExtra("user_uuid")
                            ?: intent.getStringExtra("USER_UUID")
                            ?: getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE).getString("user_uuid", null)
                            ?: getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE).getString("user_uuid", null)

                        val intent = Intent(
                            this@DashboardActivity,
                            AccountActivity::class.java
                        ).apply {
                            putExtra("EXTRA_USER_UUID", currentUuid)
                            putExtra("user_uuid", currentUuid)
                            putExtra("USER_UUID", currentUuid)
                        }
                        startActivity(intent)
                    },
                    onLogoutClick = {
                        cerrarSesion()
                    }
                )
            }
        }
    }

    private fun cerrarSesion() {
        getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this@DashboardActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun DashboardMainScreen(
    onPaseadoresClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

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
                0 -> DashboardHomeContent(
                    onPaseadoresClick = onPaseadoresClick,
                    onProfileClick = onProfileClick,
                    onLogoutClick = onLogoutClick
                )
                else -> DashboardHomeContent(
                    onPaseadoresClick = onPaseadoresClick,
                    onProfileClick = onProfileClick,
                    onLogoutClick = onLogoutClick
                )
            }
        }

        // BARRA DE NAVEGACIÓN INFERIOR
        BottomNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { index ->
                when (index) {
                    1 -> onPaseadoresClick()
                    3 -> onProfileClick()
                    else -> selectedTab = index
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
    onPaseadoresClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

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
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menú",
                        tint = PrimaryDark
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Mi Perfil") },
                        onClick = {
                            showMenu = false
                            onProfileClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryDark)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cerrar Sesión", color = Color(0xFFD32F2F)) },
                        onClick = {
                            showMenu = false
                            onLogoutClick()
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFD32F2F))
                        }
                    )
                }
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
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Ver paseadores", tint = PrimaryDark)
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