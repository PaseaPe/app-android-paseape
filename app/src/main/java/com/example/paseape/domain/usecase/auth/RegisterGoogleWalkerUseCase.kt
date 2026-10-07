package com.example.paseape.domain.usecase.auth

import com.example.paseape.domain.model.RegisterGoogleWalkerParams
import com.example.paseape.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterGoogleWalkerUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(params: RegisterGoogleWalkerParams): Result<Unit> {
        return authRepository.registerGoogleWalker(params)
    }
}