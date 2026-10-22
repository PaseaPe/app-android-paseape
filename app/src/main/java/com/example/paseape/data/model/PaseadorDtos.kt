package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class PaseadorDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("usuario_uuid") val usuarioUuid: String? = null,
    @SerializedName("nombres") val nombres: String? = null,
    @SerializedName("apellidos") val apellidos: String? = null,
    @SerializedName("correo") val correo: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("tipo_documento_id") val tipoDocumentoId: Int? = null,
    @SerializedName("numero_documento") val numeroDocumento: String? = null,
    @SerializedName("experiencia_anos") val experienciaAnos: Int? = null,
    @SerializedName("biografia") val biografia: String? = null,
    @SerializedName("tarifa_hora_pen") val tarifaHoraPen: Double? = null,
    @SerializedName("distrito_cobertura_id") val distritoCoberturaId: Int? = null,
    @SerializedName("distrito_cobertura_descripcion") val distritoCoberturaDescripcion: String? = null,
    @SerializedName("estado_verificacion_id") val estadoVerificacionId: Int? = null,
    @SerializedName("estado_verificacion_descripcion") val estadoVerificacionDescripcion: String? = null,
    @SerializedName("paseos_completados") val paseosCompletados: Int? = null,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double? = null,
    @SerializedName("estado") val estado: Int? = null
) {
    val nombreCompleto: String
        get() = "${nombres.orEmpty()} ${apellidos.orEmpty()}".trim().ifBlank { "Paseador" }
}

data class PaseadoresListResponse(
    @SerializedName("response") val response: List<PaseadorDto>? = null,
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null
)

data class UsuarioPaseadorDetalleDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("nombres") val nombres: String? = null,
    @SerializedName("apellidos") val apellidos: String? = null,
    @SerializedName("correo") val correo: String? = null,
    @SerializedName("correo_verificado") val correoVerificado: Int? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("foto_perfil_url") val fotoPerfilUrl: String? = null,
    @SerializedName("tipo_usuario_id") val tipoUsuarioId: Int? = null,
    @SerializedName("tipo_usuario_descripcion") val tipoUsuarioDescripcion: String? = null,
    @SerializedName("usuario_estado_id") val usuarioEstadoId: Int? = null,
    @SerializedName("usuario_estado_descripcion") val usuarioEstadoDescripcion: String? = null,
    @SerializedName("tipo_proveedor_auth_id") val tipoProveedorAuthId: Int? = null,
    @SerializedName("tipo_proveedor_auth_descripcion") val tipoProveedorAuthDescripcion: String? = null,
    @SerializedName("estado") val estado: Int? = null,
    @SerializedName("cliente") val cliente: Any? = null,
    @SerializedName("paseador") val paseador: PaseadorDto? = null
) {
    val nombreCompleto: String
        get() {
            val nombreBase = "${nombres.orEmpty()} ${apellidos.orEmpty()}".trim()
            if (nombreBase.isNotBlank()) return nombreBase
            return paseador?.nombreCompleto ?: "Paseador"
        }
}

data class PaseadorDetalleResponseData(
    @SerializedName("usuario") val usuario: UsuarioPaseadorDetalleDto? = null,
    @SerializedName("disponibilidad") val disponibilidad: DisponibilidadPaseadorDto? = null
)

data class DisponibilidadPaseadorDto(
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
    @SerializedName("mascotas_ocupadas") val mascotasOcupadas: Int? = null,
    @SerializedName("cupos_disponibles") val cuposDisponibles: Int? = null,
    @SerializedName("esta_lleno") val estaLleno: Boolean? = null,
    @SerializedName("disponible") val disponible: Boolean? = null
)

data class DetallePaseadorResponse(
    @SerializedName("response") val response: PaseadorDetalleResponseData? = null,
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null
)
