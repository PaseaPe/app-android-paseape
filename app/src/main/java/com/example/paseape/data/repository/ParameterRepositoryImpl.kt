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
        return runCatching {
            val response = apiService.getApplicationParameters()
            val data = response.response ?: throw Exception(response.message ?: "Error al obtener parámetros")
            data.toDomain()
        }
    }

    override suspend fun getRazasByTipo(tipoMascotaId: Int): Result<List<CatalogItem>> {
        return runCatching {
            val response = apiService.getRazasByTipo(tipoMascotaId)
            response.response.orEmpty().map { it.toDomain() }
        }
    }

    private fun ParameterItemDto.toDomain(): CatalogItem = CatalogItem(id = this.id, descripcion = this.descripcion)

    private fun ApplicationParametersResDto.toDomain(): ApplicationParameters = ApplicationParameters(
        tiposMascota = this.tiposMascota.map { it.toDomain() },
        distritos = this.distritos.map { it.toDomain() },
        tiposUsuario = this.tiposUsuario.map { it.toDomain() },
        tiposDocumento = this.tiposDocumento.map { it.toDomain() },
        generosMascota = this.generosMascota.map { it.toDomain() },
        tamanosMascota = this.tamanosMascota.map { it.toDomain() },
        nivelesEnergia = this.nivelesEnergia.map { it.toDomain() }
    )
}
