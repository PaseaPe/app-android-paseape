package com.example.paseape.data.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val usuario: Usuario? = null
)

data class Usuario(
    val idusuario: Int,
    val nombre: String,
    val apellido: String,
    val email: String,
    val rol: String,
    val foto_perfil: String?
)