package com.example.paseape.data.repository

import com.example.paseape.data.model.CrearSolicitudInmediataReqDto
import com.example.paseape.data.model.PaseoResDto
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient

class PaseosRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun obtenerPaseos(token: String, clienteId: Long): Result<List<PaseoResDto>> {
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = apiService.obtenerPaseosPorCliente(authHeader, clienteId)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(
                    Exception(ApiErrorMapper.fromResponse(response, ApiErrorContext.WALK))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK)))
        }
    }

    suspend fun crearSolicitudInmediata(
        token: String,
        request: CrearSolicitudInmediataReqDto
    ): Result<SolicitudPaseoResDto> {
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = apiService.crearSolicitudInmediata(authHeader, request)
            val body = response.body()

            if (response.isSuccessful && body?.response != null) {
                Result.success(body.response)
            } else if (body != null && !body.message.isNullOrBlank()) {
                Result.failure(Exception(body.message))
            } else {
                Result.failure(
                    Exception(ApiErrorMapper.fromResponse(response, ApiErrorContext.WALK))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK)))
        }
    }
}
