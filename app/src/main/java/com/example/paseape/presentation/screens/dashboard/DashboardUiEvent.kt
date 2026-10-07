package com.example.paseape.presentation.screens.dashboard

sealed interface DashboardUiEvent {
    data class SearchQueryChanged(val query: String) : DashboardUiEvent
    data class CategorySelected(val category: String) : DashboardUiEvent
    data object LogoutRequested : DashboardUiEvent
}
