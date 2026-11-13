package com.example.paseape.data.network

import android.content.ContentResolver
import android.net.Uri
import com.example.paseape.data.model.ArchivoPendiente
import com.example.paseape.data.model.contentTypeParaAntecedente
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody

/**
 * Utilidades para convertir los archivos elegidos con el selector del sistema
 * (SAF) en partes multipart listas para `POST /documento/usuario/{uuid}/antecedentes`.
 *
 * El backend espera un campo repetido llamado [CAMPO_ARCHIVOS] ("files").
 */
object DocumentoUpload {

    const val CAMPO_ARCHIVOS = "files"

    private data class ArchivoLeido(
        val archivo: ArchivoPendiente,
        val bytes: ByteArray
    )

    /**
     * Construye una parte por archivo leyendo su contenido desde el [resolver].
     *
     * @param onProgress recibe el porcentaje global (0..100) considerando el
     *   tamaño total real de todos los archivos.
     * @return lista de partes; si un archivo no se puede leer se omite.
     */
    fun crearPartes(
        resolver: ContentResolver,
        archivos: List<ArchivoPendiente>,
        onProgress: (Int) -> Unit
    ): List<MultipartBody.Part> {
        if (archivos.isEmpty()) return emptyList()

        val leidos = archivos.mapNotNull { archivo ->
            val bytes = leerBytes(resolver, archivo.uri) ?: return@mapNotNull null
            ArchivoLeido(archivo, bytes)
        }
        if (leidos.isEmpty()) return emptyList()

        val totalBytes = leidos.sumOf { it.bytes.size.toLong() }.coerceAtLeast(1L)
        var ultimoPorcentaje = -1

        fun publicarGlobal(parcial: Long) {
            val global = ((parcial * 100) / totalBytes).toInt().coerceIn(0, 100)
            if (global != ultimoPorcentaje) {
                ultimoPorcentaje = global
                onProgress(global)
            }
        }

        var desplazamiento = 0L
        return leidos.map { leido ->
            val tamano = leido.bytes.size.toLong()
            val inicio = desplazamiento
            val tipoDeclarado = leido.archivo.contentType
            val tipoFinal = if (tipoDeclarado.isNullOrBlank() ||
                tipoDeclarado.equals("application/octet-stream", ignoreCase = true)
            ) {
                // Algunos proveedores devuelven un tipo genérico; la extensión es
                // más fiable y ya fue validada (PDF/DOC/DOCX).
                contentTypeParaAntecedente(leido.archivo.nombre)
            } else {
                tipoDeclarado
            }
            val mediaType = tipoFinal.toMediaTypeOrNull()

            val body = ProgressRequestBody(leido.bytes, mediaType) { porcentajeArchivo ->
                publicarGlobal(inicio + (tamano * porcentajeArchivo / 100))
            }

            val nombreSeguro = leido.archivo.nombre.ifBlank { "antecedente" }
            val part = MultipartBody.Part.createFormData(CAMPO_ARCHIVOS, nombreSeguro, body)

            desplazamiento += tamano
            part
        }
    }

    private fun leerBytes(resolver: ContentResolver, uri: Uri): ByteArray? =
        runCatching {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
}
