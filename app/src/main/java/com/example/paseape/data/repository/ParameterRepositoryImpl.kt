package com.example.paseape.data.repository

import com.example.paseape.data.remote.api.ApiService
import com.example.paseape.data.remote.dto.parameter.ApplicationParametersResDto
import com.example.paseape.data.remote.dto.parameter.ParameterItemDto
import com.example.paseape.domain.model.ApplicationParameters
import com.example.paseape.domain.model.CatalogItem
import com.example.paseape.domain.repository.ParameterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParameterRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ParameterRepository {

    override suspend fun getApplicationParameters(): Result<ApplicationParameters> {
        return try {
            val response = apiService.getApplicationParameters()
            val data = response.response
            if (data != null) {
                Result.success(data.toDomain())
            } else {
                Result.failure(Exception(response.message ?: "No se obtuvieron parámetros"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRazasByTipo(tipoMascotaId: Int): Result<List<CatalogItem>> {
        return try {
            val response = apiService.getRazasByTipo(tipoMascotaId)
            val list = response.response.orEmpty().map { it.toDomain() }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ParameterItemDto.toDomain(): CatalogItem {
        return CatalogItem(
            id = this.id,
            descripcion = this.descripcion
        )
    }

    private fun ApplicationParametersResDto.toDomain(): ApplicationParameters {
        return ApplicationParameters(
            tiposMascota = this.tiposMascota.map { it.toDomain() },
            distritos = this.distritos.map { it.toDomain() },
            tiposUsuario = this.tiposUsuario.map { it.toDomain() }
        )
    }
}
