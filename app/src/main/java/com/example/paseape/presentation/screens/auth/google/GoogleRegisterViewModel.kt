package com.example.paseape.presentation.screens.auth.google

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.core.auth.GoogleAuthManager
import com.example.paseape.core.util.AppConstants
import com.example.paseape.domain.model.RegisterGoogleClientParams
import com.example.paseape.domain.model.RegisterGoogleWalkerParams
import com.example.paseape.domain.model.RegisterPetParams
import com.example.paseape.domain.usecase.auth.RegisterGoogleClientUseCase
import com.example.paseape.domain.usecase.auth.RegisterGoogleWalkerUseCase
import com.example.paseape.domain.usecase.parameter.GetApplicationParametersUseCase
import com.example.paseape.domain.usecase.parameter.GetRazasByTipoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoogleRegisterViewModel @Inject constructor(
    private val googleAuthManager: GoogleAuthManager,
    private val getApplicationParametersUseCase: GetApplicationParametersUseCase,
    private val getRazasByTipoUseCase: GetRazasByTipoUseCase,
    private val registerGoogleClientUseCase: RegisterGoogleClientUseCase,
    private val registerGoogleWalkerUseCase: RegisterGoogleWalkerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleRegisterUiState())
    val uiState: StateFlow<GoogleRegisterUiState> = _uiState.asStateFlow()

    init {
        loadParameters()
    }

    private fun loadParameters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isParametersLoading = true) }
            val result = getApplicationParametersUseCase()
            result.onSuccess { params ->
                _uiState.update { it.copy(parameters = params, isParametersLoading = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(isParametersLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun onEvent(event: GoogleRegisterUiEvent) {
        when (event) {
            is GoogleRegisterUiEvent.RequestGoogleSignIn -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    val result = googleAuthManager.getGoogleIdToken(event.activityContext)
                    result.onSuccess { token ->
                        _uiState.update { it.copy(idToken = token, isLoading = false) }
                    }.onFailure { err ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                    }
                }
            }
            is GoogleRegisterUiEvent.RoleChanged -> _uiState.update { it.copy(selectedTipoUsuarioId = event.tipoUsuarioId) }
            is GoogleRegisterUiEvent.TelefonoChanged -> _uiState.update { it.copy(telefono = event.telefono) }
            is GoogleRegisterUiEvent.DireccionReferenciaChanged -> _uiState.update { it.copy(direccionReferencia = event.direccion) }
            is GoogleRegisterUiEvent.DistritoSelected -> _uiState.update { it.copy(distritoId = event.distritoId) }
            is GoogleRegisterUiEvent.ContactoEmergenciaNombreChanged -> _uiState.update { it.copy(contactoEmergenciaNombre = event.nombre) }
            is GoogleRegisterUiEvent.ContactoEmergenciaTelefonoChanged -> _uiState.update { it.copy(contactoEmergenciaTelefono = event.telefono) }
            is GoogleRegisterUiEvent.NotasAdicionalesChanged -> _uiState.update { it.copy(notasAdicionales = event.notas) }

            // Mascotas
            is GoogleRegisterUiEvent.SetWantsToRegisterPets -> _uiState.update {
                it.copy(
                    wantsToRegisterPets = event.wants,
                    mascotas = if (event.wants && it.mascotas.isEmpty()) listOf(PetFormState()) else it.mascotas
                )
            }
            is GoogleRegisterUiEvent.AddPetClicked -> _uiState.update {
                it.copy(mascotas = it.mascotas + PetFormState())
            }
            is GoogleRegisterUiEvent.RemovePetClicked -> _uiState.update {
                it.copy(mascotas = it.mascotas.filterNot { pet -> pet.tempId == event.tempId })
            }
            is GoogleRegisterUiEvent.PetNombreChanged -> updatePet(event.tempId) { it.copy(nombre = event.nombre) }
            is GoogleRegisterUiEvent.PetTipoMascotaSelected -> {
                updatePet(event.tempId) { it.copy(tipoMascotaId = event.tipoId, tipoRazaId = null, isLoadingRazas = true) }
                loadRazasForPet(event.tempId, event.tipoId)
            }
            is GoogleRegisterUiEvent.PetRazaSelected -> updatePet(event.tempId) { it.copy(tipoRazaId = event.razaId) }
            is GoogleRegisterUiEvent.PetGeneroSelected -> updatePet(event.tempId) { it.copy(tipoGeneroMascotaId = event.generoId) }
            is GoogleRegisterUiEvent.PetTamanoSelected -> updatePet(event.tempId) { it.copy(tipoTamanoMascotaId = event.tamanoId) }
            is GoogleRegisterUiEvent.PetNivelEnergiaSelected -> updatePet(event.tempId) { it.copy(tipoNivelEnergiaId = event.nivelId) }
            is GoogleRegisterUiEvent.PetEdadAnosChanged -> updatePet(event.tempId) { it.copy(edadAnos = event.anos) }
            is GoogleRegisterUiEvent.PetEdadMesesChanged -> updatePet(event.tempId) { it.copy(edadMeses = event.meses) }
            is GoogleRegisterUiEvent.PetPesoChanged -> updatePet(event.tempId) { it.copy(pesoKg = event.peso) }
            is GoogleRegisterUiEvent.PetEsterilizadoChanged -> updatePet(event.tempId) { it.copy(esterilizado = event.esterilizado) }
            is GoogleRegisterUiEvent.PetPrecaucionesChanged -> updatePet(event.tempId) { it.copy(precaucionesMedicas = event.precauciones) }

            // Paseador
            is GoogleRegisterUiEvent.TipoDocumentoSelected -> _uiState.update { it.copy(tipoDocumentoId = event.id) }
            is GoogleRegisterUiEvent.NumeroDocumentoChanged -> _uiState.update { it.copy(numeroDocumento = event.numero) }
            is GoogleRegisterUiEvent.ExperienciaAnosChanged -> _uiState.update { it.copy(experienciaAnos = event.anos) }
            is GoogleRegisterUiEvent.TarifaHoraChanged -> _uiState.update { it.copy(tarifaHoraPen = event.tarifa) }
            is GoogleRegisterUiEvent.DistritoCoberturaSelected -> _uiState.update { it.copy(distritoCoberturaId = event.id) }
            is GoogleRegisterUiEvent.BiografiaChanged -> _uiState.update { it.copy(biografia = event.bio) }
            is GoogleRegisterUiEvent.AntecedentesUrlChanged -> _uiState.update { it.copy(antecedentesPolicialesUrl = event.url) }

            is GoogleRegisterUiEvent.SubmitRegistration -> submitForm()
            is GoogleRegisterUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun updatePet(tempId: String, transform: (PetFormState) -> PetFormState) {
        _uiState.update { state ->
            state.copy(mascotas = state.mascotas.map { if (it.tempId == tempId) transform(it) else it })
        }
    }

    private fun loadRazasForPet(tempId: String, tipoMascotaId: Int) {
        viewModelScope.launch {
            val result = getRazasByTipoUseCase(tipoMascotaId)
            result.onSuccess { razas ->
                updatePet(tempId) { it.copy(availableRazas = razas, isLoadingRazas = false) }
            }.onFailure {
                updatePet(tempId) { it.copy(isLoadingRazas = false) }
            }
        }
    }

    private fun submitForm() {
        val state = _uiState.value
        if (state.idToken.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Debes vincular tu cuenta con Google") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (state.selectedTipoUsuarioId == AppConstants.TIPO_USUARIO_CLIENTE_ID) {
                val distrito = state.distritoId
                if (state.telefono.isBlank() || state.direccionReferencia.isBlank() || distrito == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Completa los datos del cliente") }
                    return@launch
                }

                val mascotasList = if (state.wantsToRegisterPets) {
                    state.mascotas.mapNotNull { pet ->
                        if (pet.nombre.isNotBlank() && pet.tipoMascotaId != null && pet.tipoRazaId != null) {
                            RegisterPetParams(
                                nombre = pet.nombre,
                                tipoMascotaId = pet.tipoMascotaId,
                                tipoRazaId = pet.tipoRazaId,
                                tipoGeneroMascotaId = pet.tipoGeneroMascotaId ?: 1,
                                tipoTamanoMascotaId = pet.tipoTamanoMascotaId ?: 1,
                                tipoNivelEnergiaId = pet.tipoNivelEnergiaId ?: 1,
                                edadAnos = pet.edadAnos.toIntOrNull() ?: 0,
                                edadMeses = pet.edadMeses.toIntOrNull() ?: 0,
                                pesoKg = pet.pesoKg.toDoubleOrNull() ?: 0.0,
                                esterilizado = pet.esterilizado
                            )
                        } else null
                    }
                } else emptyList()

                val params = RegisterGoogleClientParams(
                    idToken = state.idToken,
                    telefono = state.telefono,
                    direccionReferencia = state.direccionReferencia,
                    distritoId = distrito,
                    contactoEmergenciaNombre = state.contactoEmergenciaNombre,
                    contactoEmergenciaTelefono = state.contactoEmergenciaTelefono,
                    notasAdicionales = state.notasAdicionales,
                    mascotas = mascotasList
                )
                val result = registerGoogleClientUseCase(params)
                result.onSuccess {
                    _uiState.update { it.copy(isLoading = false, isRegistrationSuccess = true) }
                }.onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }

            } else {
                val tipoDoc = state.tipoDocumentoId
                val cobertura = state.distritoCoberturaId
                if (state.telefono.isBlank() || tipoDoc == null || state.numeroDocumento.isBlank() || cobertura == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Completa los datos del paseador") }
                    return@launch
                }

                val params = RegisterGoogleWalkerParams(
                    idToken = state.idToken,
                    telefono = state.telefono,
                    tipoDocumentoId = tipoDoc,
                    numeroDocumento = state.numeroDocumento,
                    antecedentesPolicialesUrl = state.antecedentesPolicialesUrl,
                    experienciaAnos = state.experienciaAnos.toIntOrNull() ?: 0,
                    biografia = state.biografia,
                    tarifaHoraPen = state.tarifaHoraPen.toDoubleOrNull() ?: 0.0,
                    distritoCoberturaId = cobertura
                )
                val result = registerGoogleWalkerUseCase(params)
                result.onSuccess {
                    _uiState.update { it.copy(isLoading = false, isRegistrationSuccess = true) }
                }.onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
            }
        }
    }
}