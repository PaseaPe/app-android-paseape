package com.example.paseape.domain.usecase.parameter

import com.example.paseape.domain.model.CatalogItem
import com.example.paseape.domain.repository.ParameterRepository
import javax.inject.Inject

class GetRazasByTipoUseCase @Inject constructor(
    private val parameterRepository: ParameterRepository
) {
    suspend operator fun invoke(tipoMascotaId: Int): Result<List<CatalogItem>> {
        return parameterRepository.getRazasByTipo(tipoMascotaId)
    }
}
