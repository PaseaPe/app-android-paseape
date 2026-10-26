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
        val distritoSeleccionado: String?,
        val distritoClienteDefault: String? = null
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
    private var ultimoUserUuid: String? = null
    private var distritoClienteDefault: String? = null
    var distritoSeleccionadoActual by mutableStateOf<String?>(null)

    fun cargarPaseadores(authHeader: String, userUuid: String? = ultimoUserUuid) {
        ultimoAuthHeader = authHeader
        if (!userUuid.isNullOrBlank()) {
            ultimoUserUuid = userUuid
        }
        _uiState.value = PaseadoresListUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Cargar catálogo de distritos desde application-parameters si aún no existe
                if (catalogoDistritos.isEmpty()) {
                    runCatching {
                        val paramsRes = RetrofitClient.apiService.getApplicationParameters(authHeader)
                        if (paramsRes.isSuccessful) {
                            catalogoDistritos = paramsRes.body()?.response?.distritos.orEmpty()
                        }
                    }
                }

                // 2. Obtener el distrito registrado del cliente para mostrarlo por defecto al entrar
                if (distritoClienteDefault == null && !ultimoUserUuid.isNullOrBlank()) {
                    runCatching {
                        val clienteRes = RetrofitClient.apiService.getClientePorUuid(
                            token = authHeader,
                            uuid = ultimoUserUuid!!
                        )
                        val desc = clienteRes.body()
                            ?.extraerUsuarioCliente()
                            ?.detalleCliente
                            ?.distritoDescripcion
                            ?.trim()
                            ?.uppercase()
                        if (!desc.isNullOrBlank()) {
                            distritoClienteDefault = desc
                        }
                    }
                }

                // Si es la carga inicial (sin filtro manual todavía) y conocemos el distrito del cliente,
                // lo fijamos como el distrito seleccionado por defecto.
                if (distritoSeleccionadoActual == null && !distritoClienteDefault.isNullOrBlank()) {
                    distritoSeleccionadoActual = distritoClienteDefault
                }

                // 3. Consulta al backend según el filtro seleccionado
                val response = if (distritoSeleccionadoActual == null) {
                    // Sin query params: el backend también filtra automáticamente por el distrito del cliente en sesión
                    RetrofitClient.apiService.getPaseadores(token = authHeader)
                } else if (distritoSeleccionadoActual.equals("Todos", ignoreCase = true)) {
                    val todosIds = catalogoDistritos.map { it.id }.filter { it > 0 }
                    RetrofitClient.apiService.getPaseadores(
                        token = authHeader,
                        distritoIds = if (todosIds.isNotEmpty()) todosIds else null
                    )
                } else {
                    val distritoObj = catalogoDistritos.firstOrNull {
                        it.descripcion.trim().equals(distritoSeleccionadoActual?.trim(), ignoreCase = true)
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

                        val nombresDistritos = if (catalogoDistritos.isNotEmpty()) {
                            catalogoDistritos.map { it.descripcion.trim().uppercase() }.distinct().sorted()
                        } else {
                            listaActualPaseadores.mapNotNull { it.distritoCoberturaDescripcion?.trim()?.uppercase() }.distinct().sorted()
                        }

                        val nombreMostrar = when {
                            distritoSeleccionadoActual.equals("Todos", ignoreCase = true) -> null
                            !distritoSeleccionadoActual.isNullOrBlank() -> distritoSeleccionadoActual?.trim()?.uppercase()
                            !distritoClienteDefault.isNullOrBlank() -> distritoClienteDefault
                            listaActualPaseadores.isNotEmpty() -> listaActualPaseadores.firstOrNull()?.distritoCoberturaDescripcion?.trim()?.uppercase()
                            else -> null
                        }

                        _uiState.value = PaseadoresListUiState.Success(
                            paseadores = listaActualPaseadores,
                            distritosDisponibles = nombresDistritos,
                            distritoSeleccionado = nombreMostrar,
                            distritoClienteDefault = distritoClienteDefault
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
            cargarPaseadores(ultimoAuthHeader, ultimoUserUuid)
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
                // 1. Obtener perfil del paseador (GET /paseadores/{uuid})
                val response = RetrofitClient.apiService.getPaseadorPorUsuarioUuid(
                    token = authHeader,
                    usuarioUuid = usuarioUuid
                )

                // 2. Obtener bloques de disponibilidad del día desde el nuevo endpoint especializado
                //    (GET /paseadores/{paseadorUuid}/disponibilidad)
                val dispFechaResponse = runCatching {
                    RetrofitClient.apiService.getDisponibilidadPaseadorPorFecha(
                        token = authHeader,
                        paseadorUuid = usuarioUuid
                    )
                }.getOrNull()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val detalleData = body?.response
                        val usuarioDetalle = detalleData?.extraerUsuario()

                        val dispFechaData = dispFechaResponse?.body()?.response
                        val bloquesFecha = dispFechaData?.bloques?.map { slot ->
                            if (slot.diaSemanaId == null && dispFechaData.diaSemanaId != null) {
                                slot.copy(
                                    diaSemanaId = dispFechaData.diaSemanaId,
                                    diaSemanaDescripcion = dispFechaData.diaSemanaDescripcion
                                )
                            } else {
                                slot
                            }
                        }

                        val disponibilidad = detalleData?.disponibilidad ?: bloquesFecha?.let {
                            DisponibilidadPaseadorDto(
                                maxMascotasPredeterminado = usuarioDetalle?.paseador?.maxMascotasPredeterminado,
                                slots = it
                            )
                        }

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
