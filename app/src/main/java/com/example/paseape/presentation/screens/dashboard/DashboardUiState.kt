package com.example.paseape.presentation.screens.dashboard

data class DashboardUiState(
    val userName: String = "",
    val userRole: String = "",
    val searchQuery: String = "",
    val selectedCategory: String = "Paseo",
    val categories: List<String> = listOf("Paseo", "Cuidado", "Adiestramiento"),
    val isLoading: Boolean = false
)
