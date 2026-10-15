package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class RegistroRequest(
    // Campos comunes
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("contrasena") val contrasena: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("tipo_usuario_id") val tipoUsuarioId: Int, // 1: Cliente, 2: Paseador

    // Campos específicos para Cliente (Dueño)
    @SerializedName("direccion_referencia") val direccionReferencia: String? = null,
    @SerializedName("distrito_id") val distritoId: Int? = null,
    @SerializedName("contacto_emergencia_nombre") val contactoEmergenciaNombre: String? = null,
    @SerializedName("contacto_emergencia_telefono") val contactoEmergenciaTelefono: String? = null,
    @SerializedName("notas_adicionales") val notasAdicionales: String? = null,
    @SerializedName("mascotas") val mascotas: List<MascotaResponse>? = null,

    // Campos específicos para Paseador
    @SerializedName("tipo_documento_id") val tipoDocumentoId: Int? = null,
    @SerializedName("numero_documento") val numeroDocumento: String? = null,
    @SerializedName("antecedentes_policiales_url") val antecedentesPolicialesUrl: String? = null,
    @SerializedName("experiencia_anos") val experienciaAnos: Int? = null,
    @SerializedName("biografia") val biografia: String? = null,
    @SerializedName("tarifa_hora_pen") val tarifaHoraPen: Double? = null,
    @SerializedName("distrito_cobertura_id") val distritoCoberturaId: Int? = null
)