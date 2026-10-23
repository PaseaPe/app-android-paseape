package com.example.paseape.data.network

import LogoutResponseDto
import com.example.paseape.data.model.ActualizarEstadoUsuarioRequest
import com.example.paseape.data.model.ClienteAdminDto
import com.example.paseape.data.model.ForgotPasswordRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import com.example.paseape.data.model.PaseadoresResponse
import com.example.paseape.data.model.LoginRequest
import com.example.paseape.data.model.AuthResData
import com.example.paseape.data.model.LoginResponse
import com.example.paseape.data.model.RegistroRequest
import com.example.paseape.data.model.GenericResponse
import com.example.paseape.data.model.GoogleAuthReq
import com.example.paseape.data.model.GoogleAuthResponse
import com.example.paseape.data.model.PaseadorAdminDto
import com.example.paseape.data.model.RegistroGoogleRequest
import com.example.paseape.data.model.ResetPasswordRequest
import com.example.paseape.data.model.ResponseDto
import com.example.paseape.data.model.UserProfileResponse
import com.example.paseape.data.model.ClienteProfileResponse
import com.example.paseape.data.model.ActualizarPerfilReqDto
import com.example.paseape.data.model.CrearMascotaRequest
import com.example.paseape.data.model.DetalleMascotaResponse
import com.example.paseape.data.model.ActualizarMascotaRequest
import com.example.paseape.data.model.PaseadoresListResponse
import com.example.paseape.data.model.DetallePaseadorResponse
import com.example.paseape.data.model.UpdateProfileRequestDto
import com.example.paseape.data.model.UpdateCoverageRequestDto
import com.example.paseape.data.model.ApplicationParametersResponse
import com.example.paseape.data.model.ActualizarBloqueDisponibilidadRequestDto
import com.example.paseape.data.model.ActualizarCapacidadBaseRequestDto
import com.example.paseape.data.model.DisponibilidadAccionResponseDto
import com.example.paseape.data.model.DisponibilidadResponseDto
import com.example.paseape.data.model.GuardarDisponibilidadRequestDto
import com.example.paseape.data.model.RazasResponse
import com.example.paseape.data.model.ActualizarVerificacionPaseadorRequest
import com.example.paseape.data.model.GenericApiResponse
import com.example.paseape.data.model.NotificacionDto
import com.example.paseape.data.model.PaseoResDto
import retrofit2.Response
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("cliente/{uuid}")
    suspend fun getClientePorUuid(
        @Header("Authorization") token: String,
        @Path("uuid") uuid: String
    ): Response<ClienteProfileResponse>

    @PUT("auth/profile")
    suspend fun actualizarPerfil(
        @Header("Authorization") token: String,
        @Body request: ActualizarPerfilReqDto
    ): Response<GenericResponse>

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequestDto
    ): Response<GenericResponse>

    @POST("mascotas")
    suspend fun crearMascota(
        @Header("Authorization") token: String,
        @Body request: CrearMascotaRequest
    ): Response<GenericResponse>

    @GET("mascotas/mascota/{uuid}")
    suspend fun getMascotaPorUuid(
        @Header("Authorization") token: String,
        @Path("uuid") uuid: String
    ): Response<DetalleMascotaResponse>

    @PUT("mascotas/{uuid}")
    suspend fun actualizarMascota(
        @Header("Authorization") token: String,
        @Path("uuid") uuid: String,
        @Body request: ActualizarMascotaRequest
    ): Response<GenericResponse>

    @GET("paseadores")
    suspend fun getPaseadores(
        @Header("Authorization") token: String
    ): Response<PaseadoresListResponse>

    @GET("paseadores/{usuario_uuid}")
    suspend fun getPaseadorPorUsuarioUuid(
        @Header("Authorization") token: String,
        @Path("usuario_uuid") usuarioUuid: String
    ): Response<DetallePaseadorResponse>

    @GET("paseadores/{uuid}")
    suspend fun getWalkerDetail(
        @Header("Authorization") token: String,
        @Path("uuid") userUuid: String
    ): Response<DetallePaseadorResponse>

    @PUT("paseadores/distrito-cobertura")
    suspend fun updateWalkerCoverage(
        @Header("Authorization") token: String,
        @Body request: UpdateCoverageRequestDto
    ): Response<GenericResponse>

    @GET("application-parameters")
    suspend fun getApplicationParameters(
        @Header("Authorization") token: String? = null
    ): Response<ApplicationParametersResponse>

    @GET("application-parameters/razas/{tipoMascotaId}")
    suspend fun getRazasPorTipo(
        @Header("Authorization") token: String? = null,
        @Path("tipoMascotaId") tipoMascotaId: Int
    ): Response<RazasResponse>

    /**
     * Catálogos de días y bloques horarios. El backend actual los expone en
     * /application-parameters/horario; se usa como respaldo de compatibilidad.
     */
    @GET("application-parameters/horario")
    suspend fun getHorarioParameters(
        @Header("Authorization") token: String? = null
    ): Response<ApplicationParametersResponse>

    // Disponibilidad semanal del paseador
    @GET("paseador/disponibilidad")
    suspend fun getMiDisponibilidad(
        @Header("Authorization") token: String
    ): Response<DisponibilidadResponseDto>

    @PUT("paseador/disponibilidad")
    suspend fun sincronizarDisponibilidad(
        @Header("Authorization") token: String,
        @Body request: GuardarDisponibilidadRequestDto
    ): Response<DisponibilidadResponseDto>

    @PATCH("paseador/disponibilidad/bloque")
    suspend fun actualizarBloqueDisponibilidad(
        @Header("Authorization") token: String,
        @Body request: ActualizarBloqueDisponibilidadRequestDto
    ): Response<DisponibilidadAccionResponseDto>

    @PATCH("paseador/disponibilidad/capacidad-base")
    suspend fun actualizarCapacidadBase(
        @Header("Authorization") token: String,
        @Body request: ActualizarCapacidadBaseRequestDto
    ): Response<DisponibilidadAccionResponseDto>

    @GET("paseadores")
    suspend fun obtenerPaseadores(): PaseadoresResponse

    @POST("auth/register/local")
    suspend fun registrarUsuario(
        @Body request: RegistroRequest
    ): Response<ResponseDto<AuthResData>>

    @POST("auth/login")
    suspend fun iniciarSesion(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<LogoutResponseDto>

    @POST("auth/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleAuthReq
    ): GoogleAuthResponse

    @POST("auth/register/google")
    suspend fun registrarConGoogle(
        @Body request: RegistroGoogleRequest
    ): GenericResponse

    @Headers("Content-Type: application/json")
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Void>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Void>

    @GET("admin/paseadores")
    suspend fun listarPaseadoresAdmin(
        @Header("Authorization") token: String
    ): Response<ResponseDto<List<PaseadorAdminDto>>>

    @GET("admin/usuarios")
    suspend fun listarClientesAdmin(
        @Header("Authorization") token: String,
        @Query("tipo_usuario_id") tipoUsuarioId: Int? = 1
    ): Response<ResponseDto<List<ClienteAdminDto>>>

    @PUT("admin/usuarios/{usuario_uuid}/estado")
    suspend fun actualizarEstadoUsuario(
        @Header("Authorization") token: String,
        @Path("usuario_uuid") usuarioUuid: String,
        @Body request: ActualizarEstadoUsuarioRequest
    ): Response<GenericApiResponse>

    @PUT("admin/paseadores/{paseador_uuid}/verificacion")
    suspend fun actualizarVerificacionPaseador(
        @Header("Authorization") token: String,
        @Path("paseador_uuid") paseadorUuid: String,
        @Body request: ActualizarVerificacionPaseadorRequest
    ): Response<GenericApiResponse>

    // Métodos de paseos y notificaciones integrados directamente en ApiService
    @GET("paseos/cliente/{clienteId}")
    suspend fun obtenerPaseosPorCliente(
        @Header("Authorization") token: String,
        @Path("clienteId") clienteId: Long
    ): Response<List<PaseoResDto>>

    @GET("notificaciones/usuario/{usuarioId}")
    suspend fun obtenerNotificaciones(
        @Header("Authorization") token: String,
        @Path("usuarioId") usuarioId: Long
    ): Response<List<NotificacionDto>>

}