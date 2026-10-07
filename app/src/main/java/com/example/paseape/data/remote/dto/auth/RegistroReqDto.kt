package com.example.paseape.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegistroReqDto(
    @SerialName("nombres")
    val nombres: String,
    @SerialName("apellidos")
    val apellidos: String,
    @SerialName("correo")
    val correo: String,
    @SerialName("contrasena")
    val contrasena: String,
    @SerialName("tipo_usuario_id")
    val tipoUsuarioId: Int
)
