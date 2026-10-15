package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class CrearMascotaPayload(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo_mascota_id") val tipoMascotaId: Int = 1,
    @SerializedName("tipo_raza_id") val tipoRazaId: Int = 1,
    @SerializedName("tipo_genero_mascota_id") val tipoGeneroMascotaId: Int = 1,
    @SerializedName("tipo_tamano_mascota_id") val tipoTamanoMascotaId: Int = 1,
    @SerializedName("tipo_nivel_energia_id") val tipoNivelEnergiaId: Int = 1,
    @SerializedName("edad_anos") val edadAnos: Int = 0,
    @SerializedName("edad_meses") val edadMeses: Int = 0,
    @SerializedName("peso_kg") val pesoKg: Double = 0.0,
    @SerializedName("esterilizado") val esterilizado: Int = 0,
    @SerializedName("sociable_con_perros") val sociableConPerros: Int = 1,
    @SerializedName("sociable_con_personas") val sociableConPersonas: Int = 1,
    @SerializedName("precauciones_medicas") val precaucionesMedicas: String = "",
    @SerializedName("foto_url") val fotoUrl: String? = null
)

data class CrearMascotaRequest(
    @SerializedName("usuario_uuid") val usuarioUuid: String,
    @SerializedName("mascota") val mascota: CrearMascotaPayload
)

data class ActualizarMascotaRequest(
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
    @SerializedName("precauciones_medicas") val precaucionesMedicas: String,
    @SerializedName("foto_url") val fotoUrl: String?
)

data class DetalleMascotaResponse(
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("response") val response: MascotaData? = null,
    @SerializedName("data") val data: MascotaData? = null,
    @SerializedName("id") val id: Long? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("tipo_mascota_id") val tipoMascotaId: Int? = null,
    @SerializedName("tipo_mascota_descripcion") val tipoMascotaDescripcion: String? = null,
    @SerializedName("tipo_raza_id") val tipoRazaId: Int? = null,
    @SerializedName("tipo_raza_descripcion") val tipoRazaDescripcion: String? = null,
    @SerializedName("tipo_genero_mascota_id") val tipoGeneroMascotaId: Int? = null,
    @SerializedName("tipo_genero_mascota_descripcion") val tipoGeneroMascotaDescripcion: String? = null,
    @SerializedName("tipo_tamano_mascota_id") val tipoTamanoMascotaId: Int? = null,
    @SerializedName("tipo_tamano_mascota_descripcion") val tipoTamanoMascotaDescripcion: String? = null,
    @SerializedName("tipo_nivel_energia_id") val tipoNivelEnergiaId: Int? = null,
    @SerializedName("tipo_nivel_energia_descripcion") val tipoNivelEnergiaDescripcion: String? = null,
    @SerializedName("edad_anos") val edadAnos: Int? = null,
    @SerializedName("edad_meses") val edadMeses: Int? = null,
    @SerializedName("peso_kg") val pesoKg: Double? = null,
    @SerializedName("esterilizado") val esterilizado: Int? = null,
    @SerializedName("sociable_con_perros") val sociableConPerros: Int? = null,
    @SerializedName("sociable_con_personas") val sociableConPersonas: Int? = null,
    @SerializedName("precauciones_medicas") val precaucionesMedicas: String? = null,
    @SerializedName("foto_url") val fotoUrl: String? = null
) {
    fun extraerMascota(): MascotaData? {
        if (response != null) return response
        if (data != null) return data
        if (!nombre.isNullOrBlank() || !uuid.isNullOrBlank()) {
            return MascotaData(
                id = id,
                uuid = uuid,
                nombre = nombre,
                tipoMascotaId = tipoMascotaId,
                tipoMascotaDescripcion = tipoMascotaDescripcion,
                tipoRazaId = tipoRazaId,
                tipoRazaDescripcion = tipoRazaDescripcion,
                tipoGeneroMascotaId = tipoGeneroMascotaId,
                tipoGeneroMascotaDescripcion = tipoGeneroMascotaDescripcion,
                tipoTamanoMascotaId = tipoTamanoMascotaId,
                tipoTamanoMascotaDescripcion = tipoTamanoMascotaDescripcion,
                tipoNivelEnergiaId = tipoNivelEnergiaId,
                tipoNivelEnergiaDescripcion = tipoNivelEnergiaDescripcion,
                edadAnos = edadAnos,
                edadMeses = edadMeses,
                pesoKg = pesoKg,
                esterilizado = esterilizado,
                sociableConPerros = sociableConPerros,
                sociableConPersonas = sociableConPersonas,
                precaucionesMedicas = precaucionesMedicas,
                fotoUrl = fotoUrl
            )
        }
        return null
    }
}
