package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class GoogleAuthReq(
    @SerializedName("id_token")
    val idToken: String
)