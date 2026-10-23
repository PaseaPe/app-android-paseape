package com.example.paseape

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.ActualizarEstadoUsuarioRequest
import com.example.paseape.data.model.ActualizarVerificacionPaseadorRequest
import com.example.paseape.data.model.ClienteAdminDto
import com.example.paseape.data.model.EstadoVerificacionPaseador
import com.example.paseape.data.model.PaseadorAdminDto
import com.example.paseape.data.model.UsuarioEstado
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdminUiState(
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val selectedTab: Int = 0,
    val paseadores: List<PaseadorAdminDto> = emptyList(),
    val clientes: List<ClienteAdminDto> = emptyList(),
    val selectedClienteForDialog: ClienteAdminDto? = null,
    val selectedPaseadorForDialog: PaseadorAdminDto? = null,
    val errorMessage: String? = null
)

class AdminViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun openClienteDialog(cliente: ClienteAdminDto) {
        _uiState.update { it.copy(selectedClienteForDialog = cliente) }
    }

    fun closeClienteDialog() {
        if (!_uiState.value.isUpdating) {
            _uiState.update { it.copy(selectedClienteForDialog = null) }
        }
    }

    fun openPaseadorDialog(paseador: PaseadorAdminDto) {
        _uiState.update { it.copy(selectedPaseadorForDialog = paseador) }
    }

    fun closePaseadorDialog() {
        if (!_uiState.value.isUpdating) {
            _uiState.update { it.copy(selectedPaseadorForDialog = null) }
        }
    }

    fun cargarDatos(token: String) {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val responsePaseadores = RetrofitClient.apiService.listarPaseadoresAdmin(bearerToken)
                val responseClientes = RetrofitClient.apiService.listarClientesAdmin(bearerToken)

                val paseadoresList = if (responsePaseadores.isSuccessful) {
                    responsePaseadores.body()?.response.orEmpty()
                } else {
                    emptyList()
                }

                val clientesList = if (responseClientes.isSuccessful) {
                    responseClientes.body()?.response.orEmpty()
                } else {
                    emptyList()
                }

                val paseadoresError = if (!responsePaseadores.isSuccessful) {
                    ApiErrorMapper.fromResponse(responsePaseadores, ApiErrorContext.ADMIN)
                } else {
                    null
                }

                val clientesError = if (!responseClientes.isSuccessful) {
                    ApiErrorMapper.fromResponse(responseClientes, ApiErrorContext.ADMIN)
                } else {
                    null
                }

                val loadError = paseadoresError ?: clientesError

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        paseadores = paseadoresList,
                        clientes = clientesList,
                        errorMessage = loadError
                    )
                }

                if (loadError != null) {
                    _snackbarEvent.emit(loadError)
                }
            } catch (e: Exception) {
                Log.e("ADMIN_VM", "Error al cargar datos de admin", e)
                val msg = ApiErrorMapper.fromThrowable(e, ApiErrorContext.ADMIN)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = msg
                    )
                }
                _snackbarEvent.emit(msg)
            }
        }
    }

    fun actualizarEstadoCliente(
        token: String,
        clienteUuid: String,
        nuevoEstado: UsuarioEstado
    ) {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                val request = ActualizarEstadoUsuarioRequest(usuarioEstadoId = nuevoEstado.id)
                val response = RetrofitClient.apiService.actualizarEstadoUsuario(bearerToken, clienteUuid, request)

                if (response.isSuccessful) {
                    val message = "Estado actualizado a ${nuevoEstado.descripcion}"
                    // Actualización local inmediata del estado
                    val clientesActualizados = _uiState.value.clientes.map { cliente ->
                        if (cliente.clienteIdentifier == clienteUuid) {
                            cliente.copy(
                                usuarioEstadoId = nuevoEstado.id,
                                estadoUsuario = nuevoEstado.descripcion
                            )
                        } else cliente
                    }

                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            selectedClienteForDialog = null,
                            clientes = clientesActualizados
                        )
                    }
                    _snackbarEvent.emit(message)
                } else {
                    val errorMsg = ApiErrorMapper.fromResponse(response, ApiErrorContext.ADMIN)
                    _uiState.update { it.copy(isUpdating = false) }
                    _snackbarEvent.emit(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("ADMIN_VM", "Error al actualizar estado de usuario", e)
                val errorMsg = ApiErrorMapper.fromThrowable(e, ApiErrorContext.ADMIN)
                _uiState.update { it.copy(isUpdating = false) }
                _snackbarEvent.emit(errorMsg)
            }
        }
    }

    fun actualizarVerificacionPaseador(
        token: String,
        paseadorUuid: String,
        nuevoEstado: EstadoVerificacionPaseador
    ) {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                val request = ActualizarVerificacionPaseadorRequest(estadoVerificacionId = nuevoEstado.id)
                val response = RetrofitClient.apiService.actualizarVerificacionPaseador(bearerToken, paseadorUuid, request)

                if (response.isSuccessful) {
                    val message = "Verificación actualizada a ${nuevoEstado.descripcion}"
                    // Actualización local inmediata de la lista de paseadores
                    val paseadoresActualizados = _uiState.value.paseadores.map { paseador ->
                        if (paseador.paseadorIdentifier == paseadorUuid) {
                            paseador.copy(
                                estadoVerificacionId = nuevoEstado.id,
                                estadoVerificacion = nuevoEstado.descripcion
                            )
                        } else paseador
                    }

                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            selectedPaseadorForDialog = null,
                            paseadores = paseadoresActualizados
                        )
                    }
                    _snackbarEvent.emit(message)
                } else {
                    val errorMsg = ApiErrorMapper.fromResponse(response, ApiErrorContext.ADMIN)
                    _uiState.update { it.copy(isUpdating = false) }
                    _snackbarEvent.emit(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("ADMIN_VM", "Error al actualizar verificación de paseador", e)
                val errorMsg = ApiErrorMapper.fromThrowable(e, ApiErrorContext.ADMIN)
                _uiState.update { it.copy(isUpdating = false) }
                _snackbarEvent.emit(errorMsg)
            }
        }
    }
}
