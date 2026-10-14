package com.example.paseape

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.UpdateProfileRequestDto
import com.example.paseape.data.model.UsuarioPaseadorDetalleDto
import com.example.paseape.ui.theme.PrimaryDark
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilPaseadorDialog(
    detalle: UsuarioPaseadorDetalleDto,
    distritos: List<DistritoParamDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (UpdateProfileRequestDto) -> Unit
) {
    val paseador = detalle.paseador

    var nombres by remember { mutableStateOf(detalle.nombres ?: paseador?.nombres.orEmpty()) }
    var apellidos by remember { mutableStateOf(detalle.apellidos ?: paseador?.apellidos.orEmpty()) }
    var telefono by remember { mutableStateOf(detalle.telefono ?: paseador?.telefono.orEmpty()) }
    var fotoPerfilUrl by remember { mutableStateOf(detalle.fotoPerfilUrl.orEmpty()) }
    var biografia by remember { mutableStateOf(paseador?.biografia.orEmpty()) }

    val initialTarifa = paseador?.tarifaHoraPen?.let { String.format(Locale.US, "%.2f", it) } ?: "25.00"
    var tarifaHora by remember { mutableStateOf(initialTarifa) }

    val currentDistritoId = paseador?.distritoCoberturaId
    var selectedDistrito by remember(distritos, currentDistritoId) {
        mutableStateOf(
            distritos.find { it.id == currentDistritoId }
                ?: distritos.find { it.descripcion.equals(paseador?.distritoCoberturaDescripcion, ignoreCase = true) }
                ?: distritos.firstOrNull()
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Editar Perfil de Paseador",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Nombres
                OutlinedTextField(
                    value = nombres,
                    onValueChange = { nombres = it },
                    label = { Text("Nombres") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Apellidos
                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    label = { Text("Apellidos") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Teléfono
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Foto de perfil URL
                OutlinedTextField(
                    value = fotoPerfilUrl,
                    onValueChange = { fotoPerfilUrl = it },
                    label = { Text("Foto de Perfil URL (Opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Biografía
                OutlinedTextField(
                    value = biografia,
                    onValueChange = { biografia = it },
                    label = { Text("Biografía") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Tarifa por hora
                OutlinedTextField(
                    value = tarifaHora,
                    onValueChange = { tarifaHora = it },
                    label = { Text("Tarifa por hora (S/)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Distrito de Cobertura (ExposedDropdownMenuBox con Distritos del Backend)
                Text(
                    text = "Distrito de cobertura",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { if (!isLoading && distritos.isNotEmpty()) dropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDistrito?.descripcion ?: if (distritos.isEmpty()) "Cargando distritos..." else "Selecciona un distrito",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = !isLoading && distritos.isNotEmpty()),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && distritos.isNotEmpty()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        distritos.forEach { distrito ->
                            DropdownMenuItem(
                                text = { Text(distrito.descripcion, fontSize = 13.sp) },
                                onClick = {
                                    selectedDistrito = distrito
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tarifaNum = tarifaHora.toDoubleOrNull() ?: 25.0
                    val req = UpdateProfileRequestDto(
                        nombres = nombres.trim().ifBlank { null },
                        apellidos = apellidos.trim().ifBlank { null },
                        telefono = telefono.trim().ifBlank { null },
                        fotoPerfilUrl = fotoPerfilUrl.trim().ifBlank { null },
                        biografia = biografia.trim().ifBlank { null },
                        tarifaHoraPen = tarifaNum,
                        distritoCoberturaId = selectedDistrito?.id
                    )
                    onConfirm(req)
                },
                enabled = !isLoading && nombres.isNotBlank() && apellidos.isNotBlank() && selectedDistrito != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualizarCoberturaDialog(
    distritoActualId: Int?,
    distritos: List<DistritoParamDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedDistrito by remember(distritos, distritoActualId) {
        mutableStateOf(
            distritos.find { it.id == distritoActualId }
                ?: distritos.firstOrNull()
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Actualizar Cobertura Rápida",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Selecciona el distrito principal en el que deseas brindar servicio de paseos:",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { if (!isLoading && distritos.isNotEmpty()) dropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDistrito?.descripcion ?: if (distritos.isEmpty()) "Cargando distritos..." else "Selecciona un distrito",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = !isLoading && distritos.isNotEmpty()),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && distritos.isNotEmpty()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        distritos.forEach { distrito ->
                            DropdownMenuItem(
                                text = { Text(distrito.descripcion, fontSize = 13.sp) },
                                onClick = {
                                    selectedDistrito = distrito
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedDistrito?.let { onConfirm(it.id) }
                },
                enabled = !isLoading && selectedDistrito != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Guardar Cobertura", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}
