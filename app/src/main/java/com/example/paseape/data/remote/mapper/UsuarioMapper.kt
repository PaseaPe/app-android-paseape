package com.example.paseape.data.remote.mapper

import com.example.paseape.core.util.AppConstants
import com.example.paseape.data.remote.dto.auth.UsuarioReqDto
import com.example.paseape.data.remote.dto.mascota.MascotaReqDto
import com.example.paseape.domain.model.RegisterClientParams
import com.example.paseape.domain.model.RegisterGoogleClientParams
import com.example.paseape.domain.model.RegisterGoogleWalkerParams
import com.example.paseape.domain.model.RegisterPetParams
import com.example.paseape.domain.model.RegisterWalkerParams

fun RegisterPetParams.toDto(): MascotaReqDto = MascotaReqDto(
    nombre = nombre,
    tipoMascotaId = tipoMascotaId,
    tipoRazaId = tipoRazaId,
    tipoGeneroMascotaId = tipoGeneroMascotaId,
    tipoTamanoMascotaId = tipoTamanoMascotaId,
    tipoNivelEnergiaId = tipoNivelEnergiaId,
    edadAnos = edadAnos,
    edadMeses = edadMeses,
    pesoKg = pesoKg,
    esterilizado = if (esterilizado) 1 else 0
)

fun RegisterClientParams.toDto(tipoUsuarioId: Int = AppConstants.TIPO_USUARIO_CLIENTE_ID): UsuarioReqDto {
    return UsuarioReqDto(
        tipoUsuarioId = tipoUsuarioId,
        idToken = idTokenGoogle,
        contrasena = contrasena,
        nombres = nombres.trim(),
        apellidos = apellidos.trim(),
        correo = correo.trim().lowercase(),
        telefono = telefono.trim(),
        direccionReferencia = direccionReferencia.trim(),
        distritoId = distritoId,
        contactoEmergenciaNombre = contactoEmergenciaNombre?.trim(),
        contactoEmergenciaTelefono = contactoEmergenciaTelefono?.trim(),
        notasAdicionales = notasAdicionales?.trim(),
        mascotas = mascotas.map { it.toDto() }
    )
}

fun RegisterWalkerParams.toDto(tipoUsuarioId: Int = AppConstants.TIPO_USUARIO_PASEADOR_ID): UsuarioReqDto {
    return UsuarioReqDto(
        tipoUsuarioId = tipoUsuarioId,
        idToken = idTokenGoogle,
        contrasena = contrasena,
        nombres = nombres.trim(),
        apellidos = apellidos.trim(),
        correo = correo.trim().lowercase(),
        telefono = telefono.trim(),
        tipoDocumentoId = tipoDocumentoId,
        numeroDocumento = numeroDocumento.trim(),
        experienciaAnos = experienciaAnos,
        biografia = biografia.trim(),
        tarifaHoraPen = tarifaHoraPen,
        distritoCoberturaId = distritoCoberturaId
    )
}

fun RegisterGoogleClientParams.toDto(): UsuarioReqDto = UsuarioReqDto(
    idToken = idToken,
    tipoUsuarioId = AppConstants.TIPO_USUARIO_CLIENTE_ID,
    telefono = telefono.trim(),
    direccionReferencia = direccionReferencia.trim(),
    distritoId = distritoId,
    contactoEmergenciaNombre = contactoEmergenciaNombre?.trim()?.ifBlank { null },
    contactoEmergenciaTelefono = contactoEmergenciaTelefono?.trim()?.ifBlank { null },
    notasAdicionales = notasAdicionales?.trim()?.ifBlank { null },
    mascotas = if (mascotas.isNotEmpty()) mascotas.map { it.toDto() } else null
)

fun RegisterGoogleWalkerParams.toDto(): UsuarioReqDto = UsuarioReqDto(
    idToken = idToken,
    tipoUsuarioId = AppConstants.TIPO_USUARIO_PASEADOR_ID,
    telefono = telefono.trim(),
    tipoDocumentoId = tipoDocumentoId,
    numeroDocumento = numeroDocumento.trim(),
    antecedentesPolicialesUrl = antecedentesPolicialesUrl?.trim()?.ifBlank { null },
    experienciaAnos = experienciaAnos,
    biografia = biografia.trim(),
    tarifaHoraPen = tarifaHoraPen,
    distritoCoberturaId = distritoCoberturaId
)