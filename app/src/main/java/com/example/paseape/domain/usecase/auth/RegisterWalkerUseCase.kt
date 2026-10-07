package com.example.paseape.domain.usecase.auth

import com.example.paseape.domain.model.RegisterWalkerParams
import com.example.paseape.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterWalkerUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(params: RegisterWalkerParams): Result<Unit> {
        return authRepository.registerWalker(params)
    }
}