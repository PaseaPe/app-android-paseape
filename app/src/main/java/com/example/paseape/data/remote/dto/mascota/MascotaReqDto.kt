package com.example.paseape.data.remote.dto.mascota

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MascotaReqDto(
    @SerialName("cliente_id")
    val clienteId: Int,
    @SerialName("nombre")
    val nombre: String,
    @SerialName("tipo_mascota_id")
    val tipoMascotaId: Int,
    @SerialName("tipo_raza_id")
    val tipoRazaId: Int,
    @SerialName("peso_kg")
    val pesoKg: Double,
    @SerialName("esterilizado")
    val esterilizado: Int
)
