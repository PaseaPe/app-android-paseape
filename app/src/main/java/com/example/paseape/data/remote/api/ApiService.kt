package com.example.paseape.data.remote.api

import com.example.paseape.data.remote.dto.auth.LoginReqDto
import com.example.paseape.data.remote.dto.auth.LoginResDto
import com.example.paseape.data.remote.dto.auth.RegistroReqDto
import com.example.paseape.data.remote.dto.auth.UsuarioReqDto
import com.example.paseape.data.remote.dto.common.ResponseDto
import com.example.paseape.data.remote.dto.mascota.MascotaReqDto
import com.example.paseape.data.remote.dto.parameter.ApplicationParametersResDto
import com.example.paseape.data.remote.dto.parameter.ParameterItemDto
import com.example.paseape.data.remote.dto.paseador.CoberturaReqDto
import com.example.paseape.data.remote.dto.paseador.PaseadorDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginReqDto
    ): ResponseDto<LoginResDto>

    @POST("auth/register/local")
    suspend fun registrarUsuarioLocal(
        @Body request: UsuarioReqDto
    ): ResponseDto<Unit>

    @POST("auth/register/google")
    suspend fun registrarUsuarioGoogle(
        @Body request: UsuarioReqDto
    ): ResponseDto<Unit>

    @POST("auth/logout")
    suspend fun logout(): ResponseDto<Unit>

    @GET("application-parameters")
    suspend fun getApplicationParameters(): ResponseDto<ApplicationParametersResDto>

    @GET("application-parameters/razas/{tipo_mascota_id}")
    suspend fun getRazasByTipo(
        @Path("tipo_mascota_id") tipoMascotaId: Int
    ): ResponseDto<List<ParameterItemDto>>

    @GET("paseadores")
    suspend fun getPaseadores(): ResponseDto<List<PaseadorDto>>

    @PUT("paseadores/distrito-cobertura")
    suspend fun updateDistritoCobertura(
        @Body request: CoberturaReqDto
    ): ResponseDto<Unit>

    @POST("mascotas")
    suspend fun registerMascota(
        @Body request: MascotaReqDto
    ): ResponseDto<Unit>
}
