package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

/**
 * Cuerpo de `POST /auth/register/google`.
 *
 * El backend toma el correo, nombres, apellidos y foto del token de Google cuando
 * no se envían, pero para el perfil paseador exige documento y cobertura.
 */
data class RegistroGoogleRequest(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("tipo_usuario_id") val tipoUsuarioId: Int,
    @SerializedName("nombres") val nombres: String? = null,
    @SerializedName("apellidos") val apellidos: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("direccion_referencia") val direccionReferencia: String? = null,
    @SerializedName("distrito_id") val distritoId: Int? = null,
    @SerializedName("contacto_emergencia_nombre") val contactoEmergenciaNombre: String? = null,
    @SerializedName("contacto_emergencia_telefono") val contactoEmergenciaTelefono: String? = null,
    @SerializedName("notas_adicionales") val notasAdicionales: String? = null,
    @SerializedName("tipo_documento_id") val tipoDocumentoId: Int? = null,
    @SerializedName("numero_documento") val numeroDocumento: String? = null,
    @SerializedName("experiencia_anos") val experienciaAnos: Int? = null,
    @SerializedName("biografia") val biografia: String? = null,
    @SerializedName("tarifa_hora_pen") val tarifaHoraPen: Double? = null,
    @SerializedName("distrito_cobertura_id") val distritoCoberturaId: Int? = null
)
