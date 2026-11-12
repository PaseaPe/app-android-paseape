package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class PaseoResDto(
    val id: Long,
    val mascotaId: Long,
    val mascotaNombre: String,
    val clienteId: Long,
    val paseadorId: Long?,
    val fechaProgramada: String,
    val estadoPaseoId: Int,
    val distritoId: Int,
    val tarifaPen: Double
)

data class CrearSolicitudInmediataReqDto(
    @SerializedName("mascota_uuids") val mascotaUuids: List<String>,
    @SerializedName("distrito_id") val distritoId: Int,
    @SerializedName("tipo_duracion_paseo_id") val tipoDuracionPaseoId: Int,
    @SerializedName("tipo_metodo_pago_id") val tipoMetodoPagoId: Int,
    @SerializedName("tarifa_hora_pen") val tarifaHoraPen: Double,
    @SerializedName("direccion_recojo") val direccionRecojo: String,
    @SerializedName("referencia_direccion") val referenciaDireccion: String? = null
)

data class CrearSolicitudProgramadaReqDto(
    @SerializedName("paseador_uuid") val paseadorUuid: String,
    @SerializedName("mascota_uuids") val mascotaUuids: List<String>,
    @SerializedName("distrito_id") val distritoId: Int,
    @SerializedName("fecha_programada") val fechaProgramada: String,
    @SerializedName("bloque_horario_id") val bloqueHorarioId: Int,
    @SerializedName("tipo_duracion_paseo_id") val tipoDuracionPaseoId: Int,
    @SerializedName("tipo_metodo_pago_id") val tipoMetodoPagoId: Int,
    @SerializedName("direccion_recojo") val direccionRecojo: String,
    @SerializedName("referencia_direccion") val referenciaDireccion: String? = null
)

data class SolicitudMascotaResDto(
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("raza") val raza: String? = null,
    @SerializedName("tamano") val tamano: String? = null,
    @SerializedName("foto_url") val fotoUrl: String? = null
)

data class SolicitudPaseoResDto(
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("codigo_reserva") val codigoReserva: String? = null,
    @SerializedName("tipo_modalidad_paseo_id") val tipoModalidadPaseoId: Int? = null,
    @SerializedName("tipo_modalidad_paseo_descripcion") val tipoModalidadPaseoDescripcion: String? = null,
    @SerializedName("estado_id") val estadoId: Int? = null,
    @SerializedName("estado_descripcion") val estadoDescripcion: String? = null,
    @SerializedName("tipo_metodo_pago_id") val tipoMetodoPagoId: Int? = null,
    @SerializedName("tipo_metodo_pago_descripcion") val tipoMetodoPagoDescripcion: String? = null,
    @SerializedName("tipo_duracion_paseo_id") val tipoDuracionPaseoId: Int? = null,
    @SerializedName("tipo_duracion_paseo_descripcion") val tipoDuracionPaseoDescripcion: String? = null,
    @SerializedName("distrito_id") val distritoId: Int? = null,
    @SerializedName("distrito_descripcion") val distritoDescripcion: String? = null,
    @SerializedName("direccion_recojo") val direccionRecojo: String? = null,
    @SerializedName("referencia_direccion") val referenciaDireccion: String? = null,
    @SerializedName("fecha_programada") val fechaProgramada: String? = null,
    @SerializedName("bloque_horario_id") val bloqueHorarioId: Int? = null,
    @SerializedName("bloque_horario_codigo") val bloqueHorarioCodigo: String? = null,
    @SerializedName("tarifa_hora_pen_snapshot") val tarifaHoraPenSnapshot: Double? = null,
    @SerializedName("monto_total") val montoTotal: Double? = null,
    @SerializedName("paseador_uuid") val paseadorUuid: String? = null,
    @SerializedName("paseador_nombre_completo") val paseadorNombreCompleto: String? = null,
    @SerializedName("cliente_uuid") val clienteUuid: String? = null,
    @SerializedName("cliente_nombre_completo") val clienteNombreCompleto: String? = null,
    @SerializedName("mascotas") val mascotas: List<SolicitudMascotaResDto>? = emptyList(),
    @SerializedName("hora_vencimiento") val horaVencimiento: String? = null,
    @SerializedName("creado_el") val creadoEl: String? = null,
    @SerializedName("fecha_hora_inicio_real") val fechaHoraInicioReal: String? = null,
    @SerializedName("fecha_hora_fin_real") val fechaHoraFinReal: String? = null
)

data class PageResponseDto<T>(
    @SerializedName("items") val items: List<T>? = emptyList(),
    @SerializedName("page") val page: Int? = 0,
    @SerializedName("size") val size: Int? = 10,
    @SerializedName("total_elements") val totalElements: Long? = 0L,
    @SerializedName("total_pages") val totalPages: Int? = 0,
    @SerializedName("is_first") val isFirst: Boolean? = true,
    @SerializedName("is_last") val isLast: Boolean? = true,
    @SerializedName("has_next") val hasNext: Boolean? = false,
    @SerializedName("has_previous") val hasPrevious: Boolean? = false
)

data class CancelarSolicitudReqDto(
    @SerializedName("motivo") val motivo: String? = null
)

/**
 * Respuesta de seguimiento activo en vivo (Sprint 3 - HU24):
 * `GET /cliente/solicitudes/{uuid}/ubicacion-actual`
 */
data class UbicacionEnVivoResDto(
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("solicitud_paseo_uuid") val solicitudPaseoUuid: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("precision_m") val precisionM: Double? = null,
    @SerializedName("velocidad_ms") val velocidadMs: Double? = null,
    @SerializedName("rumbo_grados") val rumboGrados: Double? = null,
    @SerializedName("fecha_hora_gps") val fechaHoraGps: String? = null,
    @SerializedName("senal_degradada") val senalDegradada: Boolean? = false
)

/**
 * Respuesta de recorrido histórico consolidado post-paseo (Sprint 3 - HU24):
 * `GET /solicitudes/{uuid}/recorrido`
 */
data class SolicitudPaseoRecorridoResDto(
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("solicitud_paseo_uuid") val solicitudPaseoUuid: String? = null,
    @SerializedName("distancia_metros") val distanciaMetros: Double? = null,
    @SerializedName("duracion_segundos") val duracionSegundos: Int? = null,
    @SerializedName("velocidad_promedio_kmh") val velocidadPromedioKmh: Double? = null,
    @SerializedName("coordenadas_codificadas") val coordenadasCodificadas: String? = null,
    @SerializedName("coordenadas_total_puntos") val coordenadasTotalPuntos: Int? = null,
    @SerializedName("tipo_origen_traza_descripcion") val tipoOrigenTrazaDescripcion: String? = null,
    @SerializedName("fecha_hora_inicio") val fechaHoraInicio: String? = null,
    @SerializedName("fecha_hora_fin") val fechaHoraFin: String? = null
)
