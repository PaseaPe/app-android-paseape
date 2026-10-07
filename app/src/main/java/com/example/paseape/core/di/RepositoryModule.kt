package com.example.paseape.core.di

import com.example.paseape.data.repository.AuthRepositoryImpl
import com.example.paseape.data.repository.ParameterRepositoryImpl
import com.example.paseape.data.repository.PaseadorRepositoryImpl
import com.example.paseape.domain.repository.AuthRepository
import com.example.paseape.domain.repository.ParameterRepository
import com.example.paseape.domain.repository.PaseadorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindParameterRepository(
        parameterRepositoryImpl: ParameterRepositoryImpl
    ): ParameterRepository

    @Binds
    @Singleton
    abstract fun bindPaseadorRepository(
        paseadorRepositoryImpl: PaseadorRepositoryImpl
    ): PaseadorRepository
}
