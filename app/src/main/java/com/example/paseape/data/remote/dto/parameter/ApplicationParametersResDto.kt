package com.example.paseape.data.remote.dto.parameter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApplicationParametersResDto(
    @SerialName("tipos_mascota")
    val tiposMascota: List<ParameterItemDto> = emptyList(),
    @SerialName("distritos")
    val distritos: List<ParameterItemDto> = emptyList(),
    @SerialName("tipos_usuario")
    val tiposUsuario: List<ParameterItemDto> = emptyList(),
    @SerialName("tipos_documento")
    val tiposDocumento: List<ParameterItemDto> = emptyList(),
    @SerialName("generos_mascota")
    val generosMascota: List<ParameterItemDto> = emptyList(),
    @SerialName("tamanos_mascota")
    val tamanosMascota: List<ParameterItemDto> = emptyList(),
    @SerialName("niveles_energia")
    val nivelesEnergia: List<ParameterItemDto> = emptyList()
)
