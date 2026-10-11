package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class GoogleAuthResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("pictureUrl") val pictureUrl: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("token") val token: String? = null
)