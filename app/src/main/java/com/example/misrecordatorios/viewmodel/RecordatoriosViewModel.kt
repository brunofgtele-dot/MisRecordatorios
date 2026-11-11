
package com.example.misrecordatorios.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.misrecordatorios.Categoria
import com.example.misrecordatorios.EstadoRecordatorio
import com.example.misrecordatorios.Prioridad
import com.example.misrecordatorios.Recordatorio
import com.example.misrecordatorios.data.RecordatoriosRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordatoriosViewModel(
    private val repository: RecordatoriosRepository =
        RecordatoriosRepository()
) : ViewModel() {

    // Estado de la pantalla de listado
    private val _uiState =
        MutableStateFlow<RecordatoriosUiState>(
            RecordatoriosUiState.Loading
        )

    val uiState: StateFlow<RecordatoriosUiState> =
        _uiState.asStateFlow()

    // Estado del formulario de creación
    private val _guardadoState =
        MutableStateFlow<GuardadoUiState>(
            GuardadoUiState.Idle
        )

    val guardadoState: StateFlow<GuardadoUiState> =
        _guardadoState.asStateFlow()

    // Permite simular un error para las pruebas
    private var simularError = false

    init {
        // Observa cambios en los datos del repositorio
        viewModelScope.launch {
            repository.recordatorios.collect { lista ->
                if (!simularError &&
                    _uiState.value !is RecordatoriosUiState.Loading &&
                    _uiState.value !is RecordatoriosUiState.Error
                ) {
                    actualizarEstadoLista(lista)
                }
            }
        }

        cargarRecordatorios()
    }

    private fun actualizarEstadoLista(
        lista: List<Recordatorio>
    ) {
        _uiState.value = if (lista.isEmpty()) {
            RecordatoriosUiState.Empty
        } else {
            RecordatoriosUiState.Success(lista)
        }
    }

    // Cargar recordatorios desde el repositorio
    fun cargarRecordatorios() {
        viewModelScope.launch {
            _uiState.value = RecordatoriosUiState.Loading

            try {
                val lista = repository.obtenerRecordatorios()

                if (simularError) {
                    simularError = false
                    throw IllegalStateException(
                        "Error simulado al cargar recordatorios"
                    )
                }

                actualizarEstadoLista(lista)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = RecordatoriosUiState.Error(
                    e.message ?: "Error desconocido"
                )
            }
        }
    }

    // Simular error de carga para la evidencia
    fun provocarError() {
        simularError = true
        cargarRecordatorios()
    }

    // Reintentar después de un error
    fun reintentar() {
        simularError = false
        cargarRecordatorios()
    }

    // Crear y guardar un nuevo recordatorio
    fun guardarRecordatorio(
        titulo: String,
        descripcion: String,
        categoria: Categoria,
        prioridad: Prioridad
    ) {
        // Evitar dos operaciones de guardado simultáneas
        if (_guardadoState.value is GuardadoUiState.Saving) {
            return
        }
        if (titulo.isBlank()) {
            _guardadoState.value = GuardadoUiState.Error(
                "El título es obligatorio"
            )
            return
        }

        if (titulo.length > 100) {
            _guardadoState.value = GuardadoUiState.Error(
                "El título no puede superar 100 caracteres"
            )
            return
        }

        viewModelScope.launch {
            _guardadoState.value = GuardadoUiState.Saving

            try {
                val nuevo = Recordatorio(
                    id = repository.siguienteId(),
                    titulo = titulo.trim(),
                    descripcion = descripcion
                        .trim()
                        .ifBlank { null },
                    categoria = categoria,
                    prioridad = prioridad,
                    estado = EstadoRecordatorio.Pendiente
                )

                repository.guardar(nuevo)

                _guardadoState.value = GuardadoUiState.Saved

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _guardadoState.value = GuardadoUiState.Error(
                    e.message ?: "No se pudo guardar"
                )
            }
        }
    }

    // Restablecer el formulario
    fun reiniciarGuardado() {
        _guardadoState.value = GuardadoUiState.Idle
    }

    // Obtener un recordatorio para su detalle
    fun obtenerRecordatorio(id: Int): Recordatorio? {
        return repository.recordatorios.value
            .find { it.id == id }
    }

    // Actualizar el estado de un recordatorio
    fun actualizarEstado(
        id: Int,
        nuevoEstado: EstadoRecordatorio
    ) {
        viewModelScope.launch {
            repository.actualizarEstado(id, nuevoEstado)
        }
    }

    // Simular estado vacío
    fun vaciarRecordatorios() {
        viewModelScope.launch {
            repository.vaciar()
            actualizarEstadoLista(repository.recordatorios.value)
        }
    }

    // Restaurar los datos iniciales
    fun restaurarRecordatorios() {
        viewModelScope.launch {
            repository.restaurar()
            actualizarEstadoLista(repository.recordatorios.value)
        }
    }
}
 