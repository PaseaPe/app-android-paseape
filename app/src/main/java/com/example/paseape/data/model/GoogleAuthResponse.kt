package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class GoogleAuthResponse(
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("response") val response: AuthResData? = null,
    @SerializedName("id") val id: Long? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("pictureUrl") val pictureUrl: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("token") val token: String? = null
) {
    val effectiveToken: String?
        get() = response?.token ?: token

    val effectiveUuid: String?
        get() = response?.uuid ?: uuid

    val effectiveRole: String?
        get() = response?.role ?: role

    val effectiveUserTypeId: Int?
        get() = response?.userTypeId
}
