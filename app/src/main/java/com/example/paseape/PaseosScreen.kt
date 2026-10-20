package com.example.paseape.ui.screens.paseos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paseape.viewmodel.PaseosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaseosScreen(
    viewModel: PaseosViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Paseos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // =========================================================================
            // MOCK DATA (Activo para probar la interfaz visualmente)
            // =========================================================================
            if (viewModel.listaPaseos.isEmpty()) {
                Text(
                    text = "No tienes paseos registrados.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(viewModel.listaPaseos) { paseo ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Mascota: ${paseo.mascotaNombre}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Fecha: ${paseo.fechaProgramada}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Tarifa: S/ ${paseo.tarifaPen}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // CÓDIGO REAL DE CONEXIÓN (Descomentar cuando conectes la API en PaseosViewModel)
            // =========================================================================
            /*
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                viewModel.errorMessage != null -> {
                    Text(
                        text = "Error: ${viewModel.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                viewModel.listaPaseos.isEmpty() -> {
                    Text(
                        text = "No tienes paseos registrados.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(viewModel.listaPaseos) { paseo ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Mascota: ${paseo.mascotaNombre}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Fecha: ${paseo.fechaProgramada}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Tarifa: S/ ${paseo.tarifaPen}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            */
        }
    }
}