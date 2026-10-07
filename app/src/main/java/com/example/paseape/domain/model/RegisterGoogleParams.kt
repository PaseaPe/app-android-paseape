package com.example.paseape.domain.model

data class RegisterGoogleClientParams(
    val idToken: String,
    val telefono: String,
    val direccionReferencia: String,
    val distritoId: Int,
    val contactoEmergenciaNombre: String? = null,
    val contactoEmergenciaTelefono: String? = null,
    val notasAdicionales: String? = null,
    val mascotas: List<RegisterPetParams> = emptyList()
)

data class RegisterGoogleWalkerParams(
    val idToken: String,
    val telefono: String,
    val tipoDocumentoId: Int,
    val numeroDocumento: String,
    val antecedentesPolicialesUrl: String? = null,
    val experienciaAnos: Int,
    val biografia: String,
    val tarifaHoraPen: Double,
    val distritoCoberturaId: Int
)