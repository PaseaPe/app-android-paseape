package com.example.paseape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.DisponibilidadPaseadorDto
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
    data class Success(
        val detalle: UsuarioPaseadorDetalleDto,
        val disponibilidad: DisponibilidadPaseadorDto? = null
    ) : PaseadorDetalleUiState
    data class Error(val message: String) : PaseadorDetalleUiState
}

class PaseadoresViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PaseadoresListUiState>(PaseadoresListUiState.Loading)
    val uiState: StateFlow<PaseadoresListUiState> = _uiState.asStateFlow()

    private var catalogoDistritos: List<DistritoParamDto> = emptyList()
    private var listaActualPaseadores: List<PaseadorDto> = emptyList()
    private var ultimoAuthHeader: String = ""
    var distritoSeleccionadoActual by mutableStateOf<String?>(null)

    fun cargarPaseadores(authHeader: String) {
        ultimoAuthHeader = authHeader
        _uiState.value = PaseadoresListUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Cargar catálogo de distritos desde application-parameters si aún no existe
                if (catalogoDistritos.isEmpty()) {
                    try {
                        val paramsRes = RetrofitClient.apiService.getApplicationParameters(authHeader)
                        if (paramsRes.isSuccessful) {
                            catalogoDistritos = paramsRes.body()?.response?.distritos.orEmpty()
                        }
                    } catch (_: Exception) {
                        // Fallback a los distritos obtenidos en la lista si falla el catálogo
                    }
                }

                // 2. Consulta al backend según el filtro seleccionado
                val response = if (distritoSeleccionadoActual == null) {
                    // Inicial: sin query params (el backend resuelve automáticamente el distrito del cliente en sesión)
                    RetrofitClient.apiService.getPaseadores(token = authHeader)
                } else if (distritoSeleccionadoActual.equals("Todos", ignoreCase = true)) {
                    // "Todos los distritos": enviamos todos los IDs del catálogo para que el backend busque globalmente
                    val todosIds = catalogoDistritos.map { it.id }.filter { it > 0 }
                    RetrofitClient.apiService.getPaseadores(
                        token = authHeader,
                        distritoIds = if (todosIds.isNotEmpty()) todosIds else null
                    )
                } else {
                    // Distrito específico seleccionado en el menú desplegable
                    val distritoObj = catalogoDistritos.firstOrNull {
                        it.descripcion.equals(distritoSeleccionadoActual, ignoreCase = true)
                    }
                    if (distritoObj != null) {
                        RetrofitClient.apiService.getPaseadores(
                            token = authHeader,
                            distritoId = distritoObj.id
                        )
                    } else {
                        RetrofitClient.apiService.getPaseadores(token = authHeader)
                    }
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        listaActualPaseadores = body?.response.orEmpty()

                        // Generar lista de distritos para el desplegable (todos los distritos disponibles)
                        val nombresDistritos = if (catalogoDistritos.isNotEmpty()) {
                            catalogoDistritos.map { it.descripcion.trim().uppercase() }.distinct().sorted()
                        } else {
                            listaActualPaseadores.mapNotNull { it.distritoCoberturaDescripcion?.trim()?.uppercase() }.distinct().sorted()
                        }

                        // Resolver el nombre del distrito a mostrar en el selector
                        val nombreMostrar = when {
                            distritoSeleccionadoActual.equals("Todos", ignoreCase = true) -> null
                            !distritoSeleccionadoActual.isNullOrBlank() -> distritoSeleccionadoActual?.trim()?.uppercase()
                            listaActualPaseadores.isNotEmpty() -> listaActualPaseadores.firstOrNull()?.distritoCoberturaDescripcion?.trim()?.uppercase()
                            else -> null
                        }

                        _uiState.value = PaseadoresListUiState.Success(
                            paseadores = listaActualPaseadores,
                            distritosDisponibles = nombresDistritos,
                            distritoSeleccionado = nombreMostrar
                        )
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
        distritoSeleccionadoActual = if (distrito.isNullOrBlank() || distrito.equals("Todos", ignoreCase = true)) {
            "Todos"
        } else {
            distrito
        }
        if (ultimoAuthHeader.isNotBlank()) {
            cargarPaseadores(ultimoAuthHeader)
        }
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
                        val detalleData = body?.response
                        val usuarioDetalle = detalleData?.usuario
                        val disponibilidad = detalleData?.disponibilidad
                        if (usuarioDetalle != null) {
                            _uiState.value = PaseadorDetalleUiState.Success(
                                detalle = usuarioDetalle,
                                disponibilidad = disponibilidad
                            )
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
