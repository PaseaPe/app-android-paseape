package com.example.paseape.presentation.screens.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.core.util.AppConstants
import com.example.paseape.core.util.ValidationUtils
import com.example.paseape.domain.model.RegisterClientParams
import com.example.paseape.domain.model.RegisterPetParams
import com.example.paseape.domain.model.RegisterWalkerParams
import com.example.paseape.domain.usecase.auth.RegisterClientUseCase
import com.example.paseape.domain.usecase.auth.RegisterWalkerUseCase
import com.example.paseape.domain.usecase.parameter.GetApplicationParametersUseCase
import com.example.paseape.domain.usecase.parameter.GetRazasByTipoUseCase
import com.example.paseape.presentation.screens.auth.google.PetFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val getApplicationParametersUseCase: GetApplicationParametersUseCase,
    private val getRazasByTipoUseCase: GetRazasByTipoUseCase,
    private val registerClientUseCase: RegisterClientUseCase,
    private val registerWalkerUseCase: RegisterWalkerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

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

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.RoleChanged -> _uiState.update { it.copy(selectedTipoUsuarioId = event.tipoUsuarioId) }
            is RegisterUiEvent.NombresChanged -> _uiState.update { it.copy(nombres = event.nombres, nombresError = null) }
            is RegisterUiEvent.ApellidosChanged -> _uiState.update { it.copy(apellidos = event.apellidos, apellidosError = null) }
            is RegisterUiEvent.CorreoChanged -> _uiState.update { it.copy(correo = event.correo, correoError = null) }
            is RegisterUiEvent.ContrasenaChanged -> _uiState.update { it.copy(contrasena = event.contrasena, contrasenaError = null) }
            is RegisterUiEvent.ConfirmarContrasenaChanged -> _uiState.update { it.copy(confirmarContrasena = event.confirmar, confirmarContrasenaError = null) }
            is RegisterUiEvent.TelefonoChanged -> _uiState.update { it.copy(telefono = event.telefono, telefonoError = null) }

            // Cliente
            is RegisterUiEvent.DireccionReferenciaChanged -> _uiState.update { it.copy(direccionReferencia = event.direccion, direccionReferenciaError = null) }
            is RegisterUiEvent.DistritoSelected -> _uiState.update { it.copy(distritoId = event.distritoId, distritoError = null) }
            is RegisterUiEvent.ContactoEmergenciaNombreChanged -> _uiState.update { it.copy(contactoEmergenciaNombre = event.nombre) }
            is RegisterUiEvent.ContactoEmergenciaTelefonoChanged -> _uiState.update { it.copy(contactoEmergenciaTelefono = event.telefono) }
            is RegisterUiEvent.NotasAdicionalesChanged -> _uiState.update { it.copy(notasAdicionales = event.notas) }

            // Mascotas
            is RegisterUiEvent.SetWantsToRegisterPets -> _uiState.update {
                it.copy(
                    wantsToRegisterPets = event.wants,
                    mascotas = if (event.wants && it.mascotas.isEmpty()) listOf(PetFormState()) else it.mascotas
                )
            }
            is RegisterUiEvent.AddPetClicked -> _uiState.update { it.copy(mascotas = it.mascotas + PetFormState()) }
            is RegisterUiEvent.RemovePetClicked -> _uiState.update {
                it.copy(mascotas = it.mascotas.filterNot { pet -> pet.tempId == event.tempId })
            }
            is RegisterUiEvent.PetNombreChanged -> updatePet(event.tempId) { it.copy(nombre = event.nombre) }
            is RegisterUiEvent.PetTipoMascotaSelected -> {
                updatePet(event.tempId) { it.copy(tipoMascotaId = event.tipoId, tipoRazaId = null, isLoadingRazas = true) }
                loadRazasForPet(event.tempId, event.tipoId)
            }
            is RegisterUiEvent.PetRazaSelected -> updatePet(event.tempId) { it.copy(tipoRazaId = event.razaId) }
            is RegisterUiEvent.PetGeneroSelected -> updatePet(event.tempId) { it.copy(tipoGeneroMascotaId = event.generoId) }
            is RegisterUiEvent.PetTamanoSelected -> updatePet(event.tempId) { it.copy(tipoTamanoMascotaId = event.tamanoId) }
            is RegisterUiEvent.PetNivelEnergiaSelected -> updatePet(event.tempId) { it.copy(tipoNivelEnergiaId = event.nivelId) }
            is RegisterUiEvent.PetEdadAnosChanged -> updatePet(event.tempId) { it.copy(edadAnos = event.anos) }
            is RegisterUiEvent.PetEdadMesesChanged -> updatePet(event.tempId) { it.copy(edadMeses = event.meses) }
            is RegisterUiEvent.PetPesoChanged -> updatePet(event.tempId) { it.copy(pesoKg = event.peso) }
            is RegisterUiEvent.PetEsterilizadoChanged -> updatePet(event.tempId) { it.copy(esterilizado = event.esterilizado) }
            is RegisterUiEvent.PetSociableConPerrosChanged -> updatePet(event.tempId) { it.copy(sociableConPerros = event.sociable) }
            is RegisterUiEvent.PetSociableConPersonasChanged -> updatePet(event.tempId) { it.copy(sociableConPersonas = event.sociable) }
            is RegisterUiEvent.PetPrecaucionesChanged -> updatePet(event.tempId) { it.copy(precaucionesMedicas = event.precauciones) }
            is RegisterUiEvent.PetFotoUrlChanged -> updatePet(event.tempId) { it.copy(fotoUrl = event.url) }

            // Paseador
            is RegisterUiEvent.TipoDocumentoSelected -> _uiState.update { it.copy(tipoDocumentoId = event.id, tipoDocumentoError = null) }
            is RegisterUiEvent.NumeroDocumentoChanged -> _uiState.update { it.copy(numeroDocumento = event.numero, numeroDocumentoError = null) }
            is RegisterUiEvent.ExperienciaAnosChanged -> _uiState.update { it.copy(experienciaAnos = event.anos, experienciaAnosError = null) }
            is RegisterUiEvent.TarifaHoraChanged -> _uiState.update { it.copy(tarifaHoraPen = event.tarifa, tarifaHoraPenError = null) }
            is RegisterUiEvent.DistritoCoberturaSelected -> _uiState.update { it.copy(distritoCoberturaId = event.id, distritoCoberturaError = null) }
            is RegisterUiEvent.BiografiaChanged -> _uiState.update { it.copy(biografia = event.bio, biografiaError = null) }
            is RegisterUiEvent.AntecedentesUrlChanged -> _uiState.update { it.copy(antecedentesPolicialesUrl = event.url) }

            is RegisterUiEvent.SubmitRegistration -> validateAndSubmit()
            is RegisterUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
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

    private fun validateAndSubmit() {
        val s = _uiState.value
        var hasError = false

        var nombresError: String? = null
        var apellidosError: String? = null
        var correoError: String? = null
        var contrasenaError: String? = null
        var confirmarError: String? = null
        var telefonoError: String? = null

        if (s.nombres.isBlank()) { nombresError = "Ingresa tus nombres"; hasError = true }
        if (s.apellidos.isBlank()) { apellidosError = "Ingresa tus apellidos"; hasError = true }
        if (!ValidationUtils.isValidEmail(s.correo)) { correoError = "Correo electrónico inválido"; hasError = true }
        if (!ValidationUtils.isValidPassword(s.contrasena)) { contrasenaError = "Mínimo 6 caracteres"; hasError = true }
        if (s.contrasena != s.confirmarContrasena) { confirmarError = "Las contraseñas no coinciden"; hasError = true }
        if (s.telefono.isBlank()) { telefonoError = "Ingresa tu número de teléfono"; hasError = true }

        if (s.selectedTipoUsuarioId == AppConstants.TIPO_USUARIO_CLIENTE_ID) {
            var direccionError: String? = null
            var distritoError: String? = null
            if (s.direccionReferencia.isBlank()) { direccionError = "Ingresa tu dirección de referencia"; hasError = true }
            if (s.distritoId == null) { distritoError = "Selecciona un distrito"; hasError = true }

            if (hasError) {
                _uiState.update {
                    it.copy(
                        nombresError = nombresError,
                        apellidosError = apellidosError,
                        correoError = correoError,
                        contrasenaError = contrasenaError,
                        confirmarContrasenaError = confirmarError,
                        telefonoError = telefonoError,
                        direccionReferenciaError = direccionError,
                        distritoError = distritoError
                    )
                }
                return
            }

            executeClientRegistration()
        } else {
            var docTypeError: String? = null
            var docNumberError: String? = null
            var expError: String? = null
            var tarifaError: String? = null
            var coberturaError: String? = null
            var bioError: String? = null

            if (s.tipoDocumentoId == null) { docTypeError = "Selecciona tipo de documento"; hasError = true }
            if (s.numeroDocumento.isBlank()) { docNumberError = "Ingresa número de documento"; hasError = true }
            if (s.experienciaAnos.toIntOrNull() == null) { expError = "Ingresa años de experiencia válidos"; hasError = true }
            if (s.tarifaHoraPen.toDoubleOrNull() == null) { tarifaError = "Ingresa una tarifa válida"; hasError = true }
            if (s.distritoCoberturaId == null) { coberturaError = "Selecciona tu distrito de cobertura"; hasError = true }
            if (s.biografia.isBlank()) { bioError = "Ingresa una breve biografía"; hasError = true }

            if (hasError) {
                _uiState.update {
                    it.copy(
                        nombresError = nombresError,
                        apellidosError = apellidosError,
                        correoError = correoError,
                        contrasenaError = contrasenaError,
                        confirmarContrasenaError = confirmarError,
                        telefonoError = telefonoError,
                        tipoDocumentoError = docTypeError,
                        numeroDocumentoError = docNumberError,
                        experienciaAnosError = expError,
                        tarifaHoraPenError = tarifaError,
                        distritoCoberturaError = coberturaError,
                        biografiaError = bioError
                    )
                }
                return
            }

            executeWalkerRegistration()
        }
    }

    private fun executeClientRegistration() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val mascotasList = if (s.wantsToRegisterPets) {
                s.mascotas.mapNotNull { pet ->
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
                            esterilizado = pet.esterilizado,
                            sociableConPerros = pet.sociableConPerros,
                            sociableConPersonas = pet.sociableConPersonas,
                            precaucionesMedicas = pet.precaucionesMedicas.trim().ifBlank { null },
                            fotoUrl = pet.fotoUrl.trim().ifBlank { null }
                        )
                    } else null
                }
            } else emptyList()

            val params = RegisterClientParams(
                nombres = s.nombres,
                apellidos = s.apellidos,
                correo = s.correo,
                contrasena = s.contrasena,
                telefono = s.telefono,
                direccionReferencia = s.direccionReferencia,
                distritoId = s.distritoId!!,
                contactoEmergenciaNombre = s.contactoEmergenciaNombre.ifBlank { null },
                contactoEmergenciaTelefono = s.contactoEmergenciaTelefono.ifBlank { null },
                notasAdicionales = s.notasAdicionales.ifBlank { null },
                mascotas = mascotasList
            )

            val result = registerClientUseCase(params)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isRegistrationSuccess = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    private fun executeWalkerRegistration() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val params = RegisterWalkerParams(
                nombres = s.nombres,
                apellidos = s.apellidos,
                correo = s.correo,
                contrasena = s.contrasena,
                telefono = s.telefono,
                tipoDocumentoId = s.tipoDocumentoId!!,
                numeroDocumento = s.numeroDocumento,
                antecedentesPolicialesUrl = s.antecedentesPolicialesUrl.ifBlank { null },
                experienciaAnos = s.experienciaAnos.toIntOrNull() ?: 0,
                biografia = s.biografia,
                tarifaHoraPen = s.tarifaHoraPen.toDoubleOrNull() ?: 0.0,
                distritoCoberturaId = s.distritoCoberturaId!!
            )

            val result = registerWalkerUseCase(params)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isRegistrationSuccess = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }
}