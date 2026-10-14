package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class ClienteProfileResponse(
    @SerializedName("statusCode") val statusCode: Int? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("header") val header: Any? = null,
    @SerializedName("response") val response: UsuarioClienteData? = null,
    @SerializedName("data") val data: UsuarioClienteData? = null,
    @SerializedName("id") val id: Long? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("nombres") val nombres: String? = null,
    @SerializedName("apellidos") val apellidos: String? = null,
    @SerializedName("correo") val correo: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("tipo_usuario_descripcion") val tipoUsuarioDescripcion: String? = null,
    @SerializedName("usuario_estado_descripcion") val usuarioEstadoDescripcion: String? = null,
    @SerializedName("cliente") val cliente: ClienteDetalleData? = null,
    @SerializedName("cliente_detalle") val clienteDetalle: ClienteDetalleData? = null,
    @SerializedName("detalle") val detalle: ClienteDetalleData? = null
) {
    fun extraerUsuarioCliente(): UsuarioClienteData? {
        if (response != null) return response
        if (data != null) return data
        if (!nombres.isNullOrBlank() || !uuid.isNullOrBlank()) {
            return UsuarioClienteData(
                id = id,
                uuid = uuid,
                nombres = nombres,
                apellidos = apellidos,
                correo = correo,
                telefono = telefono,
                tipoUsuarioDescripcion = tipoUsuarioDescripcion,
                usuarioEstadoDescripcion = usuarioEstadoDescripcion,
                cliente = cliente ?: clienteDetalle ?: detalle
            )
        }
        return null
    }
}

data class UsuarioClienteData(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("nombres") val nombres: String? = null,
    @SerializedName("apellidos") val apellidos: String? = null,
    @SerializedName("correo") val correo: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("tipo_usuario_descripcion") val tipoUsuarioDescripcion: String? = null,
    @SerializedName("usuario_estado_descripcion") val usuarioEstadoDescripcion: String? = null,
    @SerializedName("cliente") val cliente: ClienteDetalleData? = null,
    @SerializedName("cliente_detalle") val clienteDetalle: ClienteDetalleData? = null,
    @SerializedName("detalle") val detalle: ClienteDetalleData? = null
) {
    val detalleCliente: ClienteDetalleData?
        get() = cliente ?: clienteDetalle ?: detalle
}

data class ClienteDetalleData(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("direccion_referencia") val direccionReferencia: String? = null,
    @SerializedName("distrito_descripcion") val distritoDescripcion: String? = null,
    @SerializedName("contacto_emergencia_nombre") val contactoEmergenciaNombre: String? = null,
    @SerializedName("contacto_emergencia_telefono") val contactoEmergenciaTelefono: String? = null,
    @SerializedName("notas_adicionales") val notasAdicionales: String? = null,
    @SerializedName("mascotas") val mascotas: List<MascotaData>? = emptyList()
)

data class MascotaData(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("tipo_mascota_descripcion") val tipoMascotaDescripcion: String? = null,
    @SerializedName("tipo_raza_descripcion") val tipoRazaDescripcion: String? = null,
    @SerializedName("tipo_genero_mascota_descripcion") val tipoGeneroMascotaDescripcion: String? = null,
    @SerializedName("tipo_tamano_mascota_descripcion") val tipoTamanoMascotaDescripcion: String? = null,
    @SerializedName("tipo_nivel_energia_descripcion") val tipoNivelEnergiaDescripcion: String? = null,
    @SerializedName("edad_anos") val edadAnos: Int? = null,
    @SerializedName("edad_meses") val edadMeses: Int? = null,
    @SerializedName("peso_kg") val pesoKg: Double? = null,
    @SerializedName("esterilizado") val esterilizado: Int? = null,
    @SerializedName("sociable_con_perros") val sociableConPerros: Int? = null,
    @SerializedName("sociable_con_personas") val sociableConPersonas: Int? = null,
    @SerializedName("precauciones_medicas") val precaucionesMedicas: String? = null,
    @SerializedName("foto_url") val fotoUrl: String? = null
)

// Modelos conservados para retrocompatibilidad con RegistroRequest y RegistroGoogleRequest
data class MascotaResponse(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo_mascota_id") val tipoMascotaId: Int,
    @SerializedName("tipo_raza_id") val tipoRazaId: Int,
    @SerializedName("tipo_genero_mascota_id") val tipoGeneroMascotaId: Int,
    @SerializedName("tipo_tamano_mascota_id") val tipoTamanoMascotaId: Int,
    @SerializedName("tipo_nivel_energia_id") val tipoNivelEnergiaId: Int,
    @SerializedName("edad_anos") val edadAnos: Int,
    @SerializedName("edad_meses") val edadMeses: Int,
    @SerializedName("peso_kg") val pesoKg: Double,
    @SerializedName("esterilizado") val esterilizado: Int,
    @SerializedName("sociable_con_perros") val sociableConPerros: Int,
    @SerializedName("sociable_con_personas") val sociableConPersonas: Int,
    @SerializedName("precauciones_medicas") val precaucionesMedicas: String?,
    @SerializedName("foto_url") val fotoUrl: String?
)

data class UserProfileResponse(
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("tipo_usuario_id") val tipoUsuarioId: Int,
    @SerializedName("direccion_referencia") val direccionReferencia: String?,
    @SerializedName("distrito_id") val distritoId: Int?,
    @SerializedName("contacto_emergencia_nombre") val contactoEmergenciaNombre: String?,
    @SerializedName("contacto_emergencia_telefono") val contactoEmergenciaTelefono: String?,
    @SerializedName("notas_adicionales") val notasAdicionales: String?,
    @SerializedName("mascotas") val mascotas: List<MascotaResponse> = emptyList()
)