package com.example.paseape.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.paseape.data.model.ArchivoPendiente

/**
 * Lee nombre, tamaño y tipo de un archivo elegido con el selector del sistema.
 * Se usa tanto para antecedentes como para fotos de perfil/mascota.
 */
internal fun leerArchivoSeleccionado(context: Context, uri: Uri): ArchivoPendiente {
    val resolver = context.contentResolver
    var nombre = uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
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
