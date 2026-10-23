package com.example.paseape.data.network

import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Contexto de la operación para elegir un mensaje de respaldo adecuado. */
enum class ApiErrorContext {
    LOGIN,
    REGISTER,
    PASSWORD_RESET,
    PROFILE,
    PET,
    WALKER,
    WALK,
    NOTIFICATION,
    ADMIN,
    GENERIC
}

/**
 * Traduce los errores de la API a mensajes simples para el usuario.
 *
 * Nunca expone JSON, códigos HTTP ni detalles técnicos. Los detalles completos
 * se siguen registrando con Log en las pantallas que los necesiten.
 */
object ApiErrorMapper {

    private const val MAX_MESSAGE_LENGTH = 160

    private const val INVALID_CREDENTIALS = "Correo o contraseña incorrectos."
    private const val SESSION_EXPIRED = "Tu sesión expiró. Inicia sesión nuevamente."
    private const val ACCOUNT_BLOCKED = "Tu cuenta está bloqueada o inactiva. Comunícate con soporte."
    private const val ACCOUNT_NO_ACCESS = "Tu cuenta no tiene acceso. Comunícate con soporte."
    private const val USE_GOOGLE = "Esta cuenta usa Google. Inicia sesión con Google."
    private const val NO_PERMISSION = "No tienes permisos para realizar esta acción."
    private const val NOT_FOUND = "No encontramos la información solicitada."
    private const val CONFLICT = "Ya existe un registro con esos datos."
    private const val TOO_MANY_ATTEMPTS = "Demasiados intentos. Espera un momento e inténtalo de nuevo."
    private const val SERVER_ERROR = "Ocurrió un error en el servidor. Inténtalo más tarde."
    private const val NO_CONNECTION =
        "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo."

    private const val DUPLICATE_EMAIL = "Ya existe una cuenta con ese correo."
    private const val DUPLICATE_DOCUMENT = "Ese número de documento ya está registrado."
    private const val DNI_NOT_FOUND =
        "No encontramos ese DNI en RENIEC. Verifícalo e inténtalo de nuevo."

    /** Para endpoints que devuelven Response<T>. */
    fun fromResponse(
        response: Response<*>,
        context: ApiErrorContext = ApiErrorContext.GENERIC
    ): String {
        val rawBody = runCatching { response.errorBody()?.string() }.getOrNull()
        return resolve(response.code(), rawBody, context)
    }

    /** Para endpoints que lanzan HttpException o fallan por red. */
    fun fromThrowable(
        error: Throwable,
        context: ApiErrorContext = ApiErrorContext.GENERIC
    ): String {
        return when (error) {
            is HttpException -> {
                val rawBody = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
                resolve(error.code(), rawBody, context)
            }

            is UnknownHostException,
            is ConnectException,
            is SocketTimeoutException,
            is IOException -> NO_CONNECTION

            else -> fallback(context)
        }
    }

    private fun resolve(code: Int, rawBody: String?, context: ApiErrorContext): String {
        val backendMessage = extractMessage(rawBody)

        return when {
            code == 401 -> if (context == ApiErrorContext.LOGIN) {
                validationMessage(backendMessage, context)
            } else {
                SESSION_EXPIRED
            }

            code == 400 || code == 422 -> validationMessage(backendMessage, context)
            code == 403 -> if (context == ApiErrorContext.LOGIN) {
                ACCOUNT_NO_ACCESS
            } else {
                NO_PERMISSION
            }
            code == 404 -> NOT_FOUND
            code == 409 -> CONFLICT
            code == 429 -> TOO_MANY_ATTEMPTS
            code in 500..599 -> SERVER_ERROR
            else -> backendMessage?.let { normalizeKnownMessage(it) ?: it } ?: fallback(context)
        }
    }

    private fun validationMessage(message: String?, context: ApiErrorContext): String {
        if (context == ApiErrorContext.LOGIN) {
            if (message == null) return INVALID_CREDENTIALS
            val lower = message.lowercase()
            return when {
                lower.contains("bloqueada") || lower.contains("inactiva") -> ACCOUNT_BLOCKED
                lower.contains("google") -> USE_GOOGLE
                else -> INVALID_CREDENTIALS
            }
        }

        if (message == null) return fallback(context)
        return normalizeKnownMessage(message) ?: message
    }

    private fun normalizeKnownMessage(message: String): String? {
        val lower = message.lowercase()

        val correoDuplicado = lower.contains("correo") &&
            (lower.contains("ya se encuentra registrado") ||
                lower.contains("ya está registrado") ||
                lower.contains("ya está en uso") ||
                lower.contains("ya existe una cuenta"))

        val documentoDuplicado =
            (lower.contains("documento") || lower.contains("dni")) &&
                (lower.contains("ya está en uso") ||
                    lower.contains("ya se encuentra registrado") ||
                    lower.contains("ya está registrado"))

        val dniNoEncontrado = lower.contains("dni") &&
            (lower.contains("reniec") ||
                lower.contains("no fue encontrado") ||
                lower.contains("no es valido") ||
                lower.contains("no es válido"))

        return when {
            correoDuplicado -> DUPLICATE_EMAIL
            documentoDuplicado -> DUPLICATE_DOCUMENT
            dniNoEncontrado -> DNI_NOT_FOUND
            else -> null
        }
    }

    private fun extractMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null

        val message = runCatching {
            Gson().fromJson(rawBody, ErrorBody::class.java)?.message
        }.getOrNull()

        return sanitize(message)
    }

    private fun sanitize(message: String?): String? {
        val text = message
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?: return null

        if (text.isBlank() || text.length > MAX_MESSAGE_LENGTH) return null

        val lower = text.lowercase()
        if (lower.startsWith("error interno") ||
            lower.contains("exception") ||
            lower.contains("java.") ||
            lower.contains("stacktrace") ||
            lower.contains("http ") ||
            text.startsWith("{")
        ) {
            return null
        }

        return text
    }

    private fun fallback(context: ApiErrorContext): String = when (context) {
        ApiErrorContext.LOGIN -> "No pudimos iniciar sesión. Inténtalo de nuevo."
        ApiErrorContext.REGISTER -> "No pudimos completar el registro. Inténtalo de nuevo."
        ApiErrorContext.PASSWORD_RESET -> "No pudimos procesar la solicitud. Inténtalo de nuevo."
        ApiErrorContext.PROFILE -> "No pudimos completar la operación en tu perfil. Inténtalo de nuevo."
        ApiErrorContext.PET -> "No pudimos completar la operación con tu mascota. Inténtalo de nuevo."
        ApiErrorContext.WALKER -> "No pudimos cargar la información del paseador. Inténtalo de nuevo."
        ApiErrorContext.WALK -> "No pudimos cargar los paseos. Inténtalo de nuevo."
        ApiErrorContext.NOTIFICATION -> "No pudimos cargar las notificaciones. Inténtalo de nuevo."
        ApiErrorContext.ADMIN -> "No pudimos completar la operación. Inténtalo de nuevo."
        ApiErrorContext.GENERIC -> "Ocurrió un problema. Inténtalo de nuevo."
    }

    /** Forma común de los errores del backend: ResponseDto y ApiResponse. */
    private class ErrorBody(val message: String?)
}
