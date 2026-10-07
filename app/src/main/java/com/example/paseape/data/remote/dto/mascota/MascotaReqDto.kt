package com.example.paseape.data.remote.dto.mascota

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MascotaReqDto(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("uuid")
    val uuid: String? = null,
    @SerialName("nombre")
    val nombre: String,
    @SerialName("tipo_mascota_id")
    val tipoMascotaId: Int,
    @SerialName("tipo_raza_id")
    val tipoRazaId: Int,
    @SerialName("tipo_genero_mascota_id")
    val tipoGeneroMascotaId: Int,
    @SerialName("tipo_tamano_mascota_id")
    val tipoTamanoMascotaId: Int,
    @SerialName("tipo_nivel_energia_id")
    val tipoNivelEnergiaId: Int,
    @SerialName("edad_anos")
    val edadAnos: Int? = null,
    @SerialName("edad_meses")
    val edadMeses: Int? = null,
    @SerialName("peso_kg")
    val pesoKg: Double? = null,
    @SerialName("esterilizado")
    val esterilizado: Int = 0, // 0 = No, 1 = Sí
    @SerialName("sociable_con_perros")
    val sociableConPerros: Int = 1,
    @SerialName("sociable_con_personas")
    val sociableConPersonas: Int = 1,
    @SerialName("precauciones_medicas")
    val precaucionesMedicas: String? = null,
    @SerialName("foto_url")
    val fotoUrl: String? = null
)