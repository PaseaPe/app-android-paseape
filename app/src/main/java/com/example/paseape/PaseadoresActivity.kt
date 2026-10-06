package com.example.paseape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark
import kotlinx.coroutines.launch

class PaseadoresActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaseaPETheme {
                PaseadoresScreen(
                    onBack = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun PaseadoresScreen(
    onBack: () -> Unit
) {

    var textoBusqueda by remember {
        mutableStateOf("")
    }

    var paseadores by remember {
        mutableStateOf<List<Paseador>>(emptyList())
    }

    var cargando by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    val scope = rememberCoroutineScope()

    // CARGAR PASEADORES DESDE PHP
    LaunchedEffect(Unit) {

        try {

            val respuesta = RetrofitClient.api.obtenerPaseadores()

            if (respuesta.success) {
                paseadores = respuesta.paseadores
            } else {
                error = "No se pudieron cargar los paseadores"
            }

        } catch (e: Exception) {

            error = "Error de conexión: ${e.message}"

        } finally {

            cargando = false
        }
    }

    val paseadoresFiltrados = paseadores.filter {

        val nombreCompleto =
            "${it.nombre} ${it.apellido}"

        nombreCompleto.contains(
            textoBusqueda,
            ignoreCase = true
        ) ||
                it.distrito.contains(
                    textoBusqueda,
                    ignoreCase = true
                )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
    ) {

        // CABECERA

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Regresar",
                    tint = PrimaryDark
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Column {

                Text(
                    text = "Paseadores",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )

                Text(
                    text = "Encuentra el paseador ideal",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // BUSCADOR

        OutlinedTextField(
            value = textoBusqueda,
            onValueChange = {
                textoBusqueda = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = {
                Text("Buscar por nombre o distrito")
            },
            leadingIcon = {

                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        if (cargando) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator(
                    color = PrimaryDark
                )
            }

        } else if (error.isNotEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "No se pudieron cargar los paseadores",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Button(
                        onClick = {

                            cargando = true
                            error = ""

                            scope.launch {

                                try {

                                    val respuesta =
                                        RetrofitClient.api.obtenerPaseadores()

                                    if (respuesta.success) {
                                        paseadores =
                                            respuesta.paseadores
                                    } else {
                                        error =
                                            "No se pudieron cargar los paseadores"
                                    }

                                } catch (e: Exception) {

                                    error =
                                        "Error de conexión: ${e.message}"

                                } finally {

                                    cargando = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryDark
                        )
                    ) {

                        Text("Reintentar")
                    }
                }
            }

        } else {

            Text(
                text = "${paseadoresFiltrados.size} paseadores disponibles",
                modifier = Modifier.padding(horizontal = 20.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryDark
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(paseadoresFiltrados) { paseador ->

                    PaseadorCard(
                        paseador = paseador,
                        onClick = {
                            // Después abriremos el perfil
                            // del paseador
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PaseadorCard(
    paseador: Paseador,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // FOTO
            Image(
                painter = painterResource(
                    id = R.drawable.iconpaseador
                ),
                contentDescription = "Foto de ${paseador.nombre}",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "${paseador.nombre} ${paseador.apellido}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Calificación",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = paseador.calificacion.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        tint = Color.Gray,
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(3.dp)
                    )

                    Text(
                        text = paseador.distrito,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "S/ ${paseador.tarifa_hora} por hora",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryDark
                ),
                contentPadding = PaddingValues(
                    horizontal = 14.dp
                )
            ) {

                Text(
                    text = "Ver",
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}