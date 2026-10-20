package com.example.paseape.data.model

data class PaseoResDto(
    val id: Long,
    val mascotaId: Long,
    val mascotaNombre: String,
    val clienteId: Long,
    val paseadorId: Long?,
    val fechaProgramada: String,
    val estadoPaseoId: Int,
    val distritoId: Int,
    val tarifaPen: Double
)