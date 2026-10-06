package com.example.paseape.data.model

data class Paseador(
    val idpaseador: Int,
    val idusuario: Int,
    val nombre: String,
    val apellido: String,
    val telefono: String?,
    val distrito: String,
    val tarifa_hora: Double,
    val calificacion: Double,
    val foto_perfil: String?,
    val descripcion: String?,
    val experiencia: String?
)