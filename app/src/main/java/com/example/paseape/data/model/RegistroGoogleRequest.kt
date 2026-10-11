package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class RegistroGoogleRequest(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("tipo_usuario_id") val tipoUsuarioId: Int,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("direccion_referencia") val direccionReferencia: String,
    @SerializedName("distrito_id") val distritoId: Int,
    @SerializedName("contacto_emergencia_nombre") val contactoEmergenciaNombre: String,
    @SerializedName("contacto_emergencia_telefono") val contactoEmergenciaTelefono: String,
    @SerializedName("notas_adicionales") val notasAdicionales: String,
    @SerializedName("mascotas") val mascotas: List<MascotaResponse> = emptyList()
)