package com.example.paseape.domain.repository

import com.example.paseape.domain.model.ApplicationParameters
import com.example.paseape.domain.model.CatalogItem

interface ParameterRepository {
    suspend fun getApplicationParameters(): Result<ApplicationParameters>
    suspend fun getRazasByTipo(tipoMascotaId: Int): Result<List<CatalogItem>>
}
