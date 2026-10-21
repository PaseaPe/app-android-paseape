package com.example.paseape

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.paseape.data.SessionManager
import com.example.paseape.data.model.PaseadorDto
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark
import java.util.Locale
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class PaseadoresActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(this)[PaseadoresViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cargarDatos()

        setContent {
            PaseaPETheme {
                val uiState by viewModel.uiState.collectAsState()

                PaseadoresScreen(
                    uiState = uiState,
                    onBackClick = { finish() },
                    onRetry = { cargarDatos() },
                    onDistritoSelected = { distrito ->
                        viewModel.filtrarPorDistrito(distrito)
                    },
                    onPaseadorClick = { usuarioUuid ->
                        val intent = Intent(this, PaseadorDetalleActivity::class.java).apply {
                            putExtra(PaseadorDetalleActivity.EXTRA_USUARIO_UUID, usuarioUuid)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun cargarDatos() {
        val authHeader = SessionManager.getAuthHeader(this)
        viewModel.cargarPaseadores(authHeader)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaseadoresScreen(
    uiState: PaseadoresListUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onDistritoSelected: (String?) -> Unit,
    onPaseadorClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Paseadores Disponibles",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = PrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundOffWhite
                )
            )
        },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is PaseadoresListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryDark,
                            strokeWidth = 3.dp
                        )
                    }
                }

                is PaseadoresListUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Error al obtener paseadores",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = uiState.message,
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = onRetry,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reintentar", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                is PaseadoresListUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header con selector de distrito horizontal (Todos, San Borja, etc.)
                        SelectorDistritoHeader(
                            distritos = uiState.distritosDisponibles,
                            distritoSeleccionado = uiState.distritoSeleccionado,
                            onDistritoSelected = onDistritoSelected
                        )

                        if (uiState.paseadores.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay paseadores disponibles para el distrito seleccionado.",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                            ) {
                                items(
                                    items = uiState.paseadores,
                                    key = { it.usuarioUuid ?: it.uuid ?: it.id.toString() }
                                ) { paseador ->
                                    PaseadorCard(
                                        paseador = paseador,
                                        onClick = {
                                            val uUuid = paseador.usuarioUuid
                                            if (!uUuid.isNullOrBlank()) {
                                                onPaseadorClick(uUuid)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDistritoHeader(
    distritos: List<String>,
    distritoSeleccionado: String?,
    onDistritoSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Filtrar por distrito de cobertura",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = distritoSeleccionado ?: "Todos los distritos",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                // Opción "Todos"
                DropdownMenuItem(
                    text = { Text("Todos los distritos", fontWeight = FontWeight.Bold) },
                    onClick = {
                        onDistritoSelected(null)
                        expanded = false
                    }
                )

                // Lista desplegable con los distritos del servidor
                distritos.forEach { distrito ->
                    DropdownMenuItem(
                        text = { Text(distrito) },
                        onClick = {
                            onDistritoSelected(distrito)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PaseadorCard(
    paseador: PaseadorDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circular con Iniciales
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PrimaryDark.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val inicial1 = paseador.nombres?.take(1).orEmpty()
                val inicial2 = paseador.apellidos?.take(1).orEmpty()
                val iniciales = "$inicial1$inicial2".uppercase()
                Text(
                    text = iniciales.ifBlank { "P" },
                    color = PrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Información: Nombre, Distrito, Calificación y Paseos
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = paseador.nombreCompleto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Distrito de Cobertura
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = paseador.distritoCoberturaDescripcion?.uppercase() ?: "SIN DISTRITO",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calificación Promedio y Paseos Completados
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    val calificacion = paseador.calificacionPromedio ?: 0.0
                    Text(
                        text = String.format(Locale.US, "%.1f", calificacion),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = " • ${paseador.paseosCompletados ?: 0} paseos",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Tarifa por Hora
            Column(horizontalAlignment = Alignment.End) {
                val tarifa = paseador.tarifaHoraPen ?: 0.0
                Text(
                    text = String.format(Locale.US, "S/ %.2f", tarifa),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = PrimaryDark
                )
                Text(
                    text = "/ h",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}