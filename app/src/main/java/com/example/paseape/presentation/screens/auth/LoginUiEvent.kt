package com.example.paseape.presentation.screens.auth

sealed interface LoginUiEvent {
    data class EmailChanged(val email: String) : LoginUiEvent
    data class PasswordChanged(val password: String) : LoginUiEvent
    data object SubmitLogin : LoginUiEvent
    data object DismissError : LoginUiEvent
}
