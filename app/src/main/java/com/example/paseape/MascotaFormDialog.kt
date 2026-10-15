package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.paseape.data.SessionManager
import com.example.paseape.data.model.ActualizarMascotaRequest
import com.example.paseape.data.model.CrearMascotaRequest
import com.example.paseape.data.model.GeneroMascotaParamDto
import com.example.paseape.data.model.MascotaData
import com.example.paseape.data.model.NivelEnergiaParamDto
import com.example.paseape.data.model.RazaParamDto
import com.example.paseape.data.model.TamanoMascotaParamDto
import com.example.paseape.data.model.TipoMascotaParamDto
import com.example.paseape.ui.theme.PrimaryDark

@Composable
fun AgregarMascotaDialog(
    usuarioUuid: String,
    token: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (CrearMascotaRequest) -> Unit
) {
    val context = LocalContext.current
    val authToken = token ?: remember(context) { SessionManager.getToken(context) }

    MascotaFormDialog(
        titulo = "Nueva Mascota",
        initialMascota = null,
        token = authToken,
        usuarioUuid = usuarioUuid,
        onDismiss = onDismiss,
        onConfirmCrear = onConfirm,
        onConfirmActualizar = null
    )
}

@Composable
fun EditarMascotaDialog(
    mascota: MascotaData,
    token: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (ActualizarMascotaRequest) -> Unit
) {
    val context = LocalContext.current
    val authToken = token ?: remember(context) { SessionManager.getToken(context) }

    MascotaFormDialog(
        titulo = "Editar Mascota",
        initialMascota = mascota,
        token = authToken,
        usuarioUuid = "",
        onDismiss = onDismiss,
        onConfirmCrear = null,
        onConfirmActualizar = onConfirm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotaFormDialog(
    titulo: String,
    initialMascota: MascotaData? = null,
    token: String?,
    usuarioUuid: String = "",
    viewModel: PetFormViewModel = remember { PetFormViewModel() },
    onDismiss: () -> Unit,
    onConfirmCrear: ((CrearMascotaRequest) -> Unit)? = null,
    onConfirmActualizar: ((ActualizarMascotaRequest) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialMascota) {
        viewModel.cargarCatalogos(token, initialMascota)
    }

    Dialog(
        onDismissRequest = {
            if (!uiState.isSubmitting) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // ENCABEZADO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryDark.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    tint = PrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = titulo,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !uiState.isSubmitting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BANNER DE CARGA INICIAL DE CATÁLOGOS
                if (uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryDark,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Cargando catálogos maestros...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    // CONTENIDO SCROLLABLE DEL FORMULARIO
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // BANNER DE ERROR (VALIDACIÓN O RED)
                        if (!uiState.errorMessage.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = uiState.errorMessage ?: "",
                                        fontSize = 13.sp,
                                        color = Color(0xFFC62828),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // ----------------------------------------------------
                        // SECCIÓN 1: DATOS BÁSICOS
                        // ----------------------------------------------------
                        FormSectionHeader(title = "Datos Básicos")

                        // Nombre
                        OutlinedTextField(
                            value = uiState.nombre,
                            onValueChange = { viewModel.onNombreChange(it) },
                            label = { Text("Nombre de la mascota *") },
                            placeholder = { Text("Ej. Milo, Firulais") },
                            singleLine = true,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Selector Maestro: Tipo de Mascota
                        CatalogDropdownField(
                            label = "Tipo de Mascota *",
                            items = uiState.tiposMascota,
                            selectedItem = uiState.selectedTipoMascota,
                            itemLabel = { it.descripcion },
                            placeholder = "Seleccionar tipo (ej. Perro, Gato)",
                            enabled = !uiState.isSubmitting && uiState.tiposMascota.isNotEmpty(),
                            onItemSelected = { tipo ->
                                viewModel.onTipoMascotaSelected(tipo, token)
                            }
                        )

                        // Selector Maestro en Cascada: Raza (depende de Tipo de Mascota)
                        CatalogDropdownField(
                            label = "Raza *",
                            items = uiState.razasFiltradas,
                            selectedItem = uiState.selectedRaza,
                            itemLabel = { it.descripcion },
                            placeholder = when {
                                uiState.selectedTipoMascota == null -> "Primero selecciona un tipo de mascota"
                                uiState.isLoadingRazas -> "Cargando razas..."
                                uiState.razasFiltradas.isEmpty() -> "No hay razas registradas para este tipo"
                                else -> "Seleccionar raza"
                            },
                            enabled = uiState.isRazaEnabled && uiState.razasFiltradas.isNotEmpty(),
                            onItemSelected = { raza ->
                                viewModel.onRazaSelected(raza)
                            },
                            supportingText = if (uiState.selectedTipoMascota == null) {
                                { Text("Habilita este selector eligiendo un tipo de mascota.", fontSize = 11.sp, color = Color.Gray) }
                            } else null
                        )

                        // Selector Maestro: Género
                        CatalogDropdownField(
                            label = "Género de la Mascota *",
                            items = uiState.generosMascota,
                            selectedItem = uiState.selectedGenero,
                            itemLabel = { it.descripcion },
                            placeholder = "Seleccionar género (Macho, Hembra)",
                            enabled = !uiState.isSubmitting && uiState.generosMascota.isNotEmpty(),
                            onItemSelected = { genero ->
                                viewModel.onGeneroSelected(genero)
                            }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // ----------------------------------------------------
                        // SECCIÓN 2: CARACTERÍSTICAS FÍSICAS Y COMPORTAMIENTO
                        // ----------------------------------------------------
                        FormSectionHeader(title = "Características Físicas y Comportamiento")

                        // Selector Maestro: Tamaño
                        CatalogDropdownField(
                            label = "Tamaño *",
                            items = uiState.tamanosMascota,
                            selectedItem = uiState.selectedTamano,
                            itemLabel = { tamano ->
                                if (!tamano.rangoPesoRef.isNullOrBlank()) {
                                    "${tamano.descripcion} (${tamano.rangoPesoRef})"
                                } else {
                                    tamano.descripcion
                                }
                            },
                            placeholder = "Seleccionar tamaño (ej. Mediano)",
                            enabled = !uiState.isSubmitting && uiState.tamanosMascota.isNotEmpty(),
                            onItemSelected = { tamano ->
                                viewModel.onTamanoSelected(tamano)
                            },
                            supportingText = uiState.selectedTamano?.rangoPesoRef?.let { ref ->
                                { Text("Referencia de peso: $ref", fontSize = 11.sp, color = Color.Gray) }
                            }
                        )

                        // Selector Maestro: Nivel de Energía
                        CatalogDropdownField(
                            label = "Nivel de Energía *",
                            items = uiState.nivelesEnergia,
                            selectedItem = uiState.selectedNivelEnergia,
                            itemLabel = { it.descripcion },
                            placeholder = "Seleccionar nivel de energía",
                            enabled = !uiState.isSubmitting && uiState.nivelesEnergia.isNotEmpty(),
                            onItemSelected = { nivel ->
                                viewModel.onNivelEnergiaSelected(nivel)
                            }
                        )

                        // Fila Años y Meses
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.edadAnos,
                                onValueChange = { viewModel.onEdadAnosChange(it) },
                                label = { Text("Edad (Años)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = !uiState.isSubmitting,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = uiState.edadMeses,
                                onValueChange = { viewModel.onEdadMesesChange(it) },
                                label = { Text("Edad (Meses)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = !uiState.isSubmitting,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Peso en KG (Decimal)
                        OutlinedTextField(
                            value = uiState.pesoKg,
                            onValueChange = { viewModel.onPesoKgChange(it) },
                            label = { Text("Peso en KG *") },
                            placeholder = { Text("Ej. 4.50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // ----------------------------------------------------
                        // SECCIÓN 3: SALUD Y CONVIVENCIA
                        // ----------------------------------------------------
                        FormSectionHeader(title = "Salud y Convivencia")

                        FormSwitchRow(
                            label = "¿Esterilizado / Castrado?",
                            checked = uiState.esterilizado,
                            enabled = !uiState.isSubmitting,
                            onCheckedChange = { viewModel.onEsterilizadoChange(it) }
                        )

                        FormSwitchRow(
                            label = "Sociable con otros perros",
                            checked = uiState.sociableConPerros,
                            enabled = !uiState.isSubmitting,
                            onCheckedChange = { viewModel.onSociableConPerrosChange(it) }
                        )

                        FormSwitchRow(
                            label = "Sociable con personas",
                            checked = uiState.sociableConPersonas,
                            enabled = !uiState.isSubmitting,
                            onCheckedChange = { viewModel.onSociableConPersonasChange(it) }
                        )

                        // Precauciones médicas multilínea
                        OutlinedTextField(
                            value = uiState.precaucionesMedicas,
                            onValueChange = { viewModel.onPrecaucionesMedicasChange(it) },
                            label = { Text("Precauciones Médicas") },
                            placeholder = { Text("Tratamientos, alergias, indicaciones especiales...") },
                            minLines = 2,
                            maxLines = 4,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Foto URL opcional
                        OutlinedTextField(
                            value = uiState.fotoUrl,
                            onValueChange = { viewModel.onFotoUrlChange(it) },
                            label = { Text("URL de Foto (Opcional)") },
                            placeholder = { Text("https://storage.paseape.com/...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ACCIONES INFERIORES: CANCELAR Y GUARDAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !uiState.isSubmitting,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar", color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                if (initialMascota != null) {
                                    val req = viewModel.buildActualizarMascotaRequest()
                                    if (req != null) {
                                        onConfirmActualizar?.invoke(req)
                                    }
                                } else {
                                    val req = viewModel.buildCrearMascotaRequest(usuarioUuid)
                                    if (req != null) {
                                        onConfirmCrear?.invoke(req)
                                    }
                                }
                            },
                            enabled = !uiState.isSubmitting,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (initialMascota != null) "Guardar Cambios" else "Registrar Mascota",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryDark,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun FormSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryDark
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CatalogDropdownField(
    label: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    placeholder: String,
    enabled: Boolean,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: (@Composable () -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedItem?.let(itemLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder, fontSize = 13.sp, color = Color.Gray) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            supportingText = supportingText,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = enabled),
            shape = RoundedCornerShape(12.dp),
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors()
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel(item),
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
