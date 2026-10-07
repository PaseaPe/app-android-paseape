package com.example.paseape.data.remote.dto.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeaderDto(
    @SerialName("token")
    val token: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("timestamp")
    val timestamp: String? = null
)
