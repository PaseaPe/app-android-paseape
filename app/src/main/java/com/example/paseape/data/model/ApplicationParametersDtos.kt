package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class ApplicationParametersResponse(
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("response") val response: ApplicationParametersData? = null
)

data class ApplicationParametersData(
    @SerializedName("tipos_usuario") val tiposUsuario: List<TipoUsuarioParamDto>? = null,
    @SerializedName("tipos_documento") val tiposDocumento: List<TipoDocumentoParamDto>? = null,
    @SerializedName("distritos") val distritos: List<DistritoParamDto>? = null,
    @SerializedName("tipos_mascota") val tiposMascota: List<TipoMascotaParamDto>? = null,
    @SerializedName("razas") val razas: List<RazaParamDto>? = null,
    @SerializedName("generos_mascota") val generosMascota: List<GeneroMascotaParamDto>? = null,
    @SerializedName("tamanos_mascota") val tamanosMascota: List<TamanoMascotaParamDto>? = null,
    @SerializedName("niveles_energia") val nivelesEnergia: List<NivelEnergiaParamDto>? = null,
    @SerializedName("dias_semana") val diasSemana: List<DiaSemanaParamDto>? = null,
    @SerializedName("bloques_horario") val bloquesHorario: List<BloqueHorarioParamDto>? = null
)

data class DistritoParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("codigo_ubigeo") val codigoUbigeo: String? = null
)

data class TipoUsuarioParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("descripcion") val descripcion: String
)

data class TipoDocumentoParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("longitud_exacta") val longitudExacta: Int? = null
)

data class TipoMascotaParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("descripcion") val descripcion: String
)

data class RazaParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("tipo_mascota_id") val tipoMascotaId: Int? = null,
    @SerializedName("descripcion") val descripcion: String
)

data class GeneroMascotaParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("descripcion") val descripcion: String
)

data class TamanoMascotaParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("rango_peso_ref") val rangoPesoRef: String? = null
)

data class NivelEnergiaParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("descripcion") val descripcion: String
)

data class DiaSemanaParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("descripcion") val descripcion: String
)

data class BloqueHorarioParamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("descripcion") val descripcion: String
)

data class RazasResponse(
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("response") val response: List<RazaParamDto>? = null,
    @SerializedName("data") val data: List<RazaParamDto>? = null
) {
    fun extraerRazas(): List<RazaParamDto> = response ?: data ?: emptyList()
}

