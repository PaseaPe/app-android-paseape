package com.example.paseape.domain.usecase.parameter

import com.example.paseape.domain.model.ApplicationParameters
import com.example.paseape.domain.repository.ParameterRepository
import javax.inject.Inject

class GetApplicationParametersUseCase @Inject constructor(
    private val parameterRepository: ParameterRepository
) {
    suspend operator fun invoke(): Result<ApplicationParameters> {
        return parameterRepository.getApplicationParameters()
    }
}
