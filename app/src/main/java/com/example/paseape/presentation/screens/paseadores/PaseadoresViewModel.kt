package com.example.paseape.presentation.screens.paseadores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.domain.usecase.paseador.GetPaseadoresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaseadoresViewModel @Inject constructor(
    private val getPaseadoresUseCase: GetPaseadoresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaseadoresUiState())
    val uiState: StateFlow<PaseadoresUiState> = _uiState.asStateFlow()

    init {
        loadPaseadores()
    }

    fun onEvent(event: PaseadoresUiEvent) {
        when (event) {
            is PaseadoresUiEvent.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is PaseadoresUiEvent.RetryLoading -> {
                loadPaseadores()
            }
            is PaseadoresUiEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun loadPaseadores() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = getPaseadoresUseCase()
            result.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        paseadores = list,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "No se pudieron cargar los paseadores"
                    )
                }
            }
        }
    }
}
