package com.example.paseape

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.ActualizarEstadoUsuarioRequest
import com.example.paseape.data.model.ActualizarVerificacionPaseadorRequest
import com.example.paseape.data.model.ClienteAdminDto
import com.example.paseape.data.model.DigitalDocumentDto
import com.example.paseape.data.model.EstadoVerificacionPaseador
import com.example.paseape.data.model.PaseadorAdminDto
import com.example.paseape.data.model.UsuarioEstado
import com.example.paseape.data.model.soloDocumentosAdjuntos
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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
    val documentosPaseador: List<DigitalDocumentDto> = emptyList(),
    val cargandoDocumentos: Boolean = false,
    val documentosError: String? = null,
    /**
     * `usuarioUuid -> ¿tiene antecedentes adjuntos?` para las tarjetas del
     * listado. Se llena en segundo plano porque el endpoint del listado no
     * incluye `digital_documents`. Una clave ausente significa "sin dato".
     */
    val antecedentesPorUsuario: Map<String, Boolean> = emptyMap(),
    val cargandoAntecedentes: Boolean = false,
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
        _uiState.update {
            it.copy(
                selectedPaseadorForDialog = paseador,
                documentosPaseador = emptyList(),
                cargandoDocumentos = false,
                documentosError = null
            )
        }
    }

    fun closePaseadorDialog() {
        if (!_uiState.value.isUpdating) {
            _uiState.update {
                it.copy(
                    selectedPaseadorForDialog = null,
                    documentosPaseador = emptyList(),
                    cargandoDocumentos = false,
                    documentosError = null
                )
            }
        }
    }

    /**
     * Carga los documentos digitales del paseador seleccionado. El listado de
     * admin no los incluye, así que se consume `GET /paseadores/{uuid}` (devuelve
     * `digital_documents`) con el token del administrador.
     */
    fun cargarDocumentosPaseador(token: String, usuarioUuid: String) {
        if (usuarioUuid.isBlank()) return

        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(cargandoDocumentos = true, documentosError = null) }
            try {
                val response = RetrofitClient.apiService.getWalkerDetail(
                    token = bearerToken,
                    userUuid = usuarioUuid
                )

                if (response.isSuccessful) {
                    val documentos = response.body()?.response?.extraerUsuario()?.digitalDocuments
                        .orEmpty()
                        .soloDocumentosAdjuntos()
                    _uiState.update {
                        it.copy(cargandoDocumentos = false, documentosPaseador = documentos)
                    }
                } else {
                    val errorMsg = ApiErrorMapper.fromResponse(response, ApiErrorContext.ADMIN)
                    _uiState.update {
                        it.copy(cargandoDocumentos = false, documentosError = errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e("ADMIN_VM", "Error al cargar documentos del paseador", e)
                val errorMsg = ApiErrorMapper.fromThrowable(e, ApiErrorContext.ADMIN)
                _uiState.update {
                    it.copy(cargandoDocumentos = false, documentosError = errorMsg)
                }
            }
        }
    }

    /**
     * Pide la URL prefirmada del documento y la entrega por [onUrl] para que la
     * vista la descargue con `DownloadManager`. Si falla se avisa por snackbar.
     */
    fun obtenerUrlDescarga(
        token: String,
        documentUuid: String,
        onUrl: (String) -> Unit
    ) {
        if (documentUuid.isBlank()) return

        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.descargarDocumento(
                    token = bearerToken,
                    documentUuid = documentUuid
                )
                val url = response.body()?.response?.url

                if (response.isSuccessful && !url.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { onUrl(url) }
                } else {
                    _snackbarEvent.emit(
                        ApiErrorMapper.fromResponse(response, ApiErrorContext.ADMIN)
                    )
                }
            } catch (e: Exception) {
                Log.e("ADMIN_VM", "Error al obtener la URL de descarga", e)
                _snackbarEvent.emit(ApiErrorMapper.fromThrowable(e, ApiErrorContext.ADMIN))
            }
        }
    }

    /**
     * Completa en segundo plano qué paseadores del listado tienen antecedentes
     * adjuntos. El endpoint del listado no incluye `digital_documents`, así que
     * se consulta `GET /paseadores/{uuid}` por paseador (la misma llamada que
     * usa el diálogo) y se guarda el resultado por usuario. Si una consulta
     * falla se deja sin dato, sin interrumpir a las demás.
     */
    private suspend fun cargarAntecedentesDeLista(
        bearerToken: String,
        paseadores: List<PaseadorAdminDto>
    ) {
        val usuarios = paseadores
            .mapNotNull { it.detalleIdentifier?.takeIf { uuid -> uuid.isNotBlank() } }
            .distinct()
        if (usuarios.isEmpty()) return

        _uiState.update { it.copy(cargandoAntecedentes = true) }
        coroutineScope {
            usuarios.forEach { usuarioUuid ->
                launch {
                    val tieneAntecedentes = try {
                        val response = RetrofitClient.apiService.getWalkerDetail(
                            token = bearerToken,
                            userUuid = usuarioUuid
                        )
                        if (response.isSuccessful) {
                            response.body()?.response?.extraerUsuario()?.digitalDocuments
                                .orEmpty()
                                .soloDocumentosAdjuntos()
                                .isNotEmpty()
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("ADMIN_VM", "No se pudieron verificar los antecedentes", e)
                        null
                    }

                    if (tieneAntecedentes != null) {
                        _uiState.update {
                            it.copy(
                                antecedentesPorUsuario =
                                    it.antecedentesPorUsuario + (usuarioUuid to tieneAntecedentes)
                            )
                        }
                    }
                }
            }
        }
        _uiState.update { it.copy(cargandoAntecedentes = false) }
    }

    fun cargarDatos(token: String) {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    antecedentesPorUsuario = emptyMap(),
                    cargandoAntecedentes = false
                )
            }
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

                if (paseadoresList.isNotEmpty()) {
                    cargarAntecedentesDeLista(bearerToken, paseadoresList)
                }
            } catch (e: Exception) {
                Log.e("ADMIN_VM", "Error al cargar datos de admin", e)
                val msg = ApiErrorMapper.fromThrowable(e, ApiErrorContext.ADMIN)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cargandoAntecedentes = false,
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
