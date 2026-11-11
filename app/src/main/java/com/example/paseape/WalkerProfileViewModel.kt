package com.example.paseape

import android.content.ContentResolver
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.ArchivoPendiente
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.UpdateCoverageRequestDto
import com.example.paseape.data.model.UpdateProfileRequestDto
import com.example.paseape.data.model.UsuarioPaseadorDetalleDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.DocumentoUpload
import com.example.paseape.data.network.FotoUpload
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface WalkerProfileUiState {
    data object Loading : WalkerProfileUiState
    data class Success(val detalle: UsuarioPaseadorDetalleDto) : WalkerProfileUiState
    data class Error(val message: String) : WalkerProfileUiState
}

class WalkerProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<WalkerProfileUiState>(WalkerProfileUiState.Loading)
    val uiState: StateFlow<WalkerProfileUiState> = _uiState.asStateFlow()

    private val _distritos = MutableStateFlow<List<DistritoParamDto>>(emptyList())
    val distritos: StateFlow<List<DistritoParamDto>> = _distritos.asStateFlow()

    private val _isOperationLoading = MutableStateFlow(false)
    val isOperationLoading: StateFlow<Boolean> = _isOperationLoading.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _isDocumentLoading = MutableStateFlow(false)
    val isDocumentLoading: StateFlow<Boolean> = _isDocumentLoading.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    private val _documentError = MutableStateFlow<String?>(null)
    val documentError: StateFlow<String?> = _documentError.asStateFlow()

    private val _isFotoLoading = MutableStateFlow(false)
    val isFotoLoading: StateFlow<Boolean> = _isFotoLoading.asStateFlow()

    private val _fotoProgress = MutableStateFlow<Int?>(null)
    val fotoProgress: StateFlow<Int?> = _fotoProgress.asStateFlow()

    private val _fotoError = MutableStateFlow<String?>(null)
    val fotoError: StateFlow<String?> = _fotoError.asStateFlow()

    fun limpiarErrorDocumentos() {
        _documentError.value = null
        _fotoError.value = null
    }

    fun cargarParametros(token: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (!token.isNullOrBlank()) {
                    if (token.startsWith("Bearer ")) token else "Bearer $token"
                } else null
                val response = RetrofitClient.apiService.getApplicationParameters(authHeader)
                if (response.isSuccessful) {
                    val list = response.body()?.response?.distritos.orEmpty()
                    _distritos.value = list
                    Log.d("WALKER_PARAMS", "Distritos cargados dinámicamente: ${list.size}")
                } else {
                    Log.e("WALKER_PARAMS", "HTTP error ${response.code()} en application-parameters")
                }
            } catch (e: Exception) {
                Log.e("WALKER_PARAMS", "Error al consultar application-parameters", e)
            }
        }
    }

    fun cargarPerfil(token: String, usuarioUuid: String) {
        cargarParametros(token)

        if (usuarioUuid.isBlank()) {
            _uiState.value = WalkerProfileUiState.Error("No se encontró el identificador del paseador.")
            return
        }

        _uiState.value = WalkerProfileUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = RetrofitClient.apiService.getWalkerDetail(
                    token = authHeader,
                    userUuid = usuarioUuid
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val detalle = body?.response?.extraerUsuario()
                        if (detalle != null) {
                            _uiState.value = WalkerProfileUiState.Success(detalle)
                        } else {
                            _uiState.value = WalkerProfileUiState.Error("No se encontraron los datos del perfil de paseador.")
                        }
                    } else {
                        _uiState.value = WalkerProfileUiState.Error(
                            ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = WalkerProfileUiState.Error(
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
                    )
                }
            }
        }
    }

    fun actualizarPerfil(
        token: String,
        usuarioUuid: String,
        req: UpdateProfileRequestDto,
        archivosPendientes: List<ArchivoPendiente> = emptyList(),
        fotoPendiente: ArchivoPendiente? = null,
        resolver: ContentResolver? = null,
        onSuccess: () -> Unit
    ) {
        if (_isOperationLoading.value || _isDocumentLoading.value || _isFotoLoading.value) return

        _isOperationLoading.value = true
        _documentError.value = null
        _fotoError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = RetrofitClient.apiService.updateProfile(
                    token = authHeader,
                    request = req
                )

                if (response.isSuccessful) {
                    _userMessage.emit("Perfil de paseador actualizado correctamente.")

                    // Primero la foto de perfil y luego los antecedentes.
                    val errorFoto = if (fotoPendiente != null && resolver != null) {
                        enviarFoto(token, fotoPendiente, resolver)
                    } else {
                        null
                    }

                    val errorDocumentos =
                        if (errorFoto == null && archivosPendientes.isNotEmpty() && resolver != null) {
                            enviarAntecedentes(
                                token = token,
                                usuarioUuid = usuarioUuid,
                                archivos = archivosPendientes,
                                resolver = resolver
                            )
                        } else {
                            null
                        }

                    cargarPerfilSilencioso(token, usuarioUuid)

                    withContext(Dispatchers.Main) {
                        _isOperationLoading.value = false
                        if (errorFoto != null) _fotoError.value = errorFoto
                        if (errorDocumentos != null) _documentError.value = errorDocumentos

                        if (errorFoto == null && errorDocumentos == null) {
                            onSuccess()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _isOperationLoading.value = false
                        _userMessage.emit(
                            ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isOperationLoading.value = false
                    _userMessage.emit(
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
                    )
                }
            }
        }
    }

    fun actualizarCobertura(
        token: String,
        usuarioUuid: String,
        distritoId: Int,
        onSuccess: () -> Unit
    ) {
        _isOperationLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val req = UpdateCoverageRequestDto(
                    usuarioUuid = usuarioUuid,
                    distritoId = distritoId
                )
                val response = RetrofitClient.apiService.updateWalkerCoverage(
                    token = authHeader,
                    request = req
                )

                withContext(Dispatchers.Main) {
                    _isOperationLoading.value = false
                    if (response.isSuccessful) {
                        _userMessage.emit("Distrito de cobertura actualizado exitosamente.")
                        onSuccess()
                        cargarPerfilSilencioso(token, usuarioUuid)
                    } else {
                        _userMessage.emit(
                            ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isOperationLoading.value = false
                    _userMessage.emit(
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Documentos de antecedentes (subir / descargar / eliminar)
    // ------------------------------------------------------------------

    /**
     * Envía la foto de perfil del paseador al backend y devuelve el mensaje de
     * error, o `null` si todo salió bien.
     */
    private suspend fun enviarFoto(
        token: String,
        foto: ArchivoPendiente,
        resolver: ContentResolver
    ): String? {
        _isFotoLoading.value = true
        _fotoProgress.value = 0
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val parte = FotoUpload.crearParte(resolver, foto) { porcentaje ->
                _fotoProgress.value = porcentaje
            }

            if (parte == null) {
                "No se pudo leer la imagen seleccionada."
            } else {
                val response = RetrofitClient.apiService.subirFotoPaseador(authHeader, parte)
                if (response.isSuccessful) {
                    _userMessage.emit("Foto de perfil actualizada correctamente.")
                    null
                } else {
                    ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                }
            }
        } catch (e: Exception) {
            ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
        } finally {
            _isFotoLoading.value = false
            _fotoProgress.value = null
        }
    }

    /**
     * Envía los archivos seleccionados al backend y devuelve el mensaje de
     * error, o `null` si todo salió bien. Se usa desde [actualizarPerfil] para
     * subir los documentos justo después de guardar los datos del perfil.
     */
    private suspend fun enviarAntecedentes(
        token: String,
        usuarioUuid: String,
        archivos: List<ArchivoPendiente>,
        resolver: ContentResolver
    ): String? {
        _isDocumentLoading.value = true
        _uploadProgress.value = 0
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val partes = DocumentoUpload.crearPartes(resolver, archivos) { porcentaje ->
                _uploadProgress.value = porcentaje
            }

            if (partes.isEmpty()) {
                "No se pudieron leer los archivos seleccionados."
            } else {
                val response = RetrofitClient.apiService.subirAntecedentes(
                    token = authHeader,
                    usuarioUuid = usuarioUuid,
                    files = partes
                )

                if (response.isSuccessful) {
                    _userMessage.emit("Documento(s) subido(s) correctamente.")
                    null
                } else {
                    ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                }
            }
        } catch (e: Exception) {
            ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
        } finally {
            _isDocumentLoading.value = false
            _uploadProgress.value = null
        }
    }

    fun eliminarDocumento(
        token: String,
        usuarioUuid: String,
        documentUuid: String,
        onSuccess: () -> Unit = {}
    ) {
        if (documentUuid.isBlank() || _isDocumentLoading.value) return

        _isDocumentLoading.value = true
        _documentError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = RetrofitClient.apiService.eliminarDocumento(
                    token = authHeader,
                    documentUuid = documentUuid
                )

                withContext(Dispatchers.Main) {
                    _isDocumentLoading.value = false
                    if (response.isSuccessful) {
                        _userMessage.emit("Documento eliminado correctamente.")
                        onSuccess()
                        cargarPerfilSilencioso(token, usuarioUuid)
                    } else {
                        _documentError.value =
                            ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isDocumentLoading.value = false
                    _documentError.value =
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
                }
            }
        }
    }

    fun obtenerUrlDescarga(
        token: String,
        documentUuid: String,
        onUrl: (String) -> Unit
    ) {
        if (documentUuid.isBlank()) return

        _documentError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = RetrofitClient.apiService.descargarDocumento(
                    token = authHeader,
                    documentUuid = documentUuid
                )

                withContext(Dispatchers.Main) {
                    val url = response.body()?.response?.url
                    if (response.isSuccessful && !url.isNullOrBlank()) {
                        onUrl(url)
                    } else {
                        _documentError.value =
                            ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _documentError.value =
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
                }
            }
        }
    }

    /**
     * Recarga el detalle del paseador sin pasar por [WalkerProfileUiState.Loading],
     * para no cerrar los diálogos abiertos sobre la pantalla de perfil.
     */
    private fun cargarPerfilSilencioso(token: String, usuarioUuid: String) {
        if (usuarioUuid.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = RetrofitClient.apiService.getWalkerDetail(
                    token = authHeader,
                    userUuid = usuarioUuid
                )

                if (response.isSuccessful) {
                    val detalle = response.body()?.response?.extraerUsuario()
                    if (detalle != null) {
                        withContext(Dispatchers.Main) {
                            _uiState.value = WalkerProfileUiState.Success(detalle)
                        }
                    }
                } else {
                    Log.e(
                        "WALKER_PROFILE",
                        "No se pudo refrescar el perfil: HTTP ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("WALKER_PROFILE", "Error al refrescar el perfil", e)
            }
        }
    }
}
