package com.example.paseape.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import com.example.paseape.data.model.PaseadoresResponse

data class RegistroRequest(
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val contrasena: String,
    val tipo_usuario_id: Int // 1 = Cliente, 2 = Paseador
)

data class LoginRequest(
    val correo: String,
    val contrasena: String
)

data class MascotaRequest(
    val cliente_id: Int,
    val nombre: String,
    val tipo_mascota_id: Int,
    val tipo_raza_id: Int,
    val peso_kg: Double,
    val esterilizado: Int
)

data class ZonaTrabajoRequest(
    val paseador_id: Int,
    val distrito_cobertura_id: Int
)
interface ApiService {

    @GET("paseadores")
    suspend fun obtenerPaseadores(): PaseadoresResponse

    // HU01 – Registrarse como usuario (Local)
    @POST("auth/register/local")
    suspend fun registrarUsuario(
        @Body request: RegistroRequest
    ): GenericResponse

    // HU02 – Iniciar sesión
    @POST("auth/login")
    suspend fun iniciarSesion(
        @Body request: LoginRequest
    ): LoginResponse

    // HU38 – Cerrar sesión
    @POST("auth/logout")
    suspend fun cerrarSesion(
        @Header("Authorization") token: String
    ): GenericResponse

    @POST("auth/reset-password")
    suspend fun recuperarPassword(
        @Body request: Map<String, String>
    ): GenericResponse

    // HU04 – Registrar una mascota
    @POST("mascotas")
    suspend fun registrarMascota(
        @Body request: MascotaRequest
    ): GenericResponse

    // HU08 – Establecer zona de trabajo / distrito de cobertura
    @POST("paseadores/distrito-cobertura")
    suspend fun establecerZonaTrabajo(
        @Body request: ZonaTrabajoRequest
    ): GenericResponse
}


data class GenericResponse(
    val success: Boolean,
    val message: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val message: String
)