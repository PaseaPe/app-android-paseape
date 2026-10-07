package com.example.paseape.domain.model

data class Paseador(
    val idPaseador: Int,
    val idUsuario: Int,
    val nombre: String,
    val apellido: String,
    val telefono: String?,
    val distrito: String,
    val tarifaHora: Double,
    val calificacion: Double,
    val fotoPerfil: String?,
    val descripcion: String?,
    val experiencia: String?
)
