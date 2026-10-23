package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta de GET/PUT /paseador/disponibilidad.
 * El backend envuelve el payload en { statusCode, code, message, header, response }.
 */
data class DisponibilidadResponseDto(
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("response") val response: DisponibilidadDataDto? = null
)

data class DisponibilidadDataDto(
    @SerializedName("max_mascotas_predeterminado") val maxMascotasPredeterminado: Int? = null,
    @SerializedName("slots") val slots: List<DisponibilidadSlotDto>? = null
)

data class DisponibilidadSlotDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("dia_semana_id") val diaSemanaId: Int? = null,
    @SerializedName("dia_semana_descripcion") val diaSemanaDescripcion: String? = null,
    @SerializedName("bloque_horario_id") val bloqueHorarioId: Int? = null,
    @SerializedName("bloque_codigo") val bloqueCodigo: String? = null,
    @SerializedName("hora_inicio") val horaInicio: String? = null,
    @SerializedName("hora_fin") val horaFin: String? = null,
    @SerializedName("max_mascotas") val maxMascotas: Int? = null,
    @SerializedName("activo") val activo: Boolean? = null,
    // Campos de ocupación: pueden venir null y no se muestran en la pantalla.
    @SerializedName("mascotas_ocupadas") val mascotasOcupadas: Int? = null,
    @SerializedName("cupos_disponibles") val cuposDisponibles: Int? = null,
    @SerializedName("esta_lleno") val estaLleno: Boolean? = null,
    @SerializedName("disponible") val disponible: Boolean? = null
)

/** Body de PUT /paseador/disponibilidad. */
data class GuardarDisponibilidadRequestDto(
    @SerializedName("bloques") val bloques: List<DisponibilidadBloqueRequestDto>
)

data class DisponibilidadBloqueRequestDto(
    @SerializedName("dia_semana_id") val diaSemanaId: Int,
    @SerializedName("bloque_horario_id") val bloqueHorarioId: Int,
    @SerializedName("max_mascotas") val maxMascotas: Int
)

/** Body de PATCH /paseador/disponibilidad/bloque. */
data class ActualizarBloqueDisponibilidadRequestDto(
    @SerializedName("dia_semana_id") val diaSemanaId: Int,
    @SerializedName("bloque_horario_id") val bloqueHorarioId: Int,
    @SerializedName("accion") val accion: String,
    @SerializedName("max_mascotas") val maxMascotas: Int? = null
)

/** Body de PATCH /paseador/disponibilidad/capacidad-base. */
data class ActualizarCapacidadBaseRequestDto(
    @SerializedName("max_mascotas_predeterminado") val maxMascotasPredeterminado: Int
)

/**
 * Respuesta de los PATCH de disponibilidad. Solo se necesita el estado HTTP y,
 * en caso de error, el mensaje ya lo resuelve ApiErrorMapper desde el errorBody.
 */
data class DisponibilidadAccionResponseDto(
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null
)
