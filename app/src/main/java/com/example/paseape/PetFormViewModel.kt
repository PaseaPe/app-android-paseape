package com.example.paseape

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.ActualizarMascotaRequest
import com.example.paseape.data.model.CrearMascotaPayload
import com.example.paseape.data.model.CrearMascotaRequest
import com.example.paseape.data.model.GeneroMascotaParamDto
import com.example.paseape.data.model.MascotaData
import com.example.paseape.data.model.NivelEnergiaParamDto
import com.example.paseape.data.model.RazaParamDto
import com.example.paseape.data.model.TamanoMascotaParamDto
import com.example.paseape.data.model.TipoMascotaParamDto
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PetFormUiState(
    // Catálogos maestros
    val tiposMascota: List<TipoMascotaParamDto> = emptyList(),
    val todasLasRazas: List<RazaParamDto> = emptyList(),
    val razasFiltradas: List<RazaParamDto> = emptyList(),
    val generosMascota: List<GeneroMascotaParamDto> = emptyList(),
    val tamanosMascota: List<TamanoMascotaParamDto> = emptyList(),
    val nivelesEnergia: List<NivelEnergiaParamDto> = emptyList(),

    // Campos del formulario
    val nombre: String = "",
    val selectedTipoMascota: TipoMascotaParamDto? = null,
    val selectedRaza: RazaParamDto? = null,
    val selectedGenero: GeneroMascotaParamDto? = null,
    val selectedTamano: TamanoMascotaParamDto? = null,
    val selectedNivelEnergia: NivelEnergiaParamDto? = null,
    val edadAnos: String = "0",
    val edadMeses: String = "0",
    val pesoKg: String = "0.0",
    val esterilizado: Boolean = false,
    val sociableConPerros: Boolean = true,
    val sociableConPersonas: Boolean = true,
    val precaucionesMedicas: String = "",
    val fotoUrl: String = "",

    // Banderas de estado
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoadingRazas: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
) {
    val isRazaEnabled: Boolean
        get() = selectedTipoMascota != null && !isLoading && !isSubmitting && !isLoadingRazas
}

class PetFormViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PetFormUiState())
    val uiState: StateFlow<PetFormUiState> = _uiState.asStateFlow()

    fun cargarCatalogos(token: String?, initialMascota: MascotaData? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val authHeader = if (!token.isNullOrBlank()) {
                    if (token.startsWith("Bearer ")) token else "Bearer $token"
                } else null

                val response = RetrofitClient.apiService.getApplicationParameters(authHeader)
                if (response.isSuccessful) {
                    val data = response.body()?.response
                    val tipos = data?.tiposMascota.orEmpty()
                    val razas = data?.razas.orEmpty()
                    val generos = data?.generosMascota.orEmpty()
                    val tamanos = data?.tamanosMascota.orEmpty()
                    val niveles = data?.nivelesEnergia.orEmpty()

                    _uiState.update { current ->
                        current.copy(
                            tiposMascota = tipos,
                            todasLasRazas = razas,
                            generosMascota = generos,
                            tamanosMascota = tamanos,
                            nivelesEnergia = niveles,
                            isLoading = false
                        )
                    }

                    if (initialMascota != null) {
                        inicializarParaEdicion(initialMascota, authHeader)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No se pudieron cargar los catálogos maestros (HTTP ${response.code()})"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PET_FORM_VM", "Error cargando catálogos", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error de red al cargar catálogos: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun inicializarParaEdicion(mascota: MascotaData, authHeader: String?) {
        val current = _uiState.value

        // Identificar tipo mascota
        val tipo = current.tiposMascota.find { it.id == mascota.tipoMascotaId }
            ?: current.tiposMascota.find { it.descripcion.equals(mascota.tipoMascotaDescripcion, ignoreCase = true) }

        // Filtrar razas localmente
        var razasFiltradas = if (tipo != null) {
            current.todasLasRazas.filter { it.tipoMascotaId == tipo.id }
        } else {
            emptyList()
        }

        // Si la lista local está vacía pero tenemos tipo, intentamos consultar el endpoint específico
        if (tipo != null && razasFiltradas.isEmpty()) {
            try {
                val responseRazas = RetrofitClient.apiService.getRazasPorTipo(authHeader, tipo.id)
                if (responseRazas.isSuccessful) {
                    razasFiltradas = responseRazas.body()?.extraerRazas().orEmpty()
                }
            } catch (e: Exception) {
                Log.e("PET_FORM_VM", "Error al consultar razas por tipo", e)
            }
        }

        // Identificar raza
        val raza = razasFiltradas.find { it.id == mascota.tipoRazaId }
            ?: razasFiltradas.find { it.descripcion.equals(mascota.tipoRazaDescripcion, ignoreCase = true) }
            ?: current.todasLasRazas.find { it.id == mascota.tipoRazaId }
            ?: current.todasLasRazas.find { it.descripcion.equals(mascota.tipoRazaDescripcion, ignoreCase = true) }

        // Identificar género
        val genero = current.generosMascota.find { it.id == mascota.tipoGeneroMascotaId }
            ?: current.generosMascota.find { it.descripcion.equals(mascota.tipoGeneroMascotaDescripcion, ignoreCase = true) }

        // Identificar tamaño
        val tamano = current.tamanosMascota.find { it.id == mascota.tipoTamanoMascotaId }
            ?: current.tamanosMascota.find { it.descripcion.equals(mascota.tipoTamanoMascotaDescripcion, ignoreCase = true) }

        // Identificar nivel de energía
        val nivel = current.nivelesEnergia.find { it.id == mascota.tipoNivelEnergiaId }
            ?: current.nivelesEnergia.find { it.descripcion.equals(mascota.tipoNivelEnergiaDescripcion, ignoreCase = true) }

        _uiState.update {
            it.copy(
                nombre = mascota.nombre.orEmpty(),
                selectedTipoMascota = tipo,
                razasFiltradas = razasFiltradas,
                selectedRaza = raza,
                selectedGenero = genero,
                selectedTamano = tamano,
                selectedNivelEnergia = nivel,
                edadAnos = (mascota.edadAnos ?: 0).toString(),
                edadMeses = (mascota.edadMeses ?: 0).toString(),
                pesoKg = (mascota.pesoKg ?: 0.0).toString(),
                esterilizado = mascota.esterilizado == 1,
                sociableConPerros = mascota.sociableConPerros == 1,
                sociableConPersonas = mascota.sociableConPersonas == 1,
                precaucionesMedicas = mascota.precaucionesMedicas.orEmpty(),
                fotoUrl = mascota.fotoUrl.orEmpty()
            )
        }
    }

    fun onNombreChange(nombre: String) {
        _uiState.update { it.copy(nombre = nombre, errorMessage = null) }
    }

    fun onTipoMascotaSelected(tipo: TipoMascotaParamDto, token: String? = null) {
        val currentRazas = _uiState.value.todasLasRazas.filter { it.tipoMascotaId == tipo.id }
        
        _uiState.update {
            it.copy(
                selectedTipoMascota = tipo,
                selectedRaza = null, // Se resetea el valor seleccionado de raza
                razasFiltradas = currentRazas,
                errorMessage = null
            )
        }

        // Si la lista local no tiene razas para este tipo, se consulta dinámicamente por red
        if (currentRazas.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.update { it.copy(isLoadingRazas = true) }
                try {
                    val authHeader = if (!token.isNullOrBlank()) {
                        if (token.startsWith("Bearer ")) token else "Bearer $token"
                    } else null
                    val response = RetrofitClient.apiService.getRazasPorTipo(authHeader, tipo.id)
                    if (response.isSuccessful) {
                        val razas = response.body()?.extraerRazas().orEmpty()
                        _uiState.update { it.copy(razasFiltradas = razas, isLoadingRazas = false) }
                    } else {
                        _uiState.update { it.copy(isLoadingRazas = false) }
                    }
                } catch (e: Exception) {
                    Log.e("PET_FORM_VM", "Error consultando razas dinámicamente", e)
                    _uiState.update { it.copy(isLoadingRazas = false) }
                }
            }
        }
    }

    fun onRazaSelected(raza: RazaParamDto) {
        _uiState.update { it.copy(selectedRaza = raza, errorMessage = null) }
    }

    fun onGeneroSelected(genero: GeneroMascotaParamDto) {
        _uiState.update { it.copy(selectedGenero = genero, errorMessage = null) }
    }

    fun onTamanoSelected(tamano: TamanoMascotaParamDto) {
        _uiState.update { it.copy(selectedTamano = tamano, errorMessage = null) }
    }

    fun onNivelEnergiaSelected(nivel: NivelEnergiaParamDto) {
        _uiState.update { it.copy(selectedNivelEnergia = nivel, errorMessage = null) }
    }

    fun onEdadAnosChange(edad: String) {
        val filtered = edad.filter { it.isDigit() }
        _uiState.update { it.copy(edadAnos = filtered) }
    }

    fun onEdadMesesChange(meses: String) {
        val filtered = meses.filter { it.isDigit() }
        _uiState.update { it.copy(edadMeses = filtered) }
    }

    fun onPesoKgChange(peso: String) {
        _uiState.update { it.copy(pesoKg = peso) }
    }

    fun onEsterilizadoChange(esterilizado: Boolean) {
        _uiState.update { it.copy(esterilizado = esterilizado) }
    }

    fun onSociableConPerrosChange(sociable: Boolean) {
        _uiState.update { it.copy(sociableConPerros = sociable) }
    }

    fun onSociableConPersonasChange(sociable: Boolean) {
        _uiState.update { it.copy(sociableConPersonas = sociable) }
    }

    fun onPrecaucionesMedicasChange(precauciones: String) {
        _uiState.update { it.copy(precaucionesMedicas = precauciones) }
    }

    fun onFotoUrlChange(fotoUrl: String) {
        _uiState.update { it.copy(fotoUrl = fotoUrl) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun validarFormulario(): String? {
        val state = _uiState.value
        if (state.nombre.trim().isBlank()) {
            return "Por favor ingresa el nombre de la mascota."
        }
        if (state.selectedTipoMascota == null) {
            return "Por favor selecciona el tipo de mascota (Perro, Gato, etc.)."
        }
        if (state.selectedRaza == null) {
            return "Por favor selecciona la raza de la mascota."
        }
        if (state.selectedGenero == null) {
            return "Por favor selecciona el género de la mascota."
        }
        if (state.selectedTamano == null) {
            return "Por favor selecciona el tamaño de la mascota."
        }
        if (state.selectedNivelEnergia == null) {
            return "Por favor selecciona el nivel de energía de la mascota."
        }
        val peso = state.pesoKg.toDoubleOrNull()
        if (peso == null || peso < 0.0) {
            return "Por favor ingresa un peso válido en kg."
        }
        return null
    }

    fun buildCrearMascotaRequest(usuarioUuid: String): CrearMascotaRequest? {
        val validation = validarFormulario()
        if (validation != null) {
            _uiState.update { it.copy(errorMessage = validation) }
            return null
        }
        val state = _uiState.value
        val payload = CrearMascotaPayload(
            nombre = state.nombre.trim(),
            tipoMascotaId = state.selectedTipoMascota!!.id,
            tipoRazaId = state.selectedRaza!!.id,
            tipoGeneroMascotaId = state.selectedGenero!!.id,
            tipoTamanoMascotaId = state.selectedTamano!!.id,
            tipoNivelEnergiaId = state.selectedNivelEnergia!!.id,
            edadAnos = state.edadAnos.toIntOrNull() ?: 0,
            edadMeses = state.edadMeses.toIntOrNull() ?: 0,
            pesoKg = state.pesoKg.toDoubleOrNull() ?: 0.0,
            esterilizado = if (state.esterilizado) 1 else 0,
            sociableConPerros = if (state.sociableConPerros) 1 else 0,
            sociableConPersonas = if (state.sociableConPersonas) 1 else 0,
            precaucionesMedicas = state.precaucionesMedicas.trim(),
            fotoUrl = state.fotoUrl.trim().ifBlank { null }
        )
        return CrearMascotaRequest(
            usuarioUuid = usuarioUuid,
            mascota = payload
        )
    }

    fun buildActualizarMascotaRequest(): ActualizarMascotaRequest? {
        val validation = validarFormulario()
        if (validation != null) {
            _uiState.update { it.copy(errorMessage = validation) }
            return null
        }
        val state = _uiState.value
        return ActualizarMascotaRequest(
            nombre = state.nombre.trim(),
            tipoMascotaId = state.selectedTipoMascota!!.id,
            tipoRazaId = state.selectedRaza!!.id,
            tipoGeneroMascotaId = state.selectedGenero!!.id,
            tipoTamanoMascotaId = state.selectedTamano!!.id,
            tipoNivelEnergiaId = state.selectedNivelEnergia!!.id,
            edadAnos = state.edadAnos.toIntOrNull() ?: 0,
            edadMeses = state.edadMeses.toIntOrNull() ?: 0,
            pesoKg = state.pesoKg.toDoubleOrNull() ?: 0.0,
            esterilizado = if (state.esterilizado) 1 else 0,
            sociableConPerros = if (state.sociableConPerros) 1 else 0,
            sociableConPersonas = if (state.sociableConPersonas) 1 else 0,
            precaucionesMedicas = state.precaucionesMedicas.trim(),
            fotoUrl = state.fotoUrl.trim().ifBlank { null }
        )
    }

    fun submitCrearMascota(
        token: String,
        usuarioUuid: String,
        onSuccess: () -> Unit
    ) {
        val request = buildCrearMascotaRequest(usuarioUuid) ?: return
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val response = RetrofitClient.apiService.crearMascota(authHeader, request)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isSubmitting = false, success = true) }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val msg = response.message().ifBlank { "HTTP ${response.code()}" }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Error al crear mascota: $msg"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PET_FORM_VM", "Error al crear mascota", e)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Error de red al crear mascota: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun submitActualizarMascota(
        token: String,
        mascotaUuid: String,
        onSuccess: () -> Unit
    ) {
        val request = buildActualizarMascotaRequest() ?: return
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val response = RetrofitClient.apiService.actualizarMascota(authHeader, mascotaUuid, request)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isSubmitting = false, success = true) }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val msg = response.message().ifBlank { "HTTP ${response.code()}" }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Error al actualizar mascota: $msg"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PET_FORM_VM", "Error al actualizar mascota", e)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Error de red al actualizar mascota: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
