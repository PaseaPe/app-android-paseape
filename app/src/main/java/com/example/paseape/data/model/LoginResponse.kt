package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("response") val response: AuthResData?,
    @SerializedName("uuid") private val rootUuid: String? = null
) {
    val uuid: String?
        get() = rootUuid ?: response?.uuid

    val userUuid: String?
        get() = uuid
}

data class AuthResData(
    @SerializedName("id") val id: Long?,
    @SerializedName("uuid") val uuid: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("pictureUrl") val pictureUrl: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("userTypeId") val userTypeId: Int? = null,
    @SerializedName("token") val token: String? // ¡El token real vive aquí!
)