package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

/**
 * Body opcional de `PATCH paseador/solicitudes/{uuid}/rechazar`.
 *
 * Los DTOs compartidos de solicitudes (`PageResponseDto`, `SolicitudPaseoResDto`
 * y `SolicitudMascotaResDto`) se declaran en [PaseoResDto] (versión de
 * `feat/sprint-2`) para no duplicarlos y evitar errores de redeclaración
 * al hacer merge de las dos ramas.
 */
data class RechazarSolicitudRequest(
    @SerializedName("motivo_rechazo") val motivoRechazo: String? = null
)
