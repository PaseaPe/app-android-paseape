package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class PaseadoresResponse(
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("tipo_usuario_id") val tipoUsuarioId: Int,
    @SerializedName("tipo_documento_id") val tipoDocumentoId: Int,
    @SerializedName("numero_documento") val numeroDocumento: String,
    @SerializedName("antecedentes_policiales_url") val antecedentesPolicialesUrl: String?,
    @SerializedName("experiencia_anos") val experienciaAnos: Int,
    @SerializedName("biografia") val biografia: String,
    @SerializedName("tarifa_hora_pen") val tarifaHoraPen: Double,
    @SerializedName("distrito_cobertura_id") val distritoCoberturaId: Int
)