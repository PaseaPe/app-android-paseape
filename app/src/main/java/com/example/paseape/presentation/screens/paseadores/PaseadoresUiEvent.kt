package com.example.paseape.presentation.screens.paseadores

sealed interface PaseadoresUiEvent {
    data class SearchQueryChanged(val query: String) : PaseadoresUiEvent
    data object RetryLoading : PaseadoresUiEvent
    data object DismissError : PaseadoresUiEvent
}
