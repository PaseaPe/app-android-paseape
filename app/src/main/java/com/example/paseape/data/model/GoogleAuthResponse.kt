package com.example.paseape.data.model

data class GoogleAuthResponse(
    val success: Boolean,
    val message: String,
    val token: String?
)