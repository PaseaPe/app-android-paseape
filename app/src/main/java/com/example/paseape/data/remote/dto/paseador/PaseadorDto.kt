package com.example.paseape.data.remote.dto.paseador

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaseadorDto(
    @SerialName("idpaseador")
    val idPaseador: Int? = null,
    @SerialName("idusuario")
    val idUsuario: Int? = null,
    @SerialName("nombre")
    val nombre: String = "",
    @SerialName("apellido")
    val apellido: String = "",
    @SerialName("telefono")
    val telefono: String? = null,
    @SerialName("distrito")
    val distrito: String = "",
    @SerialName("tarifa_hora")
    val tarifaHora: Double = 0.0,
    @SerialName("calificacion")
    val calificacion: Double = 0.0,
    @SerialName("foto_perfil")
    val fotoPerfil: String? = null,
    @SerialName("descripcion")
    val descripcion: String? = null,
    @SerialName("experiencia")
    val experiencia: String? = null
)
