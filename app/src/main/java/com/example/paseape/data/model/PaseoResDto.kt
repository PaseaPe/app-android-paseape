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
    @SerializedName("foto_url") val fotoUrl: String? = null,
    // Campos que agrega el endpoint de detalle GET /solicitudes/{uuid}
    @SerializedName("tipo_raza_descripcion") val tipoRazaDescripcion: String? = null,
    @SerializedName("tipo_tamano_mascota_descripcion") val tipoTamanoMascotaDescripcion: String? = null,
    @SerializedName("tipo_nivel_energia_descripcion") val tipoNivelEnergiaDescripcion: String? = null,
    @SerializedName("precauciones_medicas") val precaucionesMedicas: String? = null,
    @SerializedName("edad_anos") val edadAnos: Int? = null,
    @SerializedName("edad_meses") val edadMeses: Int? = null,
    @SerializedName("peso_kg") val pesoKg: Double? = null,
    @SerializedName("sociable_con_perros") val sociableConPerros: Int? = null,
    @SerializedName("sociable_con_personas") val sociableConPersonas: Int? = null,
    @SerializedName("esterilizado") val esterilizado: Int? = null,
    @SerializedName("foto_mascota") val fotoMascota: DocumentUrlDto? = null
) {
    /** El detalle devuelve `foto_mascota`; la bandeja mantiene `foto_url`. */
    val fotoUrlEfectiva: String?
        get() = fotoMascota?.url?.takeIf { it.isNotBlank() }
            ?: fotoUrl?.takeIf { it.isNotBlank() }

    val razaEfectiva: String?
        get() = tipoRazaDescripcion?.takeIf { it.isNotBlank() }
            ?: raza?.takeIf { it.isNotBlank() }

    val tamanoEfectivo: String?
        get() = tipoTamanoMascotaDescripcion?.takeIf { it.isNotBlank() }
            ?: tamano?.takeIf { it.isNotBlank() }
}

/**
 * Datos detallados del paseador incluidos dentro de `GET /solicitudes/{uuid}` (`SolicitudPaseoDetalleResDto`).
 */
data class SolicitudPaseadorDetalleResDto(
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("usuario_uuid") val usuarioUuid: String? = null,
    @SerializedName("nombres") val nombres: String? = null,
    @SerializedName("apellidos") val apellidos: String? = null,
    @SerializedName("nombre_completo") val nombreCompleto: String? = null,
    @SerializedName("correo") val correo: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("foto_perfil_url") val fotoPerfilUrl: String? = null,
    @SerializedName("experiencia_anos") val experienciaAnos: Int? = null,
    @SerializedName("biografia") val biografia: String? = null,
    @SerializedName("tarifa_hora_pen") val tarifaHoraPen: Double? = null,
    @SerializedName("distrito_cobertura_id") val distritoCoberturaId: Int? = null,
    @SerializedName("distrito_cobertura_descripcion") val distritoCoberturaDescripcion: String? = null,
    @SerializedName("paseos_completados") val paseosCompletados: Int? = null,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double? = null
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
    @SerializedName("duracion_minutos") val duracionMinutos: Int? = null,
    @SerializedName("distrito_id") val distritoId: Int? = null,
    @SerializedName("distrito_descripcion") val distritoDescripcion: String? = null,
    @SerializedName("direccion_recojo") val direccionRecojo: String? = null,
    @SerializedName("referencia_direccion") val referenciaDireccion: String? = null,
    @SerializedName("fecha_programada") val fechaProgramada: String? = null,
    @SerializedName("bloque_horario_id") val bloqueHorarioId: Int? = null,
    @SerializedName("bloque_horario_codigo") val bloqueHorarioCodigo: String? = null,
    @SerializedName("bloque_horario_hora_inicio") val bloqueHorarioHoraInicio: String? = null,
    @SerializedName("bloque_horario_hora_fin") val bloqueHorarioHoraFin: String? = null,
    @SerializedName("tarifa_hora_pen_snapshot") val tarifaHoraPenSnapshot: Double? = null,
    @SerializedName("monto_total") val montoTotal: Double? = null,
    @SerializedName("motivo_rechazo_cancelacion") val motivoRechazoCancelacion: String? = null,
    @SerializedName("paseador_uuid") val paseadorUuid: String? = null,
    @SerializedName("paseador_nombre_completo") val paseadorNombreCompleto: String? = null,
    @SerializedName("paseador") val paseador: SolicitudPaseadorDetalleResDto? = null,
    @SerializedName("cliente_uuid") val clienteUuid: String? = null,
    @SerializedName("cliente_nombre_completo") val clienteNombreCompleto: String? = null,
    @SerializedName("cliente_telefono") val clienteTelefono: String? = null,
    @SerializedName("mascotas") val mascotas: List<SolicitudMascotaResDto>? = emptyList(),
    @SerializedName("hora_vencimiento") val horaVencimiento: String? = null,
    @SerializedName("creado_el") val creadoEl: String? = null,
    @SerializedName("fecha_hora_aceptada") val fechaHoraAceptada: String? = null,
    @SerializedName("fecha_hora_inicio_real") val fechaHoraInicioReal: String? = null,
    @SerializedName("fecha_hora_fin_real") val fechaHoraFinReal: String? = null
) {
    val paseadorUuidEfectivo: String?
        get() = paseador?.uuid?.takeIf { it.isNotBlank() }
            ?: paseadorUuid?.takeIf { it.isNotBlank() }

    val paseadorUsuarioUuidEfectivo: String?
        get() = paseador?.usuarioUuid?.takeIf { it.isNotBlank() }
            ?: paseadorUuid?.takeIf { it.isNotBlank() }

    val paseadorNombreEfectivo: String?
        get() = paseador?.nombreCompleto?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(paseador?.nombres?.trim(), paseador?.apellidos?.trim())
                .joinToString(" ")
                .trim()
                .takeIf { it.isNotBlank() }
            ?: paseadorNombreCompleto?.takeIf { it.isNotBlank() }

    val paseadorTelefonoEfectivo: String?
        get() = paseador?.telefono?.takeIf { it.isNotBlank() }

    val paseadorFotoUrlEfectiva: String?
        get() = paseador?.fotoPerfilUrl?.takeIf { it.isNotBlank() }

    val paseadorCalificacionEfectiva: Double?
        get() = paseador?.calificacionPromedio

    val paseadorPaseosCompletadosEfectivos: Int?
        get() = paseador?.paseosCompletados
}

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
