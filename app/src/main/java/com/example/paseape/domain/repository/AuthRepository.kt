package com.example.paseape.domain.repository

import com.example.paseape.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(correo: String, contrasena: String): Result<User>
    suspend fun register(
        nombres: String,
        apellidos: String,
        correo: String,
        contrasena: String,
        tipoUsuarioId: Int
    ): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun getAuthToken(): Flow<String?>
    fun getUserRole(): Flow<String?>
    fun getUserEmail(): Flow<String?>
    fun getUserName(): Flow<String?>
}
