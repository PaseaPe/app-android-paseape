package com.example.paseape.data.remote.dto.paseador

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoberturaReqDto(
    @SerialName("paseador_id")
    val paseadorId: Int,
    @SerialName("distrito_cobertura_id")
    val distritoCoberturaId: Int
)
