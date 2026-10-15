package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.ClienteAdminDto
import com.example.paseape.data.model.EstadoVerificacionPaseador
import com.example.paseape.data.model.PaseadorAdminDto
import com.example.paseape.data.model.UsuarioEstado
import com.example.paseape.ui.theme.PrimaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualizarEstadoClienteDialog(
    cliente: ClienteAdminDto,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (UsuarioEstado) -> Unit
) {
    val initialEstado = remember(cliente) {
        UsuarioEstado.fromDescripcion(cliente.estadoUsuario)
            ?: UsuarioEstado.fromId(cliente.usuarioEstadoId)
            ?: UsuarioEstado.ACTIVO
    }

    var selectedEstado by remember(cliente) { mutableStateOf(initialEstado) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Actualizar Estado del Cliente",
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
                Column {
                    Text(
                        text = "Cliente:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${cliente.nombres} ${cliente.apellidos}".trim(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = cliente.correo ?: "",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "Selecciona el nuevo estado:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded && !isUpdating,
                    onExpandedChange = { if (!isUpdating) expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedEstado.descripcion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado del Usuario") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && !isUpdating)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = !isUpdating),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUpdating,
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded && !isUpdating,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        UsuarioEstado.entries.forEach { estado ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = estado.descripcion,
                                        fontSize = 14.sp,
                                        fontWeight = if (estado == selectedEstado) FontWeight.Bold else FontWeight.Normal,
                                        color = if (estado == selectedEstado) PrimaryDark else Color(0xFF1E293B)
                                    )
                                },
                                onClick = {
                                    selectedEstado = estado
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedEstado) },
                enabled = !isUpdating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualizarVerificacionPaseadorDialog(
    paseador: PaseadorAdminDto,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (EstadoVerificacionPaseador) -> Unit
) {
    val initialEstado = remember(paseador) {
        EstadoVerificacionPaseador.fromDescripcion(paseador.estadoVerificacion)
            ?: EstadoVerificacionPaseador.fromId(paseador.estadoVerificacionId)
            ?: EstadoVerificacionPaseador.PENDIENTE
    }

    var selectedEstado by remember(paseador) { mutableStateOf(initialEstado) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Actualizar Verificación del Paseador",
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
                Column {
                    Text(
                        text = "Paseador:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${paseador.nombres} ${paseador.apellidos}".trim(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Distrito: ${paseador.distrito}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "Selecciona el estado de verificación:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded && !isUpdating,
                    onExpandedChange = { if (!isUpdating) expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedEstado.descripcion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado de Verificación") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && !isUpdating)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = !isUpdating),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUpdating,
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded && !isUpdating,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        EstadoVerificacionPaseador.entries.forEach { estado ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = estado.descripcion,
                                        fontSize = 14.sp,
                                        fontWeight = if (estado == selectedEstado) FontWeight.Bold else FontWeight.Normal,
                                        color = if (estado == selectedEstado) PrimaryDark else Color(0xFF1E293B)
                                    )
                                },
                                onClick = {
                                    selectedEstado = estado
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedEstado) },
                enabled = !isUpdating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
