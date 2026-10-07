package com.example.paseape.presentation.screens.paseadores

import com.example.paseape.domain.model.Paseador

data class PaseadoresUiState(
    val paseadores: List<Paseador> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val paseadoresFiltrados: List<Paseador>
        get() {
            if (searchQuery.isBlank()) return paseadores
            val query = searchQuery.trim()
            return paseadores.filter { paseador ->
                val fullName = "${paseador.nombre} ${paseador.apellido}"
                fullName.contains(query, ignoreCase = true) ||
                        paseador.distrito.contains(query, ignoreCase = true)
            }
        }
}
