package com.example.paseape.domain.usecase.auth

import com.example.paseape.domain.model.User
import com.example.paseape.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(correo: String, contrasena: String): Result<User> {
        return authRepository.login(correo, contrasena)
    }
}
