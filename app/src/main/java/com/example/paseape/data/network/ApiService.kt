package com.example.paseape.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import com.example.paseape.data.model.PaseadoresResponse
import com.example.paseape.data.model.LoginRequest
import com.example.paseape.data.model.LoginResponse
import com.example.paseape.data.model.RegistroRequest
import com.example.paseape.data.model.GenericResponse
import com.example.paseape.data.model.GoogleAuthReq
import com.example.paseape.data.model.GoogleAuthResponse
import com.example.paseape.data.model.RegistroGoogleRequest
import com.example.paseape.data.model.UserProfileResponse

interface ApiService {

    @GET("paseadores")
    suspend fun obtenerPaseadores(): PaseadoresResponse

    @POST("auth/register/local")
    suspend fun registrarUsuario(
        @Body request: RegistroRequest
    ): GenericResponse

    @POST("auth/login")
    suspend fun iniciarSesion(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("auth/logout")
    suspend fun cerrarSesion(
        @Header("Authorization") token: String
    ): GenericResponse

    @POST("auth/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleAuthReq
    ): GoogleAuthResponse

    @GET("clientes/perfil")
    suspend fun obtenerPerfilCliente(
        @Header("Authorization") token: String? = null
    ): UserProfileResponse

    @POST("auth/register/google")
    suspend fun registrarConGoogle(
        @Body request: RegistroGoogleRequest
    ): GenericResponse
}