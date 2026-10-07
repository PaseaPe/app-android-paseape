package com.example.paseape.data.model

data class RegistroRequest(
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val contrasena: String,
    val tipo_usuario_id: Int
)