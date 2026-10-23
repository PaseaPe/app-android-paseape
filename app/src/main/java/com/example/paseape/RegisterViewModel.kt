package com.example.paseape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.RegistroRequest
import com.example.paseape.data.model.TipoDocumentoParamDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tipos de usuario permitidos por el backend: 1 = Cliente (Dueño), 2 = Paseador. */
const val TIPO_USUARIO_CLIENTE = 1
const val TIPO_USUARIO_PASEADOR = 2

/** Campos del formulario que pueden mostrar un error debajo del control. */
enum class RegisterField {
    NOMBRES,
    APELLIDOS,
    CORREO,
    CONTRASENA,
    TELEFONO,
    DISTRITO,
    DIRECCION_REFERENCIA,
    CONTACTO_NOMBRE,
    CONTACTO_TELEFONO,
    TIPO_DOCUMENTO,
    NUMERO_DOCUMENTO,
    DISTRITO_COBERTURA,
    EXPERIENCIA,
    TARIFA,
    BIOGRAFIA
}

data class RegisterUiState(
    // Paso 1
    val tipoUsuarioId: Int = TIPO_USUARIO_CLIENTE,
    val nombres: String = "",
    val apellidos: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val telefono: String = "",

    // Paso 2 - Cliente
    val distritoSeleccionado: DistritoParamDto? = null,
    val direccionReferencia: String = "",
    val contactoEmergenciaNombre: String = "",
    val contactoEmergenciaTelefono: String = "",

    // Paso 2 - Paseador
    val tipoDocumentoSeleccionado: TipoDocumentoParamDto? = null,
    val numeroDocumento: String = "",
    val distritoCoberturaSeleccionado: DistritoParamDto? = null,
    val experienciaAnos: String = "",
    val tarifaHora: String = "",
    val biografia: String = "",

    // Catálogos del backend
    val distritos: List<DistritoParamDto> = emptyList(),
    val tiposDocumento: List<TipoDocumentoParamDto> = emptyList(),
    val isLoadingCatalogos: Boolean = false,
    val catalogosError: String? = null,

    // Estado de envío
    val isSubmitting: Boolean = false,
    val fieldErrors: Map<RegisterField, String> = emptyMap(),
    val errorGeneral: String? = null,
    val registroExitoso: Boolean = false
)

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        cargarCatalogos()
    }

    // ------------------------------------------------------------------
    // Catálogos (distritos y tipos de documento)
    // ------------------------------------------------------------------
    fun cargarCatalogos() {
        if (_uiState.value.isLoadingCatalogos) return

        _uiState.update { it.copy(isLoadingCatalogos = true, catalogosError = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // El endpoint es público: no se envía token durante el registro.
                val response = RetrofitClient.apiService.getApplicationParameters(null)
                val data = response.body()?.response

                if (response.isSuccessful && data != null) {
                    _uiState.update {
                        it.copy(
                            distritos = data.distritos.orEmpty().sortedBy { d -> d.descripcion },
                            tiposDocumento = data.tiposDocumento.orEmpty(),
                            isLoadingCatalogos = false,
                            catalogosError = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingCatalogos = false,
                            catalogosError = "No se pudieron cargar los catálogos. Inténtalo de nuevo."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCatalogos = false,
                        catalogosError = "No se pudieron cargar los catálogos. Revisa tu conexión e inténtalo de nuevo."
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Eventos de la UI
    // ------------------------------------------------------------------
    fun onTipoUsuarioChange(tipoUsuarioId: Int) {
        if (tipoUsuarioId != TIPO_USUARIO_CLIENTE && tipoUsuarioId != TIPO_USUARIO_PASEADOR) return

        _uiState.update {
            it.copy(
                tipoUsuarioId = tipoUsuarioId,
                fieldErrors = it.fieldErrors - setOf(
                    RegisterField.DISTRITO,
                    RegisterField.TIPO_DOCUMENTO,
                    RegisterField.NUMERO_DOCUMENTO,
                    RegisterField.DISTRITO_COBERTURA
                ),
                errorGeneral = null
            )
        }
    }

    fun onFieldChange(field: RegisterField, value: String) {
        _uiState.update { state ->
            val actualizado = when (field) {
                RegisterField.NOMBRES -> state.copy(nombres = value)
                RegisterField.APELLIDOS -> state.copy(apellidos = value)
                RegisterField.CORREO -> state.copy(correo = value.trim())
                RegisterField.CONTRASENA -> state.copy(contrasena = value)
                RegisterField.TELEFONO -> state.copy(telefono = value.take(15))
                RegisterField.DIRECCION_REFERENCIA -> state.copy(direccionReferencia = value)
                RegisterField.CONTACTO_NOMBRE -> state.copy(contactoEmergenciaNombre = value)
                RegisterField.CONTACTO_TELEFONO ->
                    state.copy(contactoEmergenciaTelefono = value.take(15))
                RegisterField.EXPERIENCIA ->
                    state.copy(experienciaAnos = value.filter { it.isDigit() }.take(2))
                RegisterField.TARIFA ->
                    state.copy(tarifaHora = value.filter { it.isDigit() || it == '.' }.take(7))
                RegisterField.BIOGRAFIA -> state.copy(biografia = value)
                else -> state
            }
            actualizado.copy(
                fieldErrors = actualizado.fieldErrors - field,
                errorGeneral = null
            )
        }
    }

    fun onDistritoSelected(distrito: DistritoParamDto) {
        _uiState.update {
            it.copy(
                distritoSeleccionado = distrito,
                fieldErrors = it.fieldErrors - RegisterField.DISTRITO,
                errorGeneral = null
            )
        }
    }

    fun onDistritoCoberturaSelected(distrito: DistritoParamDto) {
        _uiState.update {
            it.copy(
                distritoCoberturaSeleccionado = distrito,
                fieldErrors = it.fieldErrors - RegisterField.DISTRITO_COBERTURA,
                errorGeneral = null
            )
        }
    }

    fun onTipoDocumentoSelected(tipoDocumento: TipoDocumentoParamDto) {
        _uiState.update {
            it.copy(
                tipoDocumentoSeleccionado = tipoDocumento,
                numeroDocumento = "",
                fieldErrors = it.fieldErrors - setOf(
                    RegisterField.TIPO_DOCUMENTO,
                    RegisterField.NUMERO_DOCUMENTO
                ),
                errorGeneral = null
            )
        }
    }

    fun onNumeroDocumentoChange(value: String) {
        val tipoDocumentoId = _uiState.value.tipoDocumentoSeleccionado?.id

        val filtrado = when (tipoDocumentoId) {
            ID_TIPO_DOCUMENTO_DNI -> value.filter { it.isDigit() }.take(8)
            ID_TIPO_DOCUMENTO_CE -> value.filter { it.isDigit() }.take(9)
            ID_TIPO_DOCUMENTO_PASAPORTE ->
                value.uppercase().filter { it.isLetterOrDigit() }.take(9)
            else -> value
        }

        onFieldChange(RegisterField.NUMERO_DOCUMENTO, filtrado)
    }

    // ------------------------------------------------------------------
    // Validación (espejo de las reglas del backend)
    // ------------------------------------------------------------------
    fun validarPaso1(): Boolean = aplicarErrores(erroresPaso1(_uiState.value))

    fun validarPaso2(): Boolean = aplicarErrores(erroresPaso2(_uiState.value))

    private fun erroresPaso1(state: RegisterUiState): Map<RegisterField, String> {
        val errores = linkedMapOf<RegisterField, String>()

        when {
            state.nombres.isBlank() ->
                errores[RegisterField.NOMBRES] = "Los nombres son obligatorios."
            state.nombres.trim().length > 100 ->
                errores[RegisterField.NOMBRES] = "Máximo 100 caracteres."
        }
        when {
            state.apellidos.isBlank() ->
                errores[RegisterField.APELLIDOS] = "Los apellidos son obligatorios."
            state.apellidos.trim().length > 100 ->
                errores[RegisterField.APELLIDOS] = "Máximo 100 caracteres."
        }
        if (state.correo.isBlank()) {
            errores[RegisterField.CORREO] = "El correo es obligatorio."
        } else if (!EMAIL_REGEX.matches(state.correo.trim())) {
            errores[RegisterField.CORREO] = "Ingresa un correo válido."
        }
        if (state.contrasena.length !in 6..50) {
            errores[RegisterField.CONTRASENA] = "La contraseña debe tener entre 6 y 50 caracteres."
        }
        if (state.telefono.isBlank()) {
            errores[RegisterField.TELEFONO] = "El teléfono es obligatorio."
        } else if (state.telefono.count { it.isDigit() } < 6) {
            errores[RegisterField.TELEFONO] = "Ingresa un teléfono válido."
        }

        return errores
    }

    private fun erroresPaso2(state: RegisterUiState): Map<RegisterField, String> {
        val errores = linkedMapOf<RegisterField, String>()

        if (state.tipoUsuarioId == TIPO_USUARIO_CLIENTE) {
            if (state.distritoSeleccionado == null) {
                errores[RegisterField.DISTRITO] = "Selecciona tu distrito."
            }
        } else {
            val tipoDocumento = state.tipoDocumentoSeleccionado
            if (tipoDocumento == null) {
                errores[RegisterField.TIPO_DOCUMENTO] = "Selecciona el tipo de documento."
            } else {
                val numero = state.numeroDocumento.trim()
                when {
                    numero.isBlank() ->
                        errores[RegisterField.NUMERO_DOCUMENTO] =
                            "El número de documento es obligatorio."
                    tipoDocumento.id == ID_TIPO_DOCUMENTO_DNI &&
                        !Regex("^[0-9]{8}$").matches(numero) ->
                        errores[RegisterField.NUMERO_DOCUMENTO] = "El DNI debe tener 8 dígitos."
                    tipoDocumento.id == ID_TIPO_DOCUMENTO_CE &&
                        !Regex("^[0-9]{8,9}$").matches(numero) ->
                        errores[RegisterField.NUMERO_DOCUMENTO] =
                            "El carnet de extranjería debe tener 8 o 9 dígitos."
                    tipoDocumento.id == ID_TIPO_DOCUMENTO_PASAPORTE &&
                        !Regex("^[A-Z0-9]{9}$").matches(numero) ->
                        errores[RegisterField.NUMERO_DOCUMENTO] =
                            "El pasaporte debe tener 9 caracteres alfanuméricos."
                }
            }

            if (state.distritoCoberturaSeleccionado == null) {
                errores[RegisterField.DISTRITO_COBERTURA] =
                    "Selecciona el distrito donde brindas cobertura."
            }

            if (state.tarifaHora.isNotBlank()) {
                val tarifa = state.tarifaHora.toDoubleOrNull()
                if (tarifa == null || tarifa < 0) {
                    errores[RegisterField.TARIFA] = "Ingresa una tarifa válida."
                }
            }

            if (state.experienciaAnos.isNotBlank()) {
                val experiencia = state.experienciaAnos.toIntOrNull()
                if (experiencia == null || experiencia < 0) {
                    errores[RegisterField.EXPERIENCIA] = "Ingresa una experiencia válida."
                }
            }
        }

        return errores
    }

    private fun aplicarErrores(errores: Map<RegisterField, String>): Boolean {
        _uiState.update { it.copy(fieldErrors = errores, errorGeneral = null) }
        return errores.isEmpty()
    }

    // ------------------------------------------------------------------
    // Registro
    // ------------------------------------------------------------------
    fun registrar() {
        val state = _uiState.value
        if (state.isSubmitting) return

        if (state.isLoadingCatalogos) {
            _uiState.update {
                it.copy(errorGeneral = "Espera a que terminen de cargar los catálogos.")
            }
            return
        }

        val catalogosListos = if (state.tipoUsuarioId == TIPO_USUARIO_CLIENTE) {
            state.distritos.isNotEmpty()
        } else {
            state.distritos.isNotEmpty() && state.tiposDocumento.isNotEmpty()
        }
        if (!catalogosListos) {
            _uiState.update {
                it.copy(
                    errorGeneral = "No se pudieron cargar los catálogos. Presiona Reintentar para continuar."
                )
            }
            return
        }

        val errores = erroresPaso1(state) + erroresPaso2(state)
        if (errores.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    fieldErrors = errores,
                    errorGeneral = "Revisa los campos marcados."
                )
            }
            return
        }

        _uiState.update {
            it.copy(isSubmitting = true, fieldErrors = emptyMap(), errorGeneral = null)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.registrarUsuario(construirRequest(_uiState.value))

                if (response.isSuccessful) {
                    _uiState.update { it.copy(isSubmitting = false, registroExitoso = true) }
                } else {
                    val mensaje = ApiErrorMapper.fromResponse(response, ApiErrorContext.REGISTER)
                    _uiState.update { it.copy(isSubmitting = false, errorGeneral = mensaje) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorGeneral = ApiErrorMapper.fromThrowable(e, ApiErrorContext.REGISTER)
                    )
                }
            }
        }
    }

    private fun construirRequest(state: RegisterUiState): RegistroRequest {
        val telefono = state.telefono.trim().ifBlank { null }

        return if (state.tipoUsuarioId == TIPO_USUARIO_CLIENTE) {
            RegistroRequest(
                nombres = state.nombres.trim(),
                apellidos = state.apellidos.trim(),
                correo = state.correo.trim(),
                contrasena = state.contrasena,
                telefono = telefono,
                tipoUsuarioId = TIPO_USUARIO_CLIENTE,
                direccionReferencia = state.direccionReferencia.trim().ifBlank { null },
                distritoId = state.distritoSeleccionado?.id,
                contactoEmergenciaNombre =
                    state.contactoEmergenciaNombre.trim().ifBlank { null },
                contactoEmergenciaTelefono =
                    state.contactoEmergenciaTelefono.trim().ifBlank { null },
                notasAdicionales = null,
                mascotas = null
            )
        } else {
            RegistroRequest(
                nombres = state.nombres.trim(),
                apellidos = state.apellidos.trim(),
                correo = state.correo.trim(),
                contrasena = state.contrasena,
                telefono = telefono,
                tipoUsuarioId = TIPO_USUARIO_PASEADOR,
                tipoDocumentoId = state.tipoDocumentoSeleccionado?.id,
                numeroDocumento = state.numeroDocumento.trim().let { numero ->
                    if (state.tipoDocumentoSeleccionado?.id == ID_TIPO_DOCUMENTO_PASAPORTE) {
                        numero.uppercase()
                    } else {
                        numero
                    }
                },
                experienciaAnos = state.experienciaAnos.trim().toIntOrNull(),
                biografia = state.biografia.trim().ifBlank { null },
                tarifaHoraPen = state.tarifaHora.trim().toDoubleOrNull(),
                distritoCoberturaId = state.distritoCoberturaSeleccionado?.id
            )
        }
    }

    companion object {
        private const val ID_TIPO_DOCUMENTO_DNI = 1
        private const val ID_TIPO_DOCUMENTO_CE = 2
        private const val ID_TIPO_DOCUMENTO_PASAPORTE = 3

        private val EMAIL_REGEX =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
