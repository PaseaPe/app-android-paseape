package com.example.paseape.data.model

data class ResetPasswordRequest(
    val token: String,
    val nueva_contrasena: String
)