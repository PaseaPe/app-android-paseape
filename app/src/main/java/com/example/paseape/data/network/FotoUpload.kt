package com.example.paseape.data.network

import android.content.ContentResolver
import com.example.paseape.data.model.ArchivoPendiente
import com.example.paseape.data.model.contentTypeParaImagen
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody

/**
 * Utilidades para convertir la foto elegida con el selector del sistema en una
 * parte multipart lista para `POST cliente/foto`, `POST paseadores/foto` o
 * `POST mascotas/{uuid}/foto`.
 *
 * El backend espera un único campo llamado [CAMPO_FOTO] ("file").
 */
object FotoUpload {

    const val CAMPO_FOTO = "file"

    /**
     * Construye la parte multipart leyendo el contenido desde el [resolver].
     *
     * @param onProgress recibe el porcentaje real de avance (0..100).
     * @return la parte lista para enviar, o `null` si no se pudo leer el archivo.
     */
    fun crearParte(
        resolver: ContentResolver,
        archivo: ArchivoPendiente,
        onProgress: (Int) -> Unit
    ): MultipartBody.Part? {
        val bytes = runCatching {
            resolver.openInputStream(archivo.uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null

        if (bytes.isEmpty()) return null

        val tipoDeclarado = archivo.contentType
        val tipoFinal = if (tipoDeclarado.isNullOrBlank() ||
            tipoDeclarado.equals("application/octet-stream", ignoreCase = true) ||
            !tipoDeclarado.startsWith("image/")
        ) {
            contentTypeParaImagen(archivo.nombre)
        } else {
            tipoDeclarado
        }

        val body = ProgressRequestBody(
            bytes = bytes,
            contentType = normalizarContentType(tipoFinal).toMediaTypeOrNull(),
            onProgress = onProgress
        )
        val nombreSeguro = archivo.nombre.ifBlank { "foto" }
        return MultipartBody.Part.createFormData(CAMPO_FOTO, nombreSeguro, body)
    }

    /**
     * El backend solo acepta `image/jpeg`, `image/png`, `image/webp` y
     * `application/pdf`. Algunos proveedores devuelven variantes como
     * `image/jpg` o `image/x-png`, así que se normalizan antes de enviar.
     */
    private fun normalizarContentType(tipo: String): String = when (tipo.lowercase()) {
        "image/jpg", "image/pjpeg", "image/jpe" -> "image/jpeg"
        "image/x-png" -> "image/png"
        else -> tipo
    }
}
