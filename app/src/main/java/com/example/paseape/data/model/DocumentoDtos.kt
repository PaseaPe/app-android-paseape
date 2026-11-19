package com.example.paseape.data.model

import android.net.Uri
import com.google.gson.annotations.SerializedName

/**
 * Documento digital almacenado en OCI. Espejo de `DigitalDocumentResDto` del backend.
 */
data class DigitalDocumentDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("tipo_documento_id") val tipoDocumentoId: Int? = null,
    @SerializedName("tipo_documento_descripcion") val tipoDocumentoDescripcion: String? = null,
    @SerializedName("object_name") val objectName: String? = null,
    @SerializedName("nombre_original") val nombreOriginal: String? = null,
    @SerializedName("content_type") val contentType: String? = null,
    @SerializedName("tamano_bytes") val tamanoBytes: Long? = null
)

/**
 * Respuesta de `GET /documento/{uuid}/descarga`: URL prefirmada que caduca a los
 * `expiration_minutes` minutos (~30) y se puede abrir sin cabecera de autorización.
 */
data class DocumentUrlDto(
    @SerializedName("url") val url: String? = null,
    @SerializedName("nombre_original") val nombreOriginal: String? = null,
    @SerializedName("content_type") val contentType: String? = null,
    @SerializedName("tamano_bytes") val tamanoBytes: Long? = null,
    @SerializedName("expiration_minutes") val expirationMinutes: Int? = null
)

/**
 * Archivo elegido por el usuario en el selector del sistema y todavía no subido.
 * Se guarda la [uri] para poder leer el contenido al momento de enviarlo.
 */
data class ArchivoPendiente(
    val uri: Uri,
    val nombre: String,
    val tamanoBytes: Long,
    val contentType: String? = null
)

/** Límite validado en el cliente. El backend hoy corta en 1 MB (pendiente D2). */
const val MAX_ANTECEDENTE_BYTES: Long = 1024L * 1024L

/** Para antecedentes policiales solo se permite PDF y Word (.doc / .docx). */
fun esAntecedentePermitido(nombre: String): Boolean {
    val lower = nombre.lowercase()
    return lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx")
}

/** Content-Type deducido de la extensión (respaldo cuando el proveedor no lo informa). */
fun contentTypeParaAntecedente(nombre: String): String = when {
    nombre.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
    nombre.endsWith(".doc", ignoreCase = true) -> "application/msword"
    nombre.endsWith(".docx", ignoreCase = true) ->
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    else -> "application/octet-stream"
}

/** Texto legible para el tamaño del archivo (B / KB / MB). */
fun formatearTamanoArchivo(bytes: Long?): String {
    val valor = bytes ?: return "Tamaño desconocido"
    return when {
        valor < 1024L -> "$valor B"
        valor < 1024L * 1024L -> String.format(java.util.Locale.US, "%.0f KB", valor / 1024.0)
        else -> String.format(java.util.Locale.US, "%.2f MB", valor / (1024.0 * 1024.0))
    }
}

/**
 * IDs de `digital_document_type`, espejo del seed del backend
 * (`V2.1.0__create_digital_document_schema.sql`).
 */
object TipoDocumentoDigital {
    const val FOTO_PERFIL = 1
    const val FOTO_MASCOTA = 2
    const val ANTECEDENTE_POLICIAL = 3
    const val DOCUMENTO_IDENTIDAD = 4
}

private val DESCRIPCIONES_FOTO = setOf("FOTO_PERFIL", "FOTO_MASCOTA")

/**
 * Deja solo los archivos adjuntos descargables (antecedentes, documento de
 * identidad), excluyendo las fotos de perfil y de mascota.
 *
 * El backend devuelve todos los archivos de la carpeta del usuario en
 * `digital_documents`, así que sin este filtro la foto de perfil aparecería
 * como un documento más y se descargaría en lugar del PDF/Word.
 */
fun List<DigitalDocumentDto>.soloDocumentosAdjuntos(): List<DigitalDocumentDto> =
    filterNot { documento ->
        documento.tipoDocumentoId == TipoDocumentoDigital.FOTO_PERFIL ||
            documento.tipoDocumentoId == TipoDocumentoDigital.FOTO_MASCOTA ||
            documento.tipoDocumentoDescripcion
                ?.trim()
                ?.uppercase()
                ?.let { it in DESCRIPCIONES_FOTO } == true
    }
