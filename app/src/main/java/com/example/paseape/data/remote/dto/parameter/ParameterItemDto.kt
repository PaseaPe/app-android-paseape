package com.example.paseape.data.remote.dto.parameter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParameterItemDto(
    @SerialName("id")
    val id: Int,
    @SerialName("descripcion")
    val descripcion: String
)
