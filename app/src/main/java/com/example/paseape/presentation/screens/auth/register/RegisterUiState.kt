package com.example.paseape.presentation.screens.auth.register

import com.example.paseape.core.util.AppConstants
import com.example.paseape.domain.model.ApplicationParameters
import com.example.paseape.presentation.screens.auth.google.PetFormState

data class RegisterUiState(
    val selectedTipoUsuarioId: Int = AppConstants.TIPO_USUARIO_CLIENTE_ID,
    val parameters: ApplicationParameters = ApplicationParameters(),

    // Datos Básicos Comunes
    val nombres: String = "",
    val nombresError: String? = null,
    val apellidos: String = "",
    val apellidosError: String? = null,
    val correo: String = "",
    val correoError: String? = null,
    val contrasena: String = "",
    val contrasenaError: String? = null,
    val confirmarContrasena: String = "",
    val confirmarContrasenaError: String? = null,
    val telefono: String = "",
    val telefonoError: String? = null,

    // Datos Específicos Cliente
    val direccionReferencia: String = "",
    val direccionReferenciaError: String? = null,
    val distritoId: Int? = null,
    val distritoError: String? = null,
    val contactoEmergenciaNombre: String = "",
    val contactoEmergenciaTelefono: String = "",
    val notasAdicionales: String = "",

    // Mascotas
    val wantsToRegisterPets: Boolean = false,
    val mascotas: List<PetFormState> = emptyList(),

    // Datos Específicos Paseador
    val tipoDocumentoId: Int? = null,
    val tipoDocumentoError: String? = null,
    val numeroDocumento: String = "",
    val numeroDocumentoError: String? = null,
    val experienciaAnos: String = "",
    val experienciaAnosError: String? = null,
    val tarifaHoraPen: String = "",
    val tarifaHoraPenError: String? = null,
    val distritoCoberturaId: Int? = null,
    val distritoCoberturaError: String? = null,
    val biografia: String = "",
    val biografiaError: String? = null,
    val antecedentesPolicialesUrl: String = "",

    // Estados de control
    val isLoading: Boolean = false,
    val isParametersLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRegistrationSuccess: Boolean = false
)