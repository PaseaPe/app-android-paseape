package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

enum class UsuarioEstado(val id: Int, val descripcion: String) {
    ACTIVO(1, "ACTIVO"),
    INACTIVO(2, "INACTIVO"),
    PENDIENTE_ACTIVACION(3, "PENDIENTE_ACTIVACION"),
    SUSPENDIDO(4, "SUSPENDIDO"),
    BLOQUEADO(5, "BLOQUEADO");

    companion object {
        fun fromId(id: Int?): UsuarioEstado? = entries.find { it.id == id }
        fun fromDescripcion(desc: String?): UsuarioEstado? =
            entries.find { it.descripcion.equals(desc, ignoreCase = true) }
    }
}

enum class EstadoVerificacionPaseador(val id: Int, val descripcion: String) {
    PENDIENTE(1, "PENDIENTE"),
    EN_REVISION(2, "EN_REVISION"),
    APROBADO(3, "APROBADO"),
    RECHAZADO(4, "RECHAZADO"),
    OBSERVADO(5, "OBSERVADO");

    companion object {
        fun fromId(id: Int?): EstadoVerificacionPaseador? = entries.find { it.id == id }
        fun fromDescripcion(desc: String?): EstadoVerificacionPaseador? =
            entries.find { it.descripcion.equals(desc, ignoreCase = true) }
    }
}

data class ActualizarVerificacionPaseadorRequest(
    @SerializedName("estado_verificacion_id")
    val estadoVerificacionId: Int
)

data class GenericApiResponse(
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("response") val response: Any? = null
)

data class PaseadorAdminDto(
    @SerializedName("id") val id: Long,
    @SerializedName("uuid") val uuid: String?,
    @SerializedName("paseador_uuid") val paseadorUuid: String? = null,
    @SerializedName("usuario_uuid") val usuarioUuid: String? = null,
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("distrito_cobertura_descripcion") val distrito: String,
    @SerializedName("estado_verificacion_id") val estadoVerificacionId: Int? = null,
    @SerializedName("estado_verificacion_descripcion") val estadoVerificacion: String,
    @SerializedName("estado") val estado: Int
) {
    val paseadorIdentifier: String?
        get() = paseadorUuid?.takeIf { it.isNotBlank() } ?: uuid
}

data class ClienteAdminDto(
    @SerializedName("id") val id: Long,
    @SerializedName("uuid") val uuid: String?,
    @SerializedName("usuario_uuid") val usuarioUuid: String? = null,
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("usuario_estado_id") val usuarioEstadoId: Int? = null,
    @SerializedName("usuario_estado_descripcion") val estadoUsuario: String,
    @SerializedName("estado") val estado: Int
) {
    val clienteIdentifier: String?
        get() = usuarioUuid?.takeIf { it.isNotBlank() } ?: uuid
}