package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class MascotaResponse(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo_mascota_id") val tipoMascotaId: Int,
    @SerializedName("tipo_raza_id") val tipoRazaId: Int,
    @SerializedName("tipo_genero_mascota_id") val tipoGeneroMascotaId: Int,
    @SerializedName("tipo_tamano_mascota_id") val tipoTamanoMascotaId: Int,
    @SerializedName("tipo_nivel_energia_id") val tipoNivelEnergiaId: Int,
    @SerializedName("edad_anos") val edadAnos: Int,
    @SerializedName("edad_meses") val edadMeses: Int,
    @SerializedName("peso_kg") val pesoKg: Double,
    @SerializedName("esterilizado") val esterilizado: Int,
    @SerializedName("sociable_con_perros") val sociableConPerros: Int,
    @SerializedName("sociable_con_personas") val sociableConPersonas: Int,
    @SerializedName("precauciones_medicas") val precaucionesMedicas: String?,
    @SerializedName("foto_url") val fotoUrl: String?
)

data class UserProfileResponse(
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("tipo_usuario_id") val tipoUsuarioId: Int,
    @SerializedName("direccion_referencia") val direccionReferencia: String?,
    @SerializedName("distrito_id") val distritoId: Int?,
    @SerializedName("contacto_emergencia_nombre") val contactoEmergenciaNombre: String?,
    @SerializedName("contacto_emergencia_telefono") val contactoEmergenciaTelefono: String?,
    @SerializedName("notas_adicionales") val notasAdicionales: String?,
    @SerializedName("mascotas") val mascotas: List<MascotaResponse> = emptyList()
)