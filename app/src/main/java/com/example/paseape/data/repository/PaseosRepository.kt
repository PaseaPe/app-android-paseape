package com.example.paseape.data.repository

import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.data.model.PaseoResDto

class PaseosRepository {
    private val apiService = RetrofitClient.apiService


    suspend fun obtenerPaseos(token: String, clienteId: Long): Result<List<PaseoResDto>> {
        return try {
            val response = apiService.obtenerPaseosPorCliente("Bearer $token", clienteId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener paseos: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}