package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class AuthLoginReq(
    @SerializedName("correo")
    val correo: String,
    @SerializedName("contrasena")
    val contrasena: String
)