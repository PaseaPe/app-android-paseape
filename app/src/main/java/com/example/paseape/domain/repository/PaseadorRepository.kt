package com.example.paseape.domain.repository

import com.example.paseape.domain.model.Paseador

interface PaseadorRepository {
    suspend fun getPaseadores(): Result<List<Paseador>>
    suspend fun updateDistritoCobertura(paseadorId: Int, distritoId: Int): Result<Unit>
}
