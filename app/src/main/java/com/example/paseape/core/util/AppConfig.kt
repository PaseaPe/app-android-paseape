package com.example.paseape.core.util

import com.example.paseape.BuildConfig

/**
 * Configuración global y fuertemente tipada de la aplicación,
 * respaldada por las variables de entorno inyectadas en BuildConfig vía Gradle.
 */
object AppConfig {
    /**
     * URL base de la API backend (Local 10.0.2.2 / Red Local / OCI Producción).
     */
    val baseUrl: String = BuildConfig.BASE_URL

    /**
     * ID de cliente web para integración con Google Sign-In y Credential Manager.
     */
    val googleWebClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    /**
     * Indica si la aplicación está ejecutándose en modo depuración.
     */
    val isDebug: Boolean = BuildConfig.DEBUG
}
