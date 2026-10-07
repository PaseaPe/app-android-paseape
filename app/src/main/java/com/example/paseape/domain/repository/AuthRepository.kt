package com.example.paseape.domain.repository

import com.example.paseape.domain.model.RegisterClientParams
import com.example.paseape.domain.model.RegisterGoogleClientParams
import com.example.paseape.domain.model.RegisterGoogleWalkerParams
import com.example.paseape.domain.model.RegisterWalkerParams
import com.example.paseape.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(correo: String, contrasena: String): Result<User>
    suspend fun registerClient(params: RegisterClientParams): Result<Unit>
    suspend fun registerWalker(params: RegisterWalkerParams): Result<Unit>
    suspend fun registerGoogleClient(params: RegisterGoogleClientParams): Result<Unit>
    suspend fun registerGoogleWalker(params: RegisterGoogleWalkerParams): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun getAuthToken(): Flow<String?>
    fun getUserRole(): Flow<String?>
    fun getUserEmail(): Flow<String?>
    fun getUserName(): Flow<String?>
}
