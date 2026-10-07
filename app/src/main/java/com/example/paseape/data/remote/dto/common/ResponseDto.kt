package com.example.paseape.data.remote.dto.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResponseDto<T>(
    @SerialName("status_code")
    val statusCode: Int? = null,
    @SerialName("code")
    val code: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("header")
    val header: HeaderDto? = null,
    @SerialName("response")
    val response: T? = null
)
