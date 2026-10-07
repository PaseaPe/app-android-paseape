package com.example.paseape.domain.usecase.paseador

import com.example.paseape.domain.model.Paseador
import com.example.paseape.domain.repository.PaseadorRepository
import javax.inject.Inject

class GetPaseadoresUseCase @Inject constructor(
    private val paseadorRepository: PaseadorRepository
) {
    suspend operator fun invoke(): Result<List<Paseador>> {
        return paseadorRepository.getPaseadores()
    }
}
