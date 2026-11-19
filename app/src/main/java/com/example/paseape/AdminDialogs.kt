package com.example.paseape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.paseape.data.model.ClienteAdminDto
import com.example.paseape.data.model.DigitalDocumentDto
import com.example.paseape.data.model.EstadoVerificacionPaseador
import com.example.paseape.data.model.PaseadorAdminDto
import com.example.paseape.data.model.UsuarioEstado
import com.example.paseape.data.model.formatearTamanoArchivo
import com.example.paseape.ui.components.descargarDocumento
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

/** Par etiqueta/valor de la ficha ampliada del paseador. */
@Composable
private fun DatoPerfil(etiqueta: String, valor: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            fontSize = 11.sp,
            color = Color.Gray
        )
        Text(
            text = valor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}

/** Fila de un documento subido por el paseador, con su botón de descarga. */
@Composable
private fun FilaDocumento(
    documento: DigitalDocumentDto,
    onDescargar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = PrimaryDark,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = documento.tipoDocumentoDescripcion ?: "Documento",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "${documento.nombreOriginal ?: "Archivo sin nombre"} · " +
                        formatearTamanoArchivo(documento.tamanoBytes),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
            TextButton(onClick = onDescargar) {
                Text(
                    text = "Descargar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }
        }
    }
}

/** Fila del certificado legado (`antecedentes_policiales_url`) con su botón de descarga. */
@Composable
private fun FilaAntecedenteLegado(
    url: String,
    onDescargar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = PrimaryDark,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Certificado registrado previamente",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = url,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onDescargar) {
                Text(
                    text = "Descargar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualizarVerificacionPaseadorDialog(
    paseador: PaseadorAdminDto,
    documentos: List<DigitalDocumentDto>,
    cargandoDocumentos: Boolean,
    documentosError: String?,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (EstadoVerificacionPaseador) -> Unit,
    onDescargarDocumento: (DigitalDocumentDto) -> Unit
) {
    val initialEstado = remember(paseador) {
        EstadoVerificacionPaseador.fromDescripcion(paseador.estadoVerificacion)
            ?: EstadoVerificacionPaseador.fromId(paseador.estadoVerificacionId)
            ?: EstadoVerificacionPaseador.PENDIENTE
    }

    var selectedEstado by remember(paseador) { mutableStateOf(initialEstado) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera con foto de perfil y datos de contacto.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val foto = paseador.fotoPerfilUrl?.takeIf { it.isNotBlank() }
                    if (foto != null) {
                        AsyncImage(
                            model = foto,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column {
                        Text(
                            text = "Paseador:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${paseador.nombres} ${paseador.apellidos}".trim(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Distrito: ${paseador.distrito}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Ficha ampliada del perfil.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DatoPerfil("Correo", paseador.correo ?: "Sin correo registrado")
                    DatoPerfil("Teléfono", paseador.telefono ?: "Sin teléfono registrado")
                    DatoPerfil(
                        etiqueta = "Documento de identidad",
                        valor = buildString {
                            append(paseador.tipoDocumentoDescripcion ?: "Documento")
                            paseador.numeroDocumento?.takeIf { it.isNotBlank() }?.let {
                                append(" $it")
                            }
                        }
                    )
                    DatoPerfil(
                        etiqueta = "Experiencia",
                        valor = paseador.experienciaAnos?.let { "$it años" }
                            ?: "Sin registrar"
                    )
                    DatoPerfil(
                        etiqueta = "Tarifa por hora",
                        valor = paseador.tarifaHoraPen?.let {
                            String.format(java.util.Locale.US, "S/ %.2f PEN", it)
                        } ?: "Sin registrar"
                    )
                    DatoPerfil(
                        etiqueta = "Paseos completados",
                        valor = "${paseador.paseosCompletados ?: 0}"
                    )
                    DatoPerfil(
                        etiqueta = "Calificación promedio",
                        valor = String.format(
                            java.util.Locale.US,
                            "%.1f ⭐",
                            paseador.calificacionPromedio ?: 0.0
                        )
                    )

                    val biografia = paseador.biografia?.takeIf { it.isNotBlank() }
                    if (biografia != null) {
                        Column {
                            Text(
                                text = "Biografía",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = biografia,
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Documentos subidos por el paseador (antecedentes, DNI, etc.).
                Text(
                    text = "Documentos",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )

                when {
                    cargandoDocumentos -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cargando documentos...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    documentosError != null -> Text(
                        text = "No se pudieron cargar los documentos. $documentosError",
                        fontSize = 12.sp,
                        color = Color(0xFFB91C1C)
                    )

                    documentos.isEmpty() -> Text(
                        text = "Sin documentos digitales registrados.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    else -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        documentos.forEach { documento ->
                            FilaDocumento(documento = documento) {
                                onDescargarDocumento(documento)
                            }
                        }
                    }
                }

                // Indicador legado: solo se muestra como respaldo cuando la lista
                // de documentos viene vacía o no pudo cargarse.
                if (documentos.isEmpty()) {
                    val antecedentes = paseador.antecedentesPolicialesUrl
                        ?.takeIf { it.isNotBlank() }
                    if (antecedentes != null) {
                        FilaAntecedenteLegado(
                            url = antecedentes,
                            onDescargar = { context.descargarDocumento(antecedentes) }
                        )
                    } else {
                        Text(
                            text = "⚠ Sin antecedentes policiales/penales adjuntos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706)
                        )
                    }
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
