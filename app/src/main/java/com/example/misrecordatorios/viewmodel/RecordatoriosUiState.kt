
package com.example.misrecordatorios.viewmodel

import com.example.misrecordatorios.Recordatorio

// Estados posibles de la pantalla de listado
sealed interface RecordatoriosUiState {

    // Cargando información
    data object Loading : RecordatoriosUiState

    // Lista obtenida correctamente
    data class Success(
        val recordatorios: List<Recordatorio>
    ) : RecordatoriosUiState

    // No existen recordatorios
    data object Empty : RecordatoriosUiState

    // Ocurrió un error al consultar
    data class Error(
        val mensaje: String
    ) : RecordatoriosUiState
}

// Estados del proceso de guardado
sealed interface GuardadoUiState {

    data object Idle : GuardadoUiState

    data object Saving : GuardadoUiState

    data object Saved : GuardadoUiState

    data class Error(
        val mensaje: String
    ) : GuardadoUiState
}
 