package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

/**
 * Página genérica del backend (JSON snake_case).
 *
 * Ejemplo: `{ "items": [], "page": 0, "size": 20, "total_elements": 0,
 * "total_pages": 0, "is_first": true, "is_last": true, "has_next": false,
 * "has_previous": false }`
 */
data class PageResponseDto<T>(
    val items: List<T>?,
    val page: Int?,
    val size: Int?,
    @SerializedName("total_elements") val totalElements: Long?,
    @SerializedName("total_pages") val totalPages: Int?,
    @SerializedName("is_first") val isFirst: Boolean?,
    @SerializedName("is_last") val isLast: Boolean?,
    @SerializedName("has_next") val hasNext: Boolean?,
    @SerializedName("has_previous") val hasPrevious: Boolean?
)

/** Solicitud/paseo tal como lo devuelve la bandeja del paseador. */
data class SolicitudPaseoResDto(
    val uuid: String?,
    @SerializedName("codigo_reserva") val codigoReserva: String?,
    @SerializedName("tipo_modalidad_paseo_id") val tipoModalidadPaseoId: Int?,
    @SerializedName("tipo_modalidad_paseo_descripcion") val tipoModalidadPaseoDescripcion: String?,
    @SerializedName("estado_id") val estadoId: Int?,
    @SerializedName("estado_descripcion") val estadoDescripcion: String?,
    @SerializedName("tipo_duracion_paseo_id") val tipoDuracionPaseoId: Int?,
    @SerializedName("tipo_duracion_paseo_descripcion") val tipoDuracionPaseoDescripcion: String?,
    @SerializedName("distrito_id") val distritoId: Int?,
    @SerializedName("distrito_descripcion") val distritoDescripcion: String?,
    @SerializedName("direccion_recojo") val direccionRecojo: String?,
    @SerializedName("referencia_direccion") val referenciaDireccion: String?,
    @SerializedName("tarifa_hora_pen_snapshot") val tarifaHoraPenSnapshot: String?,
    @SerializedName("monto_total") val montoTotal: String?,
    @SerializedName("paseador_uuid") val paseadorUuid: String?,
    @SerializedName("paseador_nombre_completo") val paseadorNombreCompleto: String?,
    @SerializedName("cliente_uuid") val clienteUuid: String?,
    @SerializedName("cliente_nombre_completo") val clienteNombreCompleto: String?,
    val mascotas: List<SolicitudMascotaResDto>?,
    @SerializedName("hora_vencimiento") val horaVencimiento: String?,
    @SerializedName("creado_el") val creadoEl: String?
)

/** Mascota asociada a una solicitud de paseo. */
data class SolicitudMascotaResDto(
    val uuid: String?,
    val nombre: String?,
    val raza: String?,
    val tamano: String?,
    @SerializedName("foto_url") val fotoUrl: String?
)

/** Body opcional de `PATCH paseador/solicitudes/{uuid}/rechazar`. */
data class RechazarSolicitudRequest(
    @SerializedName("motivo_rechazo") val motivoRechazo: String? = null
)
