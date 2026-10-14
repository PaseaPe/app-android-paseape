package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequestDto(
    @SerializedName("nombres") val nombres: String? = null,
    @SerializedName("apellidos") val apellidos: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("foto_perfil_url") val fotoPerfilUrl: String? = null,
    @SerializedName("biografia") val biografia: String? = null,
    @SerializedName("tarifa_hora_pen") val tarifaHoraPen: Double? = null,
    @SerializedName("distrito_cobertura_id") val distritoCoberturaId: Int? = null
)

data class UpdateCoverageRequestDto(
    @SerializedName("usuario_uuid") val usuarioUuid: String,
    @SerializedName("distrito_id") val distritoId: Int
)
