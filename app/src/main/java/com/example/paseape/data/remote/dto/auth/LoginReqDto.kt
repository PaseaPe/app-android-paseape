package com.example.paseape.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginReqDto(
    @SerialName("correo")
    val correo: String,
    @SerialName("contrasena")
    val contrasena: String
)
