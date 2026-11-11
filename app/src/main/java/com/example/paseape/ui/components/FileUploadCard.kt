package com.example.paseape.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.ArchivoPendiente
import com.example.paseape.data.model.DigitalDocumentDto
import com.example.paseape.data.model.MAX_ANTECEDENTE_BYTES
import com.example.paseape.data.model.esAntecedentePermitido
import com.example.paseape.data.model.formatearTamanoArchivo
import com.example.paseape.ui.theme.PrimaryDark

private val ErrorRed = Color(0xFFB3261E)
private val HintGray = Color(0xFF64748B)
private val PdfRed = Color(0xFFB3261E)
private val DocBlue = Color(0xFF2563EB)
private val NeutralGray = Color(0xFF475569)
private val ZoneBackground = Color(0xFFF8FAFC)
private val ZoneBorder = Color(0xFFCBD5E1)

private val MIME_ANTECEDENTES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
)

/**
 * Widget de subida de archivos con el lenguaje visual de la app.
 *
 * Se usa en el registro del paseador y en la edición de su perfil. Permite:
 * - elegir varios archivos (PDF/Word) con el selector del sistema,
 * - validar formato y tamaño (1 MB) en el cliente,
 * - listar documentos ya subidos con icono según el tipo, tamaño y acciones,
 * - mostrar el progreso real de la subida y quitar archivos de la lista.
 *
 * @param documentosExistentes documentos que ya están en el backend.
 * @param archivosPendientes archivos elegidos y aún no enviados.
 * @param onSubir si es null no se muestra el botón de subida (el envío lo hace
 *   el botón principal de la pantalla, como en el registro).
 */
@Composable
fun FileUploadCard(
    documentosExistentes: List<DigitalDocumentDto>,
    archivosPendientes: List<ArchivoPendiente>,
    subiendo: Boolean,
    progreso: Int?,
    habilitado: Boolean,
    error: String?,
    onArchivosSeleccionados: (List<ArchivoPendiente>) -> Unit,
    onQuitarPendiente: (ArchivoPendiente) -> Unit,
    onEliminarDocumento: (DigitalDocumentDto) -> Unit,
    onDescargarDocumento: (DigitalDocumentDto) -> Unit,
    modifier: Modifier = Modifier,
    onSubir: (() -> Unit)? = null,
    titulo: String = "Certificado de antecedentes policiales / penales",
    descripcion: String =
        "Adjunta tu certificado en PDF o Word. Máximo 1 MB por archivo."
) {
    val context = LocalContext.current
    var errorSeleccion by remember { mutableStateOf<String?>(null) }

    val selector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        val aceptados = mutableListOf<ArchivoPendiente>()
        val rechazados = mutableListOf<String>()

        uris.forEach { uri ->
            val archivo = leerArchivo(context, uri) ?: return@forEach
            when {
                !esAntecedentePermitido(archivo.nombre) ->
                    rechazados += "${archivo.nombre}: formato no permitido (solo PDF o Word)."

                archivo.tamanoBytes > MAX_ANTECEDENTE_BYTES ->
                    rechazados += "${archivo.nombre}: supera el límite de 1 MB."

                else -> aceptados += archivo
            }
        }

        errorSeleccion = if (rechazados.isEmpty()) null else rechazados.joinToString("\n")
        if (aceptados.isNotEmpty()) {
            onArchivosSeleccionados(aceptados)
        }
    }

    val mensajeError = errorSeleccion ?: error

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titulo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Text(
                text = descripcion,
                fontSize = 12.sp,
                color = HintGray
            )

            // Zona de selección (toca para abrir el selector del sistema)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ZoneBackground)
                    .drawBehind {
                        val colorBorde = if (mensajeError != null) ErrorRed else ZoneBorder
                        drawRoundRect(
                            color = colorBorde,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(8.dp.toPx(), 6.dp.toPx())
                                )
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    }
                    .clickable(enabled = habilitado && !subiendo) {
                        errorSeleccion = null
                        selector.launch(MIME_ANTECEDENTES)
                    }
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = PrimaryDark,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Toca para elegir archivos",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeutralGray
                    )
                    Text(
                        text = "Seleccionar archivo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PDF o Word · máx. 1 MB",
                        fontSize = 11.sp,
                        color = HintGray
                    )
                }
            }

            // Documentos ya subidos
            documentosExistentes.forEach { documento ->
                FilaArchivo(
                    nombre = documento.nombreOriginal
                        ?: documento.objectName
                        ?: "Documento",
                    tamanoBytes = documento.tamanoBytes,
                    contentType = documento.contentType,
                    habilitado = habilitado && !subiendo,
                    onDescargar = { onDescargarDocumento(documento) },
                    onEliminar = { onEliminarDocumento(documento) }
                )
            }

            // Archivos elegidos pendientes de subir
            archivosPendientes.forEach { archivo ->
                FilaArchivo(
                    nombre = archivo.nombre,
                    tamanoBytes = archivo.tamanoBytes,
                    contentType = archivo.contentType,
                    habilitado = habilitado && !subiendo,
                    onEliminar = { onQuitarPendiente(archivo) }
                )
            }

            if (documentosExistentes.isEmpty() && archivosPendientes.isEmpty()) {
                Text(
                    text = "Todavía no hay documentos adjuntos.",
                    fontSize = 12.sp,
                    color = HintGray
                )
            }

            // Barra de progreso real durante la subida
            if (subiendo || progreso != null) {
                val porcentaje = (progreso ?: 0).coerceIn(0, 100)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { porcentaje / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryDark,
                        trackColor = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$porcentaje%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                }
                Text(
                    text = "Subiendo archivos… no cierres la pantalla.",
                    fontSize = 11.sp,
                    color = HintGray
                )
            }

            mensajeError?.let { mensaje ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = mensaje,
                        fontSize = 12.sp,
                        color = ErrorRed
                    )
                }
            }

            if (onSubir != null && archivosPendientes.isNotEmpty()) {
                Button(
                    onClick = onSubir,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = habilitado && !subiendo,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                ) {
                    if (subiendo) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subiendo…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subir archivos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Versión de solo lectura del listado de antecedentes para la vista del perfil.
 * Muestra los documentos ya subidos y permite descargarlos; agregar o eliminar
 * se hace únicamente desde el diálogo de edición de perfil.
 */
@Composable
fun DocumentosSoloLecturaCard(
    documentos: List<DigitalDocumentDto>,
    habilitado: Boolean,
    onDescargarDocumento: (DigitalDocumentDto) -> Unit,
    modifier: Modifier = Modifier,
    titulo: String = "Certificado de antecedentes policiales / penales"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titulo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            if (documentos.isEmpty()) {
                Text(
                    text = "Todavía no hay documentos adjuntos. Agrégalos desde Editar Perfil.",
                    fontSize = 12.sp,
                    color = HintGray
                )
            } else {
                documentos.forEach { documento ->
                    FilaArchivo(
                        nombre = documento.nombreOriginal
                            ?: documento.objectName
                            ?: "Documento",
                        tamanoBytes = documento.tamanoBytes,
                        contentType = documento.contentType,
                        habilitado = habilitado,
                        onDescargar = { onDescargarDocumento(documento) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaArchivo(
    nombre: String,
    tamanoBytes: Long?,
    contentType: String?,
    habilitado: Boolean,
    onEliminar: (() -> Unit)? = null,
    onDescargar: (() -> Unit)? = null
) {
    val estilo = estiloPorTipo(nombre, contentType)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(estilo.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = estilo.icono,
                contentDescription = estilo.etiqueta,
                tint = estilo.color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombre,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${estilo.etiqueta} · ${formatearTamanoArchivo(tamanoBytes)}",
                fontSize = 11.sp,
                color = HintGray
            )
        }

        if (onDescargar != null) {
            IconButton(
                onClick = onDescargar,
                enabled = habilitado,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Descargar",
                    tint = PrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (onEliminar != null) {
            IconButton(
                onClick = onEliminar,
                enabled = habilitado,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class EstiloArchivo(
    val icono: ImageVector,
    val color: Color,
    val etiqueta: String
)

private fun estiloPorTipo(nombre: String, contentType: String?): EstiloArchivo {
    val lowerNombre = nombre.lowercase()
    val lowerTipo = contentType.orEmpty().lowercase()
    return when {
        lowerNombre.endsWith(".pdf") || lowerTipo.contains("pdf") ->
            EstiloArchivo(Icons.Default.PictureAsPdf, PdfRed, "PDF")

        lowerNombre.endsWith(".doc") ||
            lowerNombre.endsWith(".docx") ||
            lowerTipo.contains("word") ||
            lowerTipo.contains("msword") ||
            lowerTipo.contains("wordprocessingml") ->
            EstiloArchivo(Icons.Default.Description, DocBlue, "Word")

        else -> EstiloArchivo(Icons.AutoMirrored.Filled.InsertDriveFile, NeutralGray, "Archivo")
    }
}

private fun leerArchivo(context: Context, uri: Uri): ArchivoPendiente? {
    val resolver = context.contentResolver
    var nombre = uri.lastPathSegment?.substringAfterLast('/') ?: "antecedente"
    var tamano = -1L

    runCatching {
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idxNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val idxTamano = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idxNombre >= 0 && !cursor.isNull(idxNombre)) {
                    nombre = cursor.getString(idxNombre)
                }
                if (idxTamano >= 0 && !cursor.isNull(idxTamano)) {
                    tamano = cursor.getLong(idxTamano)
                }
            }
        }
    }

    if (tamano < 0) {
        tamano = runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                fd.length.takeIf { it >= 0 } ?: 0L
            }
        }.getOrNull() ?: 0L
    }

    val contentType = runCatching { resolver.getType(uri) }.getOrNull()

    return ArchivoPendiente(
        uri = uri,
        nombre = nombre,
        tamanoBytes = tamano,
        contentType = contentType
    )
}
