package com.example.paseape.data.repository

import com.example.paseape.data.remote.api.ApiService
import com.example.paseape.data.remote.dto.paseador.CoberturaReqDto
import com.example.paseape.data.remote.dto.paseador.PaseadorDto
import com.example.paseape.domain.model.Paseador
import com.example.paseape.domain.repository.PaseadorRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaseadorRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : PaseadorRepository {

    override suspend fun getPaseadores(): Result<List<Paseador>> {
        return try {
            val response = apiService.getPaseadores()
            val dtoList = response.response.orEmpty()
            val domainList = dtoList.map { it.toDomain() }
            Result.success(domainList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDistritoCobertura(paseadorId: Int, distritoId: Int): Result<Unit> {
        return try {
            val request = CoberturaReqDto(
                paseadorId = paseadorId,
                distritoCoberturaId = distritoId
            )
            val response = apiService.updateDistritoCobertura(request)
            if (response.statusCode in 200..299 || response.code == "200" || response.message?.contains("éxito", ignoreCase = true) == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Error al actualizar cobertura"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun PaseadorDto.toDomain(): Paseador {
        return Paseador(
            idPaseador = this.idPaseador ?: 0,
            idUsuario = this.idUsuario ?: 0,
            nombre = this.nombre,
            apellido = this.apellido,
            telefono = this.telefono,
            distrito = this.distrito,
            tarifaHora = this.tarifaHora,
            calificacion = this.calificacion,
            fotoPerfil = this.fotoPerfil,
            descripcion = this.descripcion,
            experiencia = this.experiencia
        )
    }
}
