package com.example.paseape.data.remote.mapper

import com.example.paseape.core.util.AppConstants
import com.example.paseape.domain.model.RegisterClientParams
import com.example.paseape.domain.model.RegisterPetParams
import com.example.paseape.domain.model.RegisterWalkerParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsuarioMapperTest {

    @Test
    fun registerClientParams_toDto_withPets_mapsAllFieldsCorrectly() {
        val pet = RegisterPetParams(
            nombre = "Bobby",
            tipoMascotaId = 1,
            tipoRazaId = 4,
            tipoGeneroMascotaId = 1,
            tipoTamanoMascotaId = 3,
            tipoNivelEnergiaId = 3,
            edadAnos = 2,
            edadMeses = 4,
            pesoKg = 24.50,
            esterilizado = true,
            sociableConPerros = true,
            sociableConPersonas = true,
            precaucionesMedicas = "Alergia cutánea estacional",
            fotoUrl = "https://storage.paseape.com/mascotas/bobby.jpg"
        )

        val params = RegisterClientParams(
            nombres = "Diego Alonso",
            apellidos = "Aparicio Vela",
            correo = "diego.cliente@example.com",
            contrasena = "PaseaPe2026*",
            telefono = "+51981234567",
            direccionReferencia = "Calle Las Begonias 450, Dpto 302",
            distritoId = 31,
            contactoEmergenciaNombre = "Carlos Vela",
            contactoEmergenciaTelefono = "+51987654320",
            notasAdicionales = "Edificio con recepción las 24 horas",
            mascotas = listOf(pet)
        )

        val dto = params.toDto()

        assertEquals(AppConstants.TIPO_USUARIO_CLIENTE_ID, dto.tipoUsuarioId)
        assertEquals("Diego Alonso", dto.nombres)
        assertEquals("Aparicio Vela", dto.apellidos)
        assertEquals("diego.cliente@example.com", dto.correo)
        assertEquals("PaseaPe2026*", dto.contrasena)
        assertEquals("+51981234567", dto.telefono)
        assertEquals("Calle Las Begonias 450, Dpto 302", dto.direccionReferencia)
        assertEquals(31, dto.distritoId)
        assertEquals("Carlos Vela", dto.contactoEmergenciaNombre)
        assertEquals("+51987654320", dto.contactoEmergenciaTelefono)
        assertEquals("Edificio con recepción las 24 horas", dto.notasAdicionales)

        val petsDto = dto.mascotas
        assertEquals(1, petsDto?.size)
        val firstPetDto = petsDto!!.first()
        assertEquals("Bobby", firstPetDto.nombre)
        assertEquals(1, firstPetDto.tipoMascotaId)
        assertEquals(4, firstPetDto.tipoRazaId)
        assertEquals(1, firstPetDto.tipoGeneroMascotaId)
        assertEquals(3, firstPetDto.tipoTamanoMascotaId)
        assertEquals(3, firstPetDto.tipoNivelEnergiaId)
        assertEquals(2, firstPetDto.edadAnos)
        assertEquals(4, firstPetDto.edadMeses)
        assertEquals(24.50, firstPetDto.pesoKg!!, 0.001)
        assertEquals(1, firstPetDto.esterilizado)
        assertEquals(1, firstPetDto.sociableConPerros)
        assertEquals(1, firstPetDto.sociableConPersonas)
        assertEquals("Alergia cutánea estacional", firstPetDto.precaucionesMedicas)
        assertEquals("https://storage.paseape.com/mascotas/bobby.jpg", firstPetDto.fotoUrl)
    }

    @Test
    fun registerClientParams_toDto_emptyPets_mapsNullMascotas() {
        val params = RegisterClientParams(
            nombres = "Diego Alonso",
            apellidos = "Aparicio Vela",
            correo = "diego.cliente@example.com",
            contrasena = "PaseaPe2026*",
            telefono = "+51981234567",
            direccionReferencia = "Calle Las Begonias 450, Dpto 302",
            distritoId = 31,
            mascotas = emptyList()
        )

        val dto = params.toDto()
        assertNull(dto.mascotas)
    }

    @Test
    fun registerWalkerParams_toDto_mapsAllFieldsCorrectly() {
        val params = RegisterWalkerParams(
            nombres = "Roberto",
            apellidos = "Gómez Salas",
            correo = "roberto.walker@example.com",
            contrasena = "WalkerSecure2026#",
            telefono = "+51944556677",
            tipoDocumentoId = 1,
            numeroDocumento = "45678912",
            antecedentesPolicialesUrl = "https://storage.paseape.com/docs/cert-45678912.pdf",
            experienciaAnos = 4,
            biografia = "Entrenador certificado y paseador enfocado en razas medianas y grandes.",
            tarifaHoraPen = 25.00,
            distritoCoberturaId = 21
        )

        val dto = params.toDto()

        assertEquals(AppConstants.TIPO_USUARIO_PASEADOR_ID, dto.tipoUsuarioId)
        assertEquals("Roberto", dto.nombres)
        assertEquals("Gómez Salas", dto.apellidos)
        assertEquals("roberto.walker@example.com", dto.correo)
        assertEquals("WalkerSecure2026#", dto.contrasena)
        assertEquals("+51944556677", dto.telefono)
        assertEquals(1, dto.tipoDocumentoId)
        assertEquals("45678912", dto.numeroDocumento)
        assertEquals("https://storage.paseape.com/docs/cert-45678912.pdf", dto.antecedentesPolicialesUrl)
        assertEquals(4, dto.experienciaAnos)
        assertEquals("Entrenador certificado y paseador enfocado en razas medianas y grandes.", dto.biografia)
        assertEquals(25.00, dto.tarifaHoraPen!!, 0.001)
        assertEquals(21, dto.distritoCoberturaId)
    }
}
