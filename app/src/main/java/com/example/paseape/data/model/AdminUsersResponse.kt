package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class PaseadorAdminDto(
    @SerializedName("id") val id: Long,
    @SerializedName("uuid") val uuid: String?,
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("distrito_cobertura_descripcion") val distrito: String,
    @SerializedName("estado_verificacion_descripcion") val estadoVerificacion: String,
    @SerializedName("estado") val estado: Int
)

data class ClienteAdminDto(
    @SerializedName("id") val id: Long,
    @SerializedName("uuid") val uuid: String?, // <-- Agregamos esto
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("usuario_estado_descripcion") val estadoUsuario: String,
    @SerializedName("estado") val estado: Int
)