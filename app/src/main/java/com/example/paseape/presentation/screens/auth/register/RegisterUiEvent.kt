package com.example.paseape.presentation.screens.auth.register

sealed interface RegisterUiEvent {
    data class RoleChanged(val tipoUsuarioId: Int) : RegisterUiEvent
    data class NombresChanged(val nombres: String) : RegisterUiEvent
    data class ApellidosChanged(val apellidos: String) : RegisterUiEvent
    data class CorreoChanged(val correo: String) : RegisterUiEvent
    data class ContrasenaChanged(val contrasena: String) : RegisterUiEvent
    data class ConfirmarContrasenaChanged(val confirmar: String) : RegisterUiEvent
    data class TelefonoChanged(val telefono: String) : RegisterUiEvent

    // Eventos Cliente
    data class DireccionReferenciaChanged(val direccion: String) : RegisterUiEvent
    data class DistritoSelected(val distritoId: Int) : RegisterUiEvent
    data class ContactoEmergenciaNombreChanged(val nombre: String) : RegisterUiEvent
    data class ContactoEmergenciaTelefonoChanged(val telefono: String) : RegisterUiEvent
    data class NotasAdicionalesChanged(val notas: String) : RegisterUiEvent

    // Mascotas
    data class SetWantsToRegisterPets(val wants: Boolean) : RegisterUiEvent
    data object AddPetClicked : RegisterUiEvent
    data class RemovePetClicked(val tempId: String) : RegisterUiEvent
    data class PetNombreChanged(val tempId: String, val nombre: String) : RegisterUiEvent
    data class PetTipoMascotaSelected(val tempId: String, val tipoId: Int) : RegisterUiEvent
    data class PetRazaSelected(val tempId: String, val razaId: Int) : RegisterUiEvent
    data class PetGeneroSelected(val tempId: String, val generoId: Int) : RegisterUiEvent
    data class PetTamanoSelected(val tempId: String, val tamanoId: Int) : RegisterUiEvent
    data class PetNivelEnergiaSelected(val tempId: String, val nivelId: Int) : RegisterUiEvent
    data class PetEdadAnosChanged(val tempId: String, val anos: String) : RegisterUiEvent
    data class PetEdadMesesChanged(val tempId: String, val meses: String) : RegisterUiEvent
    data class PetPesoChanged(val tempId: String, val peso: String) : RegisterUiEvent
    data class PetEsterilizadoChanged(val tempId: String, val esterilizado: Boolean) : RegisterUiEvent
    data class PetSociableConPerrosChanged(val tempId: String, val sociable: Boolean) : RegisterUiEvent
    data class PetSociableConPersonasChanged(val tempId: String, val sociable: Boolean) : RegisterUiEvent
    data class PetPrecaucionesChanged(val tempId: String, val precauciones: String) : RegisterUiEvent
    data class PetFotoUrlChanged(val tempId: String, val url: String) : RegisterUiEvent

    // Eventos Paseador
    data class TipoDocumentoSelected(val id: Int) : RegisterUiEvent
    data class NumeroDocumentoChanged(val numero: String) : RegisterUiEvent
    data class ExperienciaAnosChanged(val anos: String) : RegisterUiEvent
    data class TarifaHoraChanged(val tarifa: String) : RegisterUiEvent
    data class DistritoCoberturaSelected(val id: Int) : RegisterUiEvent
    data class BiografiaChanged(val bio: String) : RegisterUiEvent
    data class AntecedentesUrlChanged(val url: String) : RegisterUiEvent

    data object SubmitRegistration : RegisterUiEvent
    data object DismissError : RegisterUiEvent
}