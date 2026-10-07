package com.example.paseape.domain.model

data class User(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val rol: String,
    val token: String? = null,
    val fotoPerfil: String? = null
)
