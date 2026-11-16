package com.example.paseape.ui.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast

/**
 * Descarga un archivo a la carpeta Descargas del dispositivo usando
 * [DownloadManager], sin abrir el navegador.
 *
 * - No requiere permisos de almacenamiento: `minSdk` es 31 y la carpeta pública
 *   de Descargas es accesible directamente desde la app.
 * - La notificación de descarga queda visible hasta terminar, así el usuario
 *   ve el progreso y puede abrir el archivo al finalizar.
 * - Solo acepta URLs `http(s)`. Cualquier otro esquema (enlaces legados) se
 *   abre con el visor del sistema como respaldo para no romper el comportamiento.
 *
 * @param nombreArchivo nombre deseado dentro de Descargas. Si es nulo o vacío
 * se toma el último segmento de la URL.
 */
fun Context.descargarDocumento(url: String, nombreArchivo: String? = null) {
    if (url.isBlank()) {
        Toast.makeText(this, "No hay archivo para descargar.", Toast.LENGTH_SHORT).show()
        return
    }

    val esHttp = url.startsWith("http://", ignoreCase = true) ||
        url.startsWith("https://", ignoreCase = true)

    if (!esHttp) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure {
                Toast.makeText(this, "No se pudo abrir el documento.", Toast.LENGTH_SHORT).show()
            }
        return
    }

    val nombre = nombreDescargado(url, nombreArchivo)
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(nombre)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nombre)
        .setMimeType(mimeTypeDe(nombre))
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val resultado = runCatching {
        getSystemService(DownloadManager::class.java).enqueue(request)
    }

    resultado.onSuccess {
        Toast.makeText(this, "Descargando $nombre en Descargas.", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(this, "No se pudo descargar el documento.", Toast.LENGTH_SHORT).show()
    }
}

/** Nombre válido para el sistema de archivos (sin caracteres prohibidos). */
private fun nombreDescargado(url: String, nombreArchivo: String?): String {
    val base = nombreArchivo?.trim()?.takeIf { it.isNotBlank() }
        ?: Uri.parse(url).lastPathSegment?.substringAfterLast('/')
        ?: "documento"
    val limpio = base.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    return limpio.ifBlank { "documento" }
}

/** Content-Type deducido de la extensión; si no se conoce, octet-stream. */
private fun mimeTypeDe(nombre: String): String {
    val extension = nombre.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: "application/octet-stream"
}
