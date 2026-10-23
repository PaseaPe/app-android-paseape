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
    @SerializedName("tarifa_hora_pen_snapshot") val tarifaHoraPenSnapshot: Double? = null,
    @SerializedName("monto_total") val montoTotal: Double? = null,
    @SerializedName("paseador_uuid") val paseadorUuid: String? = null,
    @SerializedName("paseador_nombre_completo") val paseadorNombreCompleto: String? = null,
    @SerializedName("cliente_uuid") val clienteUuid: String? = null,
    @SerializedName("cliente_nombre_completo") val clienteNombreCompleto: String? = null,
    @SerializedName("mascotas") val mascotas: List<SolicitudMascotaResDto>? = emptyList(),
    @SerializedName("creado_el") val creadoEl: String? = null
)
