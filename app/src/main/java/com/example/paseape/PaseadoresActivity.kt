package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.Paseador
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark

class PaseadoresActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaseaPETheme {
                PaseadoresHomeScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaseadoresScreen(onBackClick: () -> Unit) {
    // Catálogo maestro de distritos de Lima según tu documentación oficial
    val distritosLima = listOf("Todos", "Miraflores", "San Isidro", "Santiago de Surco", "San Borja", "Barranco", "Jesús María", "Magdalena del Mar", "Lince")
    var distritoSeleccionado by remember { mutableStateOf("Todos") }

    // Lista de prueba usando exactamente la estructura de tu modelo Paseador
    val listaPaseadores = remember {
        listOf(
            Paseador(
                id = 205,
                uuid = "8e7d6c5b-4a3f-4e2d-7f6d-5e4c3b2a4a19",
                nombres = "Mateo Sebastián",
                apellidos = "Quispe Vargas",
                correo = "mateo.quispe.v@gmail.com",
                telefono = "+51912345678",
                foto_perfil_url = null,
                experiencia_anos = 4,
                biografia = "Estudiante de medicina veterinaria en Lima. Experiencia de 4 años en adiestramiento positivo.",
                tarifa_hora_pen = 25.00,
                distrito_cobertura_id = 1,
                distrito_nombre = "Miraflores",
                paseador_estado_verificacion_id = 3, // 3 = Verificado
                calificacion_promedio = 5.00,
                paseos_completados = 12
            ),
            Paseador(
                id = 206,
                uuid = "9f8e7d6c-5b4a-3f1e-8e7d-6c5b4a3f2e1d",
                nombres = "Lucía",
                apellidos = "Fernández Ruiz",
                correo = "lucia.fernandez@gmail.com",
                telefono = "+51987654321",
                foto_perfil_url = null,
                experiencia_anos = 2,
                biografia = "Cuidadora y paseadora canina en zonas residenciales.",
                tarifa_hora_pen = 30.00,
                distrito_cobertura_id = 2,
                distrito_nombre = "San Isidro",
                paseador_estado_verificacion_id = 2, // 2 = En revisión
                calificacion_promedio = 4.80,
                paseos_completados = 8
            )
        )
    }

    val paseadoresFiltrados = if (distritoSeleccionado == "Todos") {
        listaPaseadores
    } else {
        listaPaseadores.filter { it.distrito_nombre.equals(distritoSeleccionado, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Paseadores Verificados",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = PrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundOffWhite)
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
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtro",
                    tint = PrimaryDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cobertura por Distrito:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(distritosLima) { distrito ->
                    FilterChip(
                        selected = distritoSeleccionado == distrito,
                        onClick = { distritoSeleccionado = distrito },
                        label = { Text(text = distrito, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryDark,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color.DarkGray
                        )
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(paseadoresFiltrados) { paseador ->
                    PaseadorDbCard(paseador = paseador)
                }
            }
        }
    }
}

@Composable
fun PaseadorDbCard(paseador: Paseador) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.perrito3),
                contentDescription = paseador.nombres,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .border(2.dp, PrimaryDark, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${paseador.nombres} ${paseador.apellidos}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Validación de paseador_estado_verificacion_id (3 = VERIFICADO)
                    if (paseador.paseador_estado_verificacion_id == 3) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "En revisión",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Distrito",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Zona: ${paseador.distrito_nombre}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${paseador.experiencia_anos} años de experiencia",
                    fontSize = 10.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Calificación",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${paseador.calificacion_promedio} • S/ ${paseador.tarifa_hora_pen} / hora",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}