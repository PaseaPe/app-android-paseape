package com.example.paseape.domain.model

data class RegisterWalkerParams(
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val contrasena: String? = null,
    val idTokenGoogle: String? = null,
    val telefono: String,
    val tipoDocumentoId: Int,
    val numeroDocumento: String,
    val antecedentesPolicialesUrl: String? = null,
    val experienciaAnos: Int,
    val biografia: String,
    val tarifaHoraPen: Double,
    val distritoCoberturaId: Int
)