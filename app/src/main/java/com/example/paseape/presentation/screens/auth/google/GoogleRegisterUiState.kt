package com.example.paseape.presentation.screens.auth.google

import com.example.paseape.core.util.AppConstants
import com.example.paseape.domain.model.ApplicationParameters
import com.example.paseape.domain.model.CatalogItem

data class PetFormState(
    val tempId: String = java.util.UUID.randomUUID().toString(),
    val nombre: String = "",
    val tipoMascotaId: Int? = null,
    val tipoRazaId: Int? = null,
    val tipoGeneroMascotaId: Int? = null,
    val tipoTamanoMascotaId: Int? = null,
    val tipoNivelEnergiaId: Int? = null,
    val edadAnos: String = "",
    val edadMeses: String = "",
    val pesoKg: String = "",
    val esterilizado: Boolean = false,
    val sociableConPerros: Boolean = true,
    val sociableConPersonas: Boolean = true,
    val precaucionesMedicas: String = "",
    val fotoUrl: String = "",
    val availableRazas: List<CatalogItem> = emptyList(),
    val isLoadingRazas: Boolean = false
)

data class GoogleRegisterUiState(
    val idToken: String = "",
    val selectedTipoUsuarioId: Int = AppConstants.TIPO_USUARIO_CLIENTE_ID,
    val parameters: ApplicationParameters = ApplicationParameters(),

    // Campos Cliente
    val telefono: String = "",
    val direccionReferencia: String = "",
    val distritoId: Int? = null,
    val contactoEmergenciaNombre: String = "",
    val contactoEmergenciaTelefono: String = "",
    val notasAdicionales: String = "",

    // Sección Mascotas
    val wantsToRegisterPets: Boolean = false,
    val mascotas: List<PetFormState> = emptyList(),

    // Campos Paseador
    val tipoDocumentoId: Int? = null,
    val numeroDocumento: String = "",
    val experienciaAnos: String = "",
    val tarifaHoraPen: String = "",
    val distritoCoberturaId: Int? = null,
    val biografia: String = "",
    val antecedentesPolicialesUrl: String = "",

    // Estados de pantalla
    val isLoading: Boolean = false,
    val isParametersLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRegistrationSuccess: Boolean = false
)