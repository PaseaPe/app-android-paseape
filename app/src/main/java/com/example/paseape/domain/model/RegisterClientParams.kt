package com.example.paseape.domain.model

data class RegisterClientParams(
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val contrasena: String? = null,
    val idTokenGoogle: String? = null,
    val telefono: String,
    val direccionReferencia: String,
    val distritoId: Int,
    val contactoEmergenciaNombre: String? = null,
    val contactoEmergenciaTelefono: String? = null,
    val notasAdicionales: String? = null,
    val mascotas: List<RegisterPetParams> = emptyList()
)

data class RegisterPetParams(
    val nombre: String,
    val tipoMascotaId: Int,
    val tipoRazaId: Int,
    val tipoGeneroMascotaId: Int,
    val tipoTamanoMascotaId: Int,
    val tipoNivelEnergiaId: Int,
    val edadAnos: Int,
    val edadMeses: Int,
    val pesoKg: Double,
    val esterilizado: Boolean
)