package com.example.paseape.data.model

data class LoginResponse(
    val statusCode: Int,
    val code: String,
    val message: String,
    val response: AuthResData? // Aquí adentro viene el token y los datos del usuario
)

data class AuthResData(
    val id: Long?,
    val uuid: String?,
    val email: String?,
    val fullName: String?,
    val pictureUrl: String?,
    val role: String?,
    val token: String? // ¡El token real vive aquí!
)