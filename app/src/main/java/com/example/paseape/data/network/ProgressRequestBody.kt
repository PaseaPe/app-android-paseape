package com.example.paseape.data.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink

/**
 * RequestBody que informa el porcentaje de avance mientras OkHttp escribe el
 * archivo. Permite mostrar una barra de progreso real (no indeterminada) en la
 * subida de antecedentes.
 *
 * El callback se ejecuta en el hilo de red de OkHttp; quien lo consuma debe
 * publicar el valor de forma segura (por ejemplo con un StateFlow).
 */
class ProgressRequestBody(
    private val bytes: ByteArray,
    private val contentType: MediaType?,
    private val onProgress: (Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = bytes.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val total = bytes.size
        if (total == 0) {
            onProgress(100)
            return
        }

        val chunkSize = 8 * 1024
        var written = 0
        var lastPercent = -1

        while (written < total) {
            val count = minOf(chunkSize, total - written)
            sink.write(bytes, written, count)
            written += count

            val percent = ((written * 100) / total).coerceIn(0, 100)
            if (percent != lastPercent) {
                lastPercent = percent
                onProgress(percent)
            }
        }

        sink.flush()
    }
}
