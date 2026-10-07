package com.example.paseape.domain.usecase.auth

import com.example.paseape.domain.model.RegisterClientParams
import com.example.paseape.domain.model.RegisterGoogleClientParams
import com.example.paseape.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterGoogleClientUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(params: RegisterGoogleClientParams): Result<Unit> {
        return authRepository.registerGoogleClient(params)
    }
}