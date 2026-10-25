package com.example.paseape.data.repository

import android.content.Context
import com.example.paseape.data.model.CrearSolicitudInmediataReqDto
import com.example.paseape.data.model.CrearSolicitudProgramadaReqDto
import com.example.paseape.data.model.PaseoResDto
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PaseosRepository {
    private val apiService = RetrofitClient.apiService

    companion object {
        private const val PREFS_SOLICITUDES = "paseape_solicitudes_cache"
        private const val KEY_LISTA_SOLICITUDES = "solicitudes_creadas_json"
        private val gson = Gson()
        private var appContext: Context? = null

        private val _solicitudesCreadasSesion = MutableStateFlow<List<SolicitudPaseoResDto>>(emptyList())
        val solicitudesCreadasSesion: StateFlow<List<SolicitudPaseoResDto>> = _solicitudesCreadasSesion.asStateFlow()

        fun inicializarCache(context: Context) {
            val appCtx = context.applicationContext
            appContext = appCtx
            if (_solicitudesCreadasSesion.value.isEmpty()) {
                val guardadas = leerDePreferencias(appCtx)
                if (guardadas.isNotEmpty()) {
                    _solicitudesCreadasSesion.value = guardadas
                }
            }
        }

        fun registrarSolicitudCreada(solicitud: SolicitudPaseoResDto) {
            val actual = _solicitudesCreadasSesion.value
            val sinDuplicados = actual.filterNot {
                (!it.uuid.isNullOrBlank() && it.uuid == solicitud.uuid) ||
                    (!it.codigoReserva.isNullOrBlank() && it.codigoReserva == solicitud.codigoReserva)
            }
            val nuevaLista = listOf(solicitud) + sinDuplicados
            _solicitudesCreadasSesion.value = nuevaLista
            appContext?.let { guardarEnPreferencias(it, nuevaLista) }
        }

        private fun guardarEnPreferencias(context: Context, lista: List<SolicitudPaseoResDto>) {
            runCatching {
                val prefs = context.getSharedPreferences(PREFS_SOLICITUDES, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_LISTA_SOLICITUDES, gson.toJson(lista)).apply()
            }
        }

        private fun leerDePreferencias(context: Context): List<SolicitudPaseoResDto> {
            return runCatching {
                val prefs = context.getSharedPreferences(PREFS_SOLICITUDES, Context.MODE_PRIVATE)
                val json = prefs.getString(KEY_LISTA_SOLICITUDES, null) ?: return emptyList()
                val type = object : TypeToken<List<SolicitudPaseoResDto>>() {}.type
                gson.fromJson<List<SolicitudPaseoResDto>>(json, type).orEmpty()
            }.getOrDefault(emptyList())
        }
    }

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
                registrarSolicitudCreada(body.response)
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

    suspend fun crearSolicitudProgramada(
        token: String,
        request: CrearSolicitudProgramadaReqDto
    ): Result<SolicitudPaseoResDto> {
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = apiService.crearSolicitudProgramada(authHeader, request)
            val body = response.body()

            if (response.isSuccessful && body?.response != null) {
                registrarSolicitudCreada(body.response)
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
