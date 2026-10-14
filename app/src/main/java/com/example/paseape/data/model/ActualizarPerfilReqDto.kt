package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class ActualizarPerfilReqDto(
    @SerializedName("nombres") val nombres: String? = null,
    @SerializedName("apellidos") val apellidos: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("foto_perfil_url") val fotoPerfilUrl: String? = null,
    @SerializedName("direccion_referencia") val direccionReferencia: String? = null,
    @SerializedName("distrito_id") val distritoId: Int? = null,
    @SerializedName("contacto_emergencia_nombre") val contactoEmergenciaNombre: String? = null,
    @SerializedName("contacto_emergencia_telefono") val contactoEmergenciaTelefono: String? = null,
    @SerializedName("notas_adicionales") val notasAdicionales: String? = null,
    @SerializedName("biografia") val biografia: String? = null,
    @SerializedName("tarifa_hora_pen") val tarifaHoraPen: Double? = null,
    @SerializedName("distrito_cobertura_id") val distritoCoberturaId: Int? = null
)
