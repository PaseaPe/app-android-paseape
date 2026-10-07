package com.example.paseape.core.network

sealed interface ApiResponse<out T> {
    data class Success<out T>(val data: T) : ApiResponse<T>
    data class Error(val message: String, val statusCode: Int? = null) : ApiResponse<Nothing>
    data object Loading : ApiResponse<Nothing>
}
