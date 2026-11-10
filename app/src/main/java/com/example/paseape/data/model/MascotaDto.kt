package com.example.paseape.data.model

data class MascotaDto(
    val id: Long,
    val nombre: String,
    val tipoMascotaId: Int,
    val tipoRazaId: Int,
    val pesoKg: Double
)