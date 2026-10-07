package com.example.paseape.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResDto(
    @SerialName("token")
    val token: String? = null,
    @SerialName("id_usuario")
    val idUsuario: Int? = null,
    @SerialName("nombre")
    val nombre: String? = null,
    @SerialName("apellido")
    val apellido: String? = null,
    @SerialName("correo")
    val correo: String? = null,
    @SerialName("tipo_usuario_id")
    val tipoUsuarioId: Int? = null,
    @SerialName("rol")
    val rol: String? = null,
    @SerialName("foto_perfil")
    val fotoPerfil: String? = null
)
