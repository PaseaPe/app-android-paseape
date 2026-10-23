package com.example.paseape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.PaseadorDto
import com.example.paseape.data.model.UsuarioPaseadorDetalleDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PaseadoresListUiState {
    data object Loading : PaseadoresListUiState
    data class Success(
        val paseadores: List<PaseadorDto>,
        val distritosDisponibles: List<String>,
        val distritoSeleccionado: String?
    ) : PaseadoresListUiState
    data class Error(val message: String) : PaseadoresListUiState
}

sealed interface PaseadorDetalleUiState {
    data object Loading : PaseadorDetalleUiState
    data class Success(val detalle: UsuarioPaseadorDetalleDto) : PaseadorDetalleUiState
    data class Error(val message: String) : PaseadorDetalleUiState
}

class PaseadoresViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PaseadoresListUiState>(PaseadoresListUiState.Loading)
    val uiState: StateFlow<PaseadoresListUiState> = _uiState.asStateFlow()

    private var listaCompletaPaseadores: List<PaseadorDto> = emptyList()
    var distritoSeleccionadoActual by mutableStateOf<String?>(null) // null representa el filtro "Todos"

    fun cargarPaseadores(authHeader: String) {
        _uiState.value = PaseadoresListUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getPaseadores(token = authHeader)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        listaCompletaPaseadores = body?.response.orEmpty()

                        // Extraer distritos de cobertura únicos para el selector de la UI
                        val distritos = listaCompletaPaseadores
                            .mapNotNull { it.distritoCoberturaDescripcion }
                            .distinct()
                            .sorted()

                        actualizarEstadoConFiltro(distritos)
                    } else {
                        _uiState.value = PaseadoresListUiState.Error(
                            ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = PaseadoresListUiState.Error(
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
                    )
                }
            }
        }
    }

    fun filtrarPorDistrito(distrito: String?) {
        distritoSeleccionadoActual = if (distrito.equals("Todos", ignoreCase = true)) null else distrito
        val distritos = listaCompletaPaseadores
            .mapNotNull { it.distritoCoberturaDescripcion }
            .distinct()
            .sorted()
        actualizarEstadoConFiltro(distritos)
    }

    private fun actualizarEstadoConFiltro(distritosDisponibles: List<String>) {
        val filtrados = if (distritoSeleccionadoActual.isNullOrBlank()) {
            listaCompletaPaseadores
        } else {
            listaCompletaPaseadores.filter {
                it.distritoCoberturaDescripcion.equals(distritoSeleccionadoActual, ignoreCase = true)
            }
        }
        _uiState.value = PaseadoresListUiState.Success(
            paseadores = filtrados,
            distritosDisponibles = distritosDisponibles,
            distritoSeleccionado = distritoSeleccionadoActual
        )
    }
}

class PaseadorDetalleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PaseadorDetalleUiState>(PaseadorDetalleUiState.Loading)
    val uiState: StateFlow<PaseadorDetalleUiState> = _uiState.asStateFlow()

    fun cargarDetalle(authHeader: String, usuarioUuid: String) {
        if (usuarioUuid.isBlank()) {
            _uiState.value = PaseadorDetalleUiState.Error("No se encontró el identificador del paseador.")
            return
        }
        _uiState.value = PaseadorDetalleUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getPaseadorPorUsuarioUuid(
                    token = authHeader,
                    usuarioUuid = usuarioUuid
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val detalle = body?.response
                        if (detalle != null) {
                            _uiState.value = PaseadorDetalleUiState.Success(detalle)
                        } else {
                            _uiState.value = PaseadorDetalleUiState.Error("No se encontraron los datos del paseador.")
                        }
                    } else {
                        _uiState.value = PaseadorDetalleUiState.Error(
                            ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = PaseadorDetalleUiState.Error(
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
                    )
                }
            }
        }
    }
}