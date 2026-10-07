package com.example.paseape.data.remote.dto.auth

import com.example.paseape.data.remote.dto.mascota.MascotaReqDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsuarioReqDto(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("uuid")
    val uuid: String? = null,

    // Autenticación y Credenciales
    @SerialName("id_token")
    val idToken: String? = null, // Usado en registro vía Google Identity
    @SerialName("contrasena")
    val contrasena: String? = null, // Usado en registro local
    @SerialName("tipo_usuario_id")
    val tipoUsuarioId: Int, // 1 = Cliente, 2 = Paseador (según catálogo)

    // Datos Personales Comunes
    @SerialName("nombres")
    val nombres: String? = null,
    @SerialName("apellidos")
    val apellidos: String? = null,
    @SerialName("correo")
    val correo: String? = null,
    @SerialName("telefono")
    val telefono: String? = null,
    @SerialName("foto_perfil_url")
    val fotoPerfilUrl: String? = null,

    // Campos Específicos para CLIENTE
    @SerialName("direccion_referencia")
    val direccionReferencia: String? = null,
    @SerialName("distrito_id")
    val distritoId: Int? = null,
    @SerialName("contacto_emergencia_nombre")
    val contactoEmergenciaNombre: String? = null,
    @SerialName("contacto_emergencia_telefono")
    val contactoEmergenciaTelefono: String? = null,
    @SerialName("notas_adicionales")
    val notasAdicionales: String? = null,
    @SerialName("mascotas")
    val mascotas: List<MascotaReqDto>? = null,

    // Campos Específicos para PASEADOR
    @SerialName("tipo_documento_id")
    val tipoDocumentoId: Int? = null,
    @SerialName("numero_documento")
    val numeroDocumento: String? = null,
    @SerialName("antecedentes_policiales_url")
    val antecedentesPolicialesUrl: String? = null,
    @SerialName("experiencia_anos")
    val experienciaAnos: Int? = null,
    @SerialName("biografia")
    val biografia: String? = null,
    @SerialName("tarifa_hora_pen")
    val tarifaHoraPen: Double? = null,
    @SerialName("distrito_cobertura_id")
    val distritoCoberturaId: Int? = null
)