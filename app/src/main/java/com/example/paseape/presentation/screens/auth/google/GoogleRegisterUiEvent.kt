package com.example.paseape.presentation.screens.auth.google

import android.content.Context

sealed interface GoogleRegisterUiEvent {
    data class RequestGoogleSignIn(val activityContext: Context) : GoogleRegisterUiEvent
    data class RoleChanged(val tipoUsuarioId: Int) : GoogleRegisterUiEvent
    data class TelefonoChanged(val telefono: String) : GoogleRegisterUiEvent
    data class DireccionReferenciaChanged(val direccion: String) : GoogleRegisterUiEvent
    data class DistritoSelected(val distritoId: Int) : GoogleRegisterUiEvent
    data class ContactoEmergenciaNombreChanged(val nombre: String) : GoogleRegisterUiEvent
    data class ContactoEmergenciaTelefonoChanged(val telefono: String) : GoogleRegisterUiEvent
    data class NotasAdicionalesChanged(val notas: String) : GoogleRegisterUiEvent

    // Eventos Mascotas
    data class SetWantsToRegisterPets(val wants: Boolean) : GoogleRegisterUiEvent
    data object AddPetClicked : GoogleRegisterUiEvent
    data class RemovePetClicked(val tempId: String) : GoogleRegisterUiEvent
    data class PetNombreChanged(val tempId: String, val nombre: String) : GoogleRegisterUiEvent
    data class PetTipoMascotaSelected(val tempId: String, val tipoId: Int) : GoogleRegisterUiEvent
    data class PetRazaSelected(val tempId: String, val razaId: Int) : GoogleRegisterUiEvent
    data class PetGeneroSelected(val tempId: String, val generoId: Int) : GoogleRegisterUiEvent
    data class PetTamanoSelected(val tempId: String, val tamanoId: Int) : GoogleRegisterUiEvent
    data class PetNivelEnergiaSelected(val tempId: String, val nivelId: Int) : GoogleRegisterUiEvent
    data class PetEdadAnosChanged(val tempId: String, val anos: String) : GoogleRegisterUiEvent
    data class PetEdadMesesChanged(val tempId: String, val meses: String) : GoogleRegisterUiEvent
    data class PetPesoChanged(val tempId: String, val peso: String) : GoogleRegisterUiEvent
    data class PetEsterilizadoChanged(val tempId: String, val esterilizado: Boolean) : GoogleRegisterUiEvent
    data class PetPrecaucionesChanged(val tempId: String, val precauciones: String) : GoogleRegisterUiEvent

    // Eventos Paseador
    data class TipoDocumentoSelected(val id: Int) : GoogleRegisterUiEvent
    data class NumeroDocumentoChanged(val numero: String) : GoogleRegisterUiEvent
    data class ExperienciaAnosChanged(val anos: String) : GoogleRegisterUiEvent
    data class TarifaHoraChanged(val tarifa: String) : GoogleRegisterUiEvent
    data class DistritoCoberturaSelected(val id: Int) : GoogleRegisterUiEvent
    data class BiografiaChanged(val bio: String) : GoogleRegisterUiEvent
    data class AntecedentesUrlChanged(val url: String) : GoogleRegisterUiEvent

    data object SubmitRegistration : GoogleRegisterUiEvent
    data object DismissError : GoogleRegisterUiEvent
}