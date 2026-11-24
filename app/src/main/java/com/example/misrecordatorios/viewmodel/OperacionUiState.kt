
package com.example.misrecordatorios.viewmodel

sealed interface OperacionUiState {

    data object Idle : OperacionUiState

    data object Procesando : OperacionUiState

    data object Actualizado : OperacionUiState

    data object Eliminado : OperacionUiState

    data class Error(
        val mensaje: String
    ) : OperacionUiState
}
 