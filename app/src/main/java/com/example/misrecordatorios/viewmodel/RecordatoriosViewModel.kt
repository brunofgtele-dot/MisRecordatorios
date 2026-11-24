
package com.example.misrecordatorios.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.misrecordatorios.Categoria
import com.example.misrecordatorios.EstadoRecordatorio
import com.example.misrecordatorios.Prioridad
import com.example.misrecordatorios.Recordatorio
import com.example.misrecordatorios.data.RecordatoriosRepository
import com.example.misrecordatorios.data.local.RecordatoriosDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordatoriosViewModel(
    application: Application
) : AndroidViewModel(application) {

    // =========================================
    // 1. INICIALIZACION DE ROOM
    // =========================================

    private val database =
        RecordatoriosDatabase.obtenerInstancia(application)

    private val repository =
        RecordatoriosRepository(
            database.recordatorioDao()
        )

    // =========================================
    // 2. ESTADOS DE LA INTERFAZ
    // =========================================

    private val _uiState =
        MutableStateFlow<RecordatoriosUiState>(
            RecordatoriosUiState.Loading
        )

    val uiState: StateFlow<RecordatoriosUiState> =
        _uiState.asStateFlow()

    private val _guardadoState =
        MutableStateFlow<GuardadoUiState>(
            GuardadoUiState.Idle
        )

    val guardadoState: StateFlow<GuardadoUiState> =
        _guardadoState.asStateFlow()

    private val _errorOperacion =
        MutableStateFlow<String?>(null)

    val errorOperacion: StateFlow<String?> =
        _errorOperacion.asStateFlow()

    private val _operacionState =
        MutableStateFlow<OperacionUiState>(
            OperacionUiState.Idle
        )

    val operacionState: StateFlow<OperacionUiState> =
        _operacionState.asStateFlow()

    // =========================================
    // 3. CONTROL DE OBSERVACION
    // =========================================

    private var observacionJob: Job? = null

    private var simulacionJob: Job? = null

    private var simulacionActiva = false

    private var ultimosRecordatorios:
            List<Recordatorio> = emptyList()

    // =========================================
    // 4. INICIALIZACION DEL VIEWMODEL
    // =========================================

    init {
        cargarRecordatorios()
    }

    // =========================================
    // 5. OBSERVAR RECORDATORIOS DESDE ROOM
    // =========================================

    fun cargarRecordatorios() {

        simulacionJob?.cancel()

        simulacionActiva = false

        observacionJob?.cancel()

        _uiState.value =
            RecordatoriosUiState.Loading

        observacionJob = viewModelScope.launch {

            try {

                repository.recordatorios.collect { lista ->

                    ultimosRecordatorios = lista

                    if (!simulacionActiva) {

                        publicarRecordatorios(lista)
                    }
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _uiState.value =
                    RecordatoriosUiState.Error(
                        "No se pudieron cargar los recordatorios."
                    )
            }
        }
    }

    private fun publicarRecordatorios(
        lista: List<Recordatorio>
    ) {

        _uiState.value = if (lista.isEmpty()) {

            RecordatoriosUiState.Empty

        } else {

            RecordatoriosUiState.Success(lista)
        }
    }

    // =========================================
    // 6. CREAR RECORDATORIOS
    // =========================================

    fun guardarRecordatorio(

        titulo: String,

        descripcion: String?,

        categoria: Categoria,

        prioridad: Prioridad

    ) {

        if (_guardadoState.value ==
            GuardadoUiState.Saving
        ) {
            return
        }

        val tituloLimpio = titulo.trim()

        if (tituloLimpio.isBlank()) {

            _guardadoState.value =
                GuardadoUiState.Error(
                    "El titulo no puede estar vacio."
                )

            return
        }

        if (tituloLimpio.length > 100) {

            _guardadoState.value =
                GuardadoUiState.Error(
                    "El titulo no puede superar 100 caracteres."
                )

            return
        }

        _guardadoState.value =
            GuardadoUiState.Saving

        viewModelScope.launch {

            try {

                val nuevoRecordatorio = Recordatorio(

                    // Valor provisional requerido
                    // por el modelo de dominio.
                    // El Mapper lo convierte a 0
                    // para que Room genere el ID.

                    id = 1,

                    titulo = tituloLimpio,

                    descripcion = descripcion
                        ?.trim()
                        ?.takeIf { it.isNotBlank() },

                    categoria = categoria,

                    prioridad = prioridad,

                    estado =
                        EstadoRecordatorio.Pendiente
                )

                repository.guardar(nuevoRecordatorio)

                _guardadoState.value =
                    GuardadoUiState.Saved

                if (simulacionActiva ||
                    observacionJob?.isActive != true
                ) {
                    cargarRecordatorios()
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _guardadoState.value =
                    GuardadoUiState.Error(
                        "No se pudo guardar el recordatorio."
                    )
            }
        }
    }

    // =========================================
    // 7. REINICIAR ESTADO DE GUARDADO
    // =========================================

    fun reiniciarGuardado() {

        _guardadoState.value =
            GuardadoUiState.Idle
    }

    // =========================================
    // 8. CONSULTAR RECORDATORIO POR ID
    // =========================================

    fun obtenerRecordatorio(
        id: Int
    ): Recordatorio? {

        return ultimosRecordatorios.find {

            it.id == id
        }
    }

    // =========================================
    // 9. ACTUALIZAR ESTADO
    // =========================================



    fun actualizarEstado(
        id: Int,
        nuevoEstado: EstadoRecordatorio
    ) {

        ejecutarOperacion(
            resultado = OperacionUiState.Actualizado
        ) {
            repository.actualizarEstado(
                id = id,
                nuevoEstado = nuevoEstado
            )
        }
    }

    fun actualizarDatos(
        id: Int,
        titulo: String,
        descripcion: String?,
        categoria: Categoria,
        prioridad: Prioridad
    ) {

        ejecutarOperacion(
            resultado = OperacionUiState.Actualizado
        ) {
            repository.actualizarDatos(
                id = id,
                titulo = titulo,
                descripcion = descripcion,
                categoria = categoria,
                prioridad = prioridad
            )
        }
    }

    fun eliminarRecordatorio(
        id: Int
    ) {

        ejecutarOperacion(
            resultado = OperacionUiState.Eliminado
        ) {
            repository.eliminar(id)
        }
    }

    private fun ejecutarOperacion(
        resultado: OperacionUiState,
        operacion: suspend () -> Unit
    ) {

        if (_operacionState.value ==
            OperacionUiState.Procesando
        ) {
            return
        }

        _operacionState.value =
            OperacionUiState.Procesando

        _errorOperacion.value = null

        viewModelScope.launch {

            try {

                operacion()

                _operacionState.value = resultado

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                val mensaje =
                    if (e is IllegalArgumentException) {
                        e.message ?: "Datos invalidos."
                    } else {
                        "No se pudo completar la operacion."
                    }

                _errorOperacion.value = mensaje

                _operacionState.value =
                    OperacionUiState.Error(mensaje)
            }
        }
    }

    fun reiniciarOperacion() {

        if (_operacionState.value !=
            OperacionUiState.Procesando
        ) {
            _operacionState.value =
                OperacionUiState.Idle

            _errorOperacion.value = null
        }
    }

    fun limpiarErrorOperacion() {

        _errorOperacion.value = null
    }

    // =========================================
    // 13. REINTENTAR CARGA
    // =========================================

    fun reintentar() {

        cargarRecordatorios()
    }

    // =========================================
    // 14. SIMULAR ESTADO LOADING
    // =========================================

    fun simularLoading() {

        simulacionJob?.cancel()

        simulacionActiva = true

        _uiState.value =
            RecordatoriosUiState.Loading

        simulacionJob = viewModelScope.launch {

            delay(1000)

            simulacionActiva = false

            cargarRecordatorios()
        }
    }

    // =========================================
    // 15. SIMULAR ESTADO EMPTY
    // =========================================

    fun vaciarRecordatorios() {

        simulacionJob?.cancel()

        simulacionActiva = true

        // Solo modifica la interfaz.
        // No elimina registros de Room.

        _uiState.value =
            RecordatoriosUiState.Empty
    }

    // =========================================
    // 16. SIMULAR ESTADO ERROR
    // =========================================

    fun provocarError() {

        simulacionJob?.cancel()

        simulacionActiva = true

        _uiState.value =
            RecordatoriosUiState.Error(
                "Error simulado al cargar recordatorios."
            )
    }

    // =========================================
    // 17. RESTAURAR VISUALIZACION REAL
    // =========================================

    fun restaurarRecordatorios() {

        simulacionJob?.cancel()

        simulacionActiva = false

        // Recupera los registros reales de Room.
        // No inserta datos simulados.

        cargarRecordatorios()
    }
}
 