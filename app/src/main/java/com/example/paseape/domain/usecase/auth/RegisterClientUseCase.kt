package com.example.paseape.domain.usecase.auth

import com.example.paseape.domain.model.RegisterClientParams
import com.example.paseape.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterClientUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(params: RegisterClientParams): Result<Unit> {
        return authRepository.registerClient(params)
    }
}