package com.example.paseape.data.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?, // Token JWT para autorización
    val usuario: UsuarioLogin?
)

data class UsuarioLogin(
    val id: Int,
    val uuid: String,
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val tipo_usuario_id: Int,
    val foto_perfil_url: String?
)