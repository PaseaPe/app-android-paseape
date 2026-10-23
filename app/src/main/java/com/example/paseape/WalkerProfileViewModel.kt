package com.example.paseape

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.UpdateCoverageRequestDto
import com.example.paseape.data.model.UpdateProfileRequestDto
import com.example.paseape.data.model.UsuarioPaseadorDetalleDto
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
                        val detalle = body?.response
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
        onSuccess: () -> Unit
    ) {
        _isOperationLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = RetrofitClient.apiService.updateProfile(
                    token = authHeader,
                    request = req
                )

                withContext(Dispatchers.Main) {
                    _isOperationLoading.value = false
                    if (response.isSuccessful) {
                        _userMessage.emit("Perfil de paseador actualizado correctamente.")
                        onSuccess()
                        cargarPerfil(token, usuarioUuid)
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
                        cargarPerfil(token, usuarioUuid)
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
}
