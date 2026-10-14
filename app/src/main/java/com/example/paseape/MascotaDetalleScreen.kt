package com.example.paseape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.paseape.data.model.ActualizarMascotaRequest
import com.example.paseape.data.model.MascotaData
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.CardSurface
import com.example.paseape.ui.theme.PrimaryDark

sealed interface MascotaDetalleUiState {
    data object Loading : MascotaDetalleUiState
    data class Success(val data: MascotaData) : MascotaDetalleUiState
    data class Error(val message: String) : MascotaDetalleUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotaDetalleScreen(
    uiState: MascotaDetalleUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onActualizarMascota: (ActualizarMascotaRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalle de Mascota",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = PrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundOffWhite
                )
            )
        },
        containerColor = BackgroundOffWhite,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is MascotaDetalleUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = PrimaryDark,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cargando mascota...",
                                color = Color.Gray,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                is MascotaDetalleUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Error al cargar mascota",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.message,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = onRetry,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Reintentar",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                is MascotaDetalleUiState.Success -> {
                    MascotaDetalleContent(
                        mascota = uiState.data,
                        onActualizarMascota = onActualizarMascota
                    )
                }
            }
        }
    }
}

@Composable
fun MascotaDetalleContent(
    mascota: MascotaData,
    onActualizarMascota: (ActualizarMascotaRequest) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TARJETA PRINCIPAL: IMAGEN, NOMBRE Y RAZA
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val fotoUrl = mascota.fotoUrl?.takeIf { it.isNotBlank() }
                if (fotoUrl != null) {
                    AsyncImage(
                        model = fotoUrl,
                        contentDescription = mascota.nombre,
                        placeholder = painterResource(id = R.drawable.logoperro),
                        error = painterResource(id = R.drawable.logoperro),
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(CardSurface),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logoperro),
                        contentDescription = mascota.nombre,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(CardSurface)
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = mascota.nombre ?: "Mascota",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark,
                    textAlign = TextAlign.Center
                )

                val raza = mascota.tipoRazaDescripcion
                    ?: "Raza no especificada"
                Text(
                    text = raza,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    mascota.tipoMascotaDescripcion?.takeIf { it.isNotBlank() }?.let { tipo ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryDark.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = tipo.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    mascota.tipoGeneroMascotaDescripcion?.takeIf { it.isNotBlank() }?.let { genero ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryDark.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = genero,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // TARJETA DE ESPECIFICACIONES FÍSICAS Y ENERGÍA
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Información Física",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )

                val anos = mascota.edadAnos ?: 0
                val meses = mascota.edadMeses ?: 0
                val edadTexto = when {
                    anos > 0 && meses > 0 -> "$anos años, $meses meses"
                    anos > 0 -> "$anos años"
                    meses > 0 -> "$meses meses"
                    else -> "No especificada"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MascotaStatItem(label = "Edad", value = edadTexto, modifier = Modifier.weight(1f))
                    MascotaStatItem(
                        label = "Peso",
                        value = mascota.pesoKg?.let { "$it kg" } ?: "No especificado",
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MascotaStatItem(
                        label = "Tamaño",
                        value = mascota.tipoTamanoMascotaDescripcion ?: "No especificado",
                        modifier = Modifier.weight(1f)
                    )
                    MascotaStatItem(
                        label = "Nivel de Energía",
                        value = mascota.tipoNivelEnergiaDescripcion ?: "Normal",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // TARJETA DE CONDUCTA Y CARACTERÍSTICAS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Comportamiento y Cuidados",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (mascota.esterilizado == 1) {
                        ProfileFeatureChip(text = "Esterilizado", isPositive = true)
                    } else {
                        ProfileFeatureChip(text = "No esterilizado", isPositive = false)
                    }

                    if (mascota.sociableConPerros == 1) {
                        ProfileFeatureChip(text = "Sociable con perros 🐶", isPositive = true)
                    }

                    if (mascota.sociableConPersonas == 1) {
                        ProfileFeatureChip(text = "Sociable con personas 👥", isPositive = true)
                    }
                }

                // BLOQUE DESTACADO DE PRECAUCIONES MÉDICAS
                Spacer(modifier = Modifier.height(4.dp))
                val precauciones = mascota.precaucionesMedicas?.takeIf { it.isNotBlank() } ?: "Ninguna precaución médica registrada"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Precauciones Médicas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBF360C)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = precauciones,
                                fontSize = 13.sp,
                                color = Color(0xFFBF360C)
                            )
                        }
                    }
                }
            }
        }

        // BOTÓN EDITAR MASCOTA
        Button(
            onClick = { showEditDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Editar Mascota",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showEditDialog) {
        EditarMascotaDialog(
            mascota = mascota,
            onDismiss = { showEditDialog = false },
            onConfirm = { req ->
                showEditDialog = false
                onActualizarMascota(req)
            }
        )
    }
}

@Composable
fun MascotaStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryDark)
    }
}

