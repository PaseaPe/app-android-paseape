package com.example.paseape.data.model

/**
 * Límite declarado por `StorageValidator` del backend para fotos. El límite
 * efectivo depende de `spring.servlet.multipart.max-file-size`.
 */
const val MAX_FOTO_BYTES: Long = 10L * 1024L * 1024L

/** Formatos de imagen aceptados por el backend para foto de perfil o mascota. */
fun esFotoPermitida(nombre: String, contentType: String?): Boolean {
    val tipo = contentType.orEmpty().lowercase()
    if (tipo.startsWith("image/")) {
        return tipo == "image/jpeg" ||
            tipo == "image/jpg" ||
            tipo == "image/png" ||
            tipo == "image/webp"
    }

    val lower = nombre.lowercase()
    return lower.endsWith(".jpg") ||
        lower.endsWith(".jpeg") ||
        lower.endsWith(".png") ||
        lower.endsWith(".webp")
}

/** Content-Type deducido de la extensión para el multipart de la foto. */
fun contentTypeParaImagen(nombre: String): String = when {
    nombre.endsWith(".png", ignoreCase = true) -> "image/png"
    nombre.endsWith(".webp", ignoreCase = true) -> "image/webp"
    else -> "image/jpeg"
}
