package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class ResponseDto<T>(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("header") val header: Any?,
    @SerializedName("response") val response: T?
)