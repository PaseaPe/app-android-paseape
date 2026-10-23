package com.example.paseape.data.repository

import com.example.paseape.data.model.PaseoResDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient

class PaseosRepository {
    private val apiService = RetrofitClient.apiService


    suspend fun obtenerPaseos(token: String, clienteId: Long): Result<List<PaseoResDto>> {
        return try {
            val response = apiService.obtenerPaseosPorCliente("Bearer $token", clienteId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception(ApiErrorMapper.fromResponse(response, ApiErrorContext.WALK))
                )
            }
        } catch (e: Exception) {
            Result.failure(
                Exception(ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK))
            )
        }
    }
}