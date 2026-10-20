package com.example.paseape.data.model

data class NotificacionDto(
    val id: Long,
    val titulo: String,
    val mensaje: String,
    val leido: Boolean,
    val fechaCreacion: String
)