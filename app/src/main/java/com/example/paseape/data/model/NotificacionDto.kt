package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class NotificacionDto(
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("tipo_notificacion_id") val tipoNotificacionId: Int? = null,
    @SerializedName("tipo_notificacion_descripcion") val tipoNotificacionDescripcion: String? = null,
    @SerializedName("titulo") val titulo: String? = null,
    @SerializedName("mensaje") val mensaje: String? = null,
    @SerializedName("referencia_id") val referenciaId: Long? = null,
    @SerializedName("referencia_tipo") val referenciaTipo: String? = null,
    @SerializedName("remitente_uuid") val remitenteUuid: String? = null,
    @SerializedName("remitente_nombre_completo") val remitenteNombreCompleto: String? = null,
    @SerializedName("leido") val leido: Boolean? = false,
    @SerializedName("fecha_leido") val fechaLeido: String? = null,
    @SerializedName("creado_el") val creadoEl: String? = null
)
