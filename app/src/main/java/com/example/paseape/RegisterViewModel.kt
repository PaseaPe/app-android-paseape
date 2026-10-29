package com.example.paseape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.DistritoParamDto
import com.example.paseape.data.model.GoogleAuthReq
import com.example.paseape.data.model.GoogleAuthResponse
import com.example.paseape.data.model.RegistroGoogleRequest
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
import kotlinx.coroutines.withContext
import retrofit2.HttpException

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

/** Resultado de consultar si el correo de Google ya tiene una cuenta. */
sealed interface ChequeoCuentaGoogle {
    data object NoRegistrada : ChequeoCuentaGoogle
    data object Registrada : ChequeoCuentaGoogle
    data class Error(val mensaje: String) : ChequeoCuentaGoogle
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
    val registroExitoso: Boolean = false,

    // Registro con Google
    val modoGoogle: Boolean = false,
    val googleEmail: String = "",
    val volverAPaso1: Boolean = false,

    // Sesión devuelta por el backend al registrar con Google
    val authToken: String? = null,
    val authUuid: String? = null,
    val authRole: Int? = null
)

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /** Token de Google solo en memoria: nunca se persiste en preferencias. */
    private var googleIdToken: String? = null

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
                RegisterField.NUMERO_DOCUMENTO -> state.copy(numeroDocumento = value)
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
    // Registro con Google
    // ------------------------------------------------------------------

    /**
     * Verifica si la cuenta de Google ya tiene un usuario registrado.
     *
     * Reutiliza `POST /auth/google` solo como consulta: no guarda la sesión ni
     * expone el token devuelto. La app únicamente necesita saber si el correo
     * ya existe para avisar en el paso 1, sin esperar al envío del formulario.
     */
    suspend fun chequearCuentaGoogle(idToken: String): ChequeoCuentaGoogle {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.loginWithGoogle(GoogleAuthReq(idToken))
                if (!response.effectiveToken.isNullOrBlank()) {
                    ChequeoCuentaGoogle.Registrada
                } else {
                    ChequeoCuentaGoogle.Error(
                        response.message?.takeIf { it.isNotBlank() }
                            ?: "No pudimos verificar tu cuenta de Google. Inténtalo de nuevo."
                    )
                }
            } catch (e: HttpException) {
                val cuerpo = runCatching { e.response()?.errorBody()?.string() }
                    .getOrNull()
                    .orEmpty()

                if (cuerpo.contains("no se encuentra registrado", ignoreCase = true)) {
                    ChequeoCuentaGoogle.NoRegistrada
                } else {
                    ChequeoCuentaGoogle.Error(
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.LOGIN)
                    )
                }
            } catch (e: Exception) {
                ChequeoCuentaGoogle.Error(
                    ApiErrorMapper.fromThrowable(e, ApiErrorContext.LOGIN)
                )
            }
        }
    }

    /**
     * Guarda el token obtenido con Credential Manager y prellena los datos que
     * el usuario aún no haya escrito. La pantalla permanece en el paso 1 para
     * que complete el teléfono antes de continuar.
     */
    fun onGoogleCredential(idToken: String, email: String, nombreCompleto: String?) {
        if (idToken.isBlank()) return

        val correoLimpio = email.trim().lowercase()
        val partesNombre = nombreCompleto.orEmpty().trim()
            .split(Regex("\\s+"), limit = 2)

        googleIdToken = idToken
        _uiState.update { state ->
            state.copy(
                modoGoogle = true,
                googleEmail = correoLimpio,
                correo = correoLimpio,
                contrasena = "",
                nombres = state.nombres.ifBlank { partesNombre.getOrNull(0).orEmpty() },
                apellidos = state.apellidos.ifBlank { partesNombre.getOrNull(1).orEmpty() },
                fieldErrors = state.fieldErrors - setOf(
                    RegisterField.CORREO,
                    RegisterField.CONTRASENA
                ),
                errorGeneral = null,
                volverAPaso1 = false
            )
        }
    }

    /** Descarta la cuenta de Google y vuelve al registro con correo y contraseña. */
    fun salirDeModoGoogle() {
        googleIdToken = null
        _uiState.update {
            it.copy(
                modoGoogle = false,
                googleEmail = "",
                correo = "",
                contrasena = "",
                fieldErrors = it.fieldErrors - setOf(
                    RegisterField.CORREO,
                    RegisterField.CONTRASENA
                ),
                errorGeneral = null,
                volverAPaso1 = false
            )
        }
    }

    /** La pantalla ya regresó al paso 1 tras expirar el token de Google. */
    fun onVolverAPaso1Aplicado() {
        _uiState.update { it.copy(volverAPaso1 = false) }
    }

    /** Errores del selector de Google que no vienen del backend. */
    fun mostrarErrorGoogle(mensaje: String) {
        _uiState.update {
            it.copy(errorGeneral = mensaje, volverAPaso1 = false)
        }
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
        if (!state.modoGoogle) {
            if (state.correo.isBlank()) {
                errores[RegisterField.CORREO] = "El correo es obligatorio."
            } else if (!EMAIL_REGEX.matches(state.correo.trim())) {
                errores[RegisterField.CORREO] = "Ingresa un correo válido."
            }
            if (state.contrasena.length !in 6..50) {
                errores[RegisterField.CONTRASENA] = "La contraseña debe tener entre 6 y 50 caracteres."
            }
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
            it.copy(
                isSubmitting = true,
                fieldErrors = emptyMap(),
                errorGeneral = null
            )
        }

        if (state.modoGoogle) {
            registrarConGoogle(state)
            return
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

    /**
     * Registra la cuenta usando el token de Google ya obtenido. El backend
     * devuelve el JWT, el uuid y el rol para iniciar sesión automáticamente.
     */
    private fun registrarConGoogle(state: RegisterUiState) {
        val idToken = googleIdToken
        if (idToken.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    modoGoogle = false,
                    errorGeneral = "No pudimos recuperar tu cuenta de Google. Inténtalo de nuevo.",
                    volverAPaso1 = true
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.registrarConGoogle(
                    construirRequestGoogle(state, idToken)
                )

                val token = response.effectiveToken
                if (!token.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            registroExitoso = true,
                            authToken = token,
                            authUuid = response.effectiveUuid,
                            authRole = resolverRol(response)
                        )
                    }
                } else {
                    val mensaje = response.message?.takeIf { it.isNotBlank() }
                        ?: "No pudimos completar el registro con Google. Inténtalo de nuevo."
                    _uiState.update { it.copy(isSubmitting = false, errorGeneral = mensaje) }
                }
            } catch (e: Exception) {
                val mensaje = ApiErrorMapper.fromThrowable(e, ApiErrorContext.REGISTER)
                val lower = mensaje.lowercase()
                val tokenInvalido = lower.contains("idtoken") ||
                    lower.contains("id_token") ||
                    lower.contains("expiro")

                if (tokenInvalido) {
                    googleIdToken = null
                }

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorGeneral = mensaje,
                        volverAPaso1 = tokenInvalido,
                        modoGoogle = if (tokenInvalido) false else it.modoGoogle,
                        googleEmail = if (tokenInvalido) "" else it.googleEmail,
                        correo = if (tokenInvalido) "" else it.correo
                    )
                }
            }
        }
    }

    private fun resolverRol(response: GoogleAuthResponse): Int {
        val role = response.effectiveRole?.uppercase()
        return when {
            role == "ADMIN" -> 99
            role == "PASEADOR" || response.effectiveUserTypeId == TIPO_USUARIO_PASEADOR ->
                TIPO_USUARIO_PASEADOR
            else -> TIPO_USUARIO_CLIENTE
        }
    }

    private fun construirRequestGoogle(
        state: RegisterUiState,
        idToken: String
    ): RegistroGoogleRequest {
        val telefono = state.telefono.trim().ifBlank { null }
        val nombres = state.nombres.trim().ifBlank { null }
        val apellidos = state.apellidos.trim().ifBlank { null }

        return if (state.tipoUsuarioId == TIPO_USUARIO_CLIENTE) {
            RegistroGoogleRequest(
                idToken = idToken,
                tipoUsuarioId = TIPO_USUARIO_CLIENTE,
                nombres = nombres,
                apellidos = apellidos,
                telefono = telefono,
                direccionReferencia = state.direccionReferencia.trim().ifBlank { null },
                distritoId = state.distritoSeleccionado?.id,
                contactoEmergenciaNombre =
                    state.contactoEmergenciaNombre.trim().ifBlank { null },
                contactoEmergenciaTelefono =
                    state.contactoEmergenciaTelefono.trim().ifBlank { null }
            )
        } else {
            RegistroGoogleRequest(
                idToken = idToken,
                tipoUsuarioId = TIPO_USUARIO_PASEADOR,
                nombres = nombres,
                apellidos = apellidos,
                telefono = telefono,
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
