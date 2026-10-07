package com.example.paseape.domain.usecase.auth

import com.example.paseape.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        nombres: String,
        apellidos: String,
        correo: String,
        contrasena: String,
        tipoUsuarioId: Int
    ): Result<Unit> {
        return authRepository.register(nombres, apellidos, correo, contrasena, tipoUsuarioId)
    }
}
