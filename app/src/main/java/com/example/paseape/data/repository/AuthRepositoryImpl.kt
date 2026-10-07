package com.example.paseape.data.repository

import com.example.paseape.data.local.session.SessionManager
import com.example.paseape.data.remote.api.ApiService
import com.example.paseape.data.remote.dto.auth.LoginReqDto
import com.example.paseape.data.remote.dto.auth.LoginResDto
import com.example.paseape.data.remote.dto.auth.RegistroReqDto
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

    override suspend fun register(
        nombres: String,
        apellidos: String,
        correo: String,
        contrasena: String,
        tipoUsuarioId: Int
    ): Result<Unit> {
        return try {
            val request = RegistroReqDto(
                nombres = nombres.trim(),
                apellidos = apellidos.trim(),
                correo = correo.trim(),
                contrasena = contrasena,
                tipoUsuarioId = tipoUsuarioId
            )
            val response = apiService.register(request)
            if (response.statusCode in 200..299 || response.code == "200" || response.message?.contains("éxito", ignoreCase = true) == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Error al registrar usuario"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
