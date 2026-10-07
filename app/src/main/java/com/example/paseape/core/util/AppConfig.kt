package com.example.paseape.core.util

import com.example.paseape.BuildConfig

object AppConfig {
    /**
     * URL base del backend REST api-paseape inyectada en tiempo de compilación.
     */
    val baseUrl: String
        get() = BuildConfig.BASE_URL

    /**
     * Web Client ID para la autenticación de Google con Credential Manager.
     */
    val googleWebClientId: String
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID
}

object AppConstants {
    // Roles del sistema
    const val ROLE_CLIENTE = "CLIENTE"
    const val ROLE_PASEADOR = "PASEADOR"

    // IDs de tipo de usuario
    const val TIPO_USUARIO_CLIENTE_ID = 1
    const val TIPO_USUARIO_PASEADOR_ID = 2

    // Códigos de respuesta estándar del backend
    const val CODE_SUCCESS = "00"
    const val CODE_VALIDATION_ERROR = "01"
    const val CODE_UNAUTHORIZED = "E01"

    // Llaves DataStore
    const val PREFERENCES_DATASTORE_NAME = "paseape_user_session"
    const val PREFERENCES_NAME = PREFERENCES_DATASTORE_NAME
    const val KEY_AUTH_TOKEN = "auth_jwt_token"
    const val KEY_USER_UUID = "user_uuid"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_USER_EMAIL = "user_email"

    // Endpoints públicos que omiten el AuthInterceptor
    val PUBLIC_ENDPOINTS = listOf(
        "auth/login",
        "auth/google",
        "auth/register/local",
        "auth/register/google",
        "auth/forgot-password",
        "auth/reset-password",
        "application-parameters"
    )
}
