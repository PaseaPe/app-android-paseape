package com.example.paseape.data.repository

import com.example.paseape.core.util.AppConstants
import com.example.paseape.data.local.session.SessionManager
import com.example.paseape.data.remote.api.ApiService
import com.example.paseape.data.remote.dto.auth.LoginReqDto
import com.example.paseape.data.remote.dto.auth.LoginResDto
import com.example.paseape.data.remote.dto.auth.RegistroReqDto
import com.example.paseape.data.remote.mapper.toDto
import com.example.paseape.domain.model.RegisterClientParams
import com.example.paseape.domain.model.RegisterWalkerParams
import com.example.paseape.domain.model.User
import com.example.paseape.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(correo: String, contrasena: String): Result<User> {
        return try {
            val request = LoginReqDto(correo = correo.trim(), contrasena = contrasena)
            val response = apiService.login(request)
            val loginData = response.response

            if (loginData != null && !loginData.token.isNullOrBlank()) {
                val user = loginData.toDomain(fallbackEmail = correo)
                sessionManager.saveUserSession(
                    token = loginData.token,
                    id = user.id,
                    nombre = "${user.nombre} ${user.apellido}".trim(),
                    email = user.correo,
                    rol = user.rol,
                    tipoUsuarioId = loginData.tipoUsuarioId
                )
                Result.success(user)
            } else {
                val errorMsg = response.message ?: "Credenciales incorrectas"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerClient(params: RegisterClientParams): Result<Unit> {
        return runCatching {
            val dto = params.toDto()
            val response = if (params.idTokenGoogle != null) {
                apiService.registrarUsuarioGoogle(dto)
            } else {
                apiService.registrarUsuarioLocal(dto)
            }

            if (response.code != AppConstants.CODE_SUCCESS) {
                throw Exception(response.message)
            }
        }
    }

    override suspend fun registerWalker(params: RegisterWalkerParams): Result<Unit> {
        return runCatching {
            val dto = params.toDto()
            val response = if (params.idTokenGoogle != null) {
                apiService.registrarUsuarioGoogle(dto)
            } else {
                apiService.registrarUsuarioLocal(dto)
            }

            if (response.code != AppConstants.CODE_SUCCESS) {
                throw Exception(response.message)
            }
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            try {
                apiService.logout()
            } catch (_: Exception) {
                // Aún si la llamada de red falla, limpiamos la sesión local
            }
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.failure(e)
        }
    }

    override fun getAuthToken(): Flow<String?> = sessionManager.authToken
    override fun getUserRole(): Flow<String?> = sessionManager.userRole
    override fun getUserEmail(): Flow<String?> = sessionManager.userEmail
    override fun getUserName(): Flow<String?> = sessionManager.userName

    private fun LoginResDto.toDomain(fallbackEmail: String): User {
        return User(
            id = this.idUsuario ?: 0,
            nombre = this.nombre.orEmpty(),
            apellido = this.apellido.orEmpty(),
            correo = this.correo ?: fallbackEmail,
            rol = this.rol.orEmpty(),
            token = this.token,
            fotoPerfil = this.fotoPerfil
        )
    }
}
