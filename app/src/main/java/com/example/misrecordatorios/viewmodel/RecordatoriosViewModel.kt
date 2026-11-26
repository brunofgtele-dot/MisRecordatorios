
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
import com.example.misrecordatorios.data.network.MonitorConectividad
import com.example.misrecordatorios.data.sync.ResultadoSincronizacion

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class RecordatoriosViewModel(
    application: Application
) : AndroidViewModel(application) {

    // ============================================
    // 1. BASE DE DATOS ROOM
    // ============================================

    private val database =
        RecordatoriosDatabase.obtenerInstancia(application)

    // ============================================
    // 2. REPOSITORIO LOCAL-FIRST
    // ============================================

    private val repository =
        RecordatoriosRepository(database = database)

    // ============================================
    // 3. MONITOR DE CONECTIVIDAD
    // ============================================

    private val monitorConectividad =
        MonitorConectividad(application)

    val conectado: StateFlow<Boolean> =
        monitorConectividad.conectado

    // ============================================
    // 4. ESTADO DEL LISTADO
    // ============================================

    private val _uiState =
        MutableStateFlow<RecordatoriosUiState>(
            RecordatoriosUiState.Loading
        )

    val uiState: StateFlow<RecordatoriosUiState> =
        _uiState.asStateFlow()

    // ============================================
    // 5. ESTADO DE GUARDADO
    // ============================================

    private val _guardadoState =
        MutableStateFlow<GuardadoUiState>(
            GuardadoUiState.Idle
        )

    val guardadoState: StateFlow<GuardadoUiState> =
        _guardadoState.asStateFlow()

    // ============================================
    // 6. ESTADO DE OPERACIONES CRUD
    // ============================================

    private val _operacionState =
        MutableStateFlow<OperacionUiState>(
            OperacionUiState.Idle
        )

    val operacionState: StateFlow<OperacionUiState> =
        _operacionState.asStateFlow()

    // ============================================
    // 7. ERRORES DE OPERACIONES LOCALES
    // ============================================

    private val _errorOperacion =
        MutableStateFlow<String?>(null)

    val errorOperacion: StateFlow<String?> =
        _errorOperacion.asStateFlow()

    // ============================================
    // 8. CANTIDAD DE REGISTROS PENDIENTES
    // ============================================

    val cantidadPendientes: StateFlow<Int> =
        repository.cantidadPendientes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // ============================================
    // 9. ESTADO DE SINCRONIZACION
    // ============================================

    val sincronizando: StateFlow<Boolean> =
        repository.sincronizando

    // ============================================
    // 10. ULTIMO RESULTADO DE SINCRONIZACION
    // ============================================

    val ultimoResultadoSincronizacion:
            StateFlow<ResultadoSincronizacion?> =
        repository.ultimoResultadoSincronizacion

    // ============================================
    // 11. ERROR DE SINCRONIZACION
    // ============================================

    private val _errorSincronizacion =
        MutableStateFlow<String?>(null)

    val errorSincronizacion: StateFlow<String?> =
        _errorSincronizacion.asStateFlow()

    // ============================================
    // 12. CONTROL DE CORRUTINAS
    // ============================================

    private var observacionJob: Job? = null

    private var simulacionJob: Job? = null

    private var sincronizacionJob: Job? = null

    // ============================================
    // 13. CONTROL DE SIMULACIONES
    // ============================================

    private var simulacionActiva = false

    private var ultimosRecordatorios:
            List<Recordatorio> = emptyList()

    // ============================================
    // 14. INICIALIZACION
    // ============================================

    init {
        monitorConectividad.iniciarMonitoreo()

        cargarRecordatorios()
    }

    // ============================================
    // 15. OBSERVAR RECORDATORIOS DESDE ROOM
    // ============================================

    fun cargarRecordatorios() {

        observacionJob?.cancel()

        simulacionJob?.cancel()

        simulacionActiva = false

        _uiState.value =
            RecordatoriosUiState.Loading

        observacionJob = viewModelScope.launch {

            try {

                repository.recordatorios.collect { recordatorios ->

                    ultimosRecordatorios = recordatorios

                    if (!simulacionActiva) {

                        publicarRecordatorios(recordatorios)
                    }
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _uiState.value =
                    RecordatoriosUiState.Error(
                        mensaje =
                            "No se pudieron cargar " +
                                    "los recordatorios."
                    )
            }
        }
    }

    // ============================================
    // 16. PUBLICAR ESTADO DEL LISTADO
    // ============================================

    private fun publicarRecordatorios(
        recordatorios: List<Recordatorio>
    ) {

        _uiState.value =
            if (recordatorios.isEmpty()) {

                RecordatoriosUiState.Empty

            } else {

                RecordatoriosUiState.Success(
                    recordatorios = recordatorios
                )
            }
    }

    // ============================================
    // 17. GUARDAR RECORDATORIO LOCALMENTE
    // ============================================

    fun guardarRecordatorio(
        titulo: String,
        descripcion: String?,
        categoria: Categoria,
        prioridad: Prioridad
    ) {

        if (
            _guardadoState.value ==
            GuardadoUiState.Saving
        ) {
            return
        }

        val tituloLimpio = titulo.trim()

        if (tituloLimpio.isBlank()) {

            _guardadoState.value =
                GuardadoUiState.Error(
                    mensaje =
                        "El titulo no puede estar vacio."
                )

            return
        }

        if (tituloLimpio.length > 100) {

            _guardadoState.value =
                GuardadoUiState.Error(
                    mensaje =
                        "El titulo no puede superar " +
                                "100 caracteres."
                )

            return
        }

        _guardadoState.value =
            GuardadoUiState.Saving

        _errorOperacion.value = null

        viewModelScope.launch {

            try {

                val recordatorio = Recordatorio(

                    id = 1,

                    titulo = tituloLimpio,

                    descripcion =
                        descripcion
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            },

                    categoria = categoria,

                    prioridad = prioridad,

                    estado =
                        EstadoRecordatorio.Pendiente
                )

                repository.guardar(recordatorio)

                recuperarObservacionSiNecesario()

                _guardadoState.value =
                    GuardadoUiState.Saved

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _guardadoState.value =
                    GuardadoUiState.Error(
                        mensaje =
                            "No se pudo guardar " +
                                    "el recordatorio."
                    )
            }
        }
    }

    // ============================================
    // 18. REINICIAR ESTADO DE GUARDADO
    // ============================================

    fun reiniciarGuardado() {

        if (
            _guardadoState.value !=
            GuardadoUiState.Saving
        ) {

            _guardadoState.value =
                GuardadoUiState.Idle
        }
    }

    // ============================================
    // 19. OBTENER RECORDATORIO POR ID
    // ============================================

    fun obtenerRecordatorio(
        id: Int
    ): Recordatorio? {

        return ultimosRecordatorios.firstOrNull {
                recordatorio -> recordatorio.id == id
        }
    }

    // ============================================
    // 20. ACTUALIZAR ESTADO LOCAL
    // ============================================

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

    // ============================================
    // 21. ACTUALIZAR DATOS LOCALES
    // ============================================

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

    // ============================================
    // 22. ELIMINAR RECORDATORIO LOCALMENTE
    // ============================================

    fun eliminarRecordatorio(
        id: Int
    ) {

        ejecutarOperacion(
            resultado = OperacionUiState.Eliminado
        ) {

            repository.eliminar(id)
        }
    }

    // ============================================
    // 23. EJECUTAR OPERACIONES CRUD
    // ============================================

    private fun ejecutarOperacion(
        resultado: OperacionUiState,
        operacion: suspend () -> Unit
    ) {

        if (
            _operacionState.value ==
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

                recuperarObservacionSiNecesario()

                _operacionState.value = resultado

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                val mensaje =
                    if (e is IllegalArgumentException) {

                        e.message
                            ?: "Los datos son invalidos."

                    } else {

                        "No se pudo completar " +
                                "la operacion."
                    }

                _errorOperacion.value = mensaje

                _operacionState.value =
                    OperacionUiState.Error(
                        mensaje = mensaje
                    )
            }
        }
    }

    // ============================================
    // 24. RECUPERAR OBSERVACION DE ROOM
    // ============================================

    private fun recuperarObservacionSiNecesario() {

        if (
            simulacionActiva ||
            observacionJob?.isActive != true
        ) {

            cargarRecordatorios()
        }
    }

    // ============================================
    // 25. REINICIAR ESTADO DE OPERACION
    // ============================================

    fun reiniciarOperacion() {

        if (
            _operacionState.value !=
            OperacionUiState.Procesando
        ) {

            _operacionState.value =
                OperacionUiState.Idle
        }
    }

    // ============================================
    // 26. LIMPIAR ERROR LOCAL
    // ============================================

    fun limpiarErrorOperacion() {

        _errorOperacion.value = null
    }

    // ============================================
    // 27. REINTENTAR CARGA LOCAL
    // ============================================

    fun reintentar() {

        cargarRecordatorios()
    }

    // ============================================
    // 28. SINCRONIZAR RECORDATORIOS
    // ============================================

    fun sincronizarRecordatorios(
        reintentarErrores: Boolean = true
    ) {

        if (
            sincronizacionJob?.isActive == true ||
            sincronizando.value
        ) {
            return
        }

        _errorSincronizacion.value = null

        sincronizacionJob = viewModelScope.launch {

            try {

                val resultado =
                    repository.sincronizar(
                        reintentarErrores =
                            reintentarErrores
                    )

                when (resultado) {

                    is ResultadoSincronizacion.Exito -> {

                        _errorSincronizacion.value = null
                    }

                    is ResultadoSincronizacion.SinCambios -> {

                        _errorSincronizacion.value = null
                    }

                    is ResultadoSincronizacion.Parcial -> {

                        _errorSincronizacion.value =
                            resultado.mensaje
                    }

                    is ResultadoSincronizacion.Error -> {

                        _errorSincronizacion.value =
                            resultado.mensaje
                    }
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _errorSincronizacion.value =
                    "No se pudo ejecutar " +
                            "la sincronizacion."
            }
        }
    }

    // ============================================
    // 29. REINTENTAR SINCRONIZACION
    // ============================================

    fun reintentarSincronizacion() {

        sincronizarRecordatorios(
            reintentarErrores = true
        )
    }

    // ============================================
    // 30. CANCELAR SINCRONIZACION
    // ============================================

    fun cancelarSincronizacion() {

        sincronizacionJob?.cancel()
    }

    // ============================================
    // 31. LIMPIAR ERROR DE SINCRONIZACION
    // ============================================

    fun limpiarErrorSincronizacion() {

        _errorSincronizacion.value = null
    }

    // ============================================
    // 32. LIMPIAR ULTIMO RESULTADO
    // ============================================

    fun limpiarUltimoResultadoSincronizacion() {

        repository
            .limpiarUltimoResultadoSincronizacion()

        _errorSincronizacion.value = null
    }

    // ============================================
    // 33. SIMULAR ESTADO LOADING
    // ============================================

    fun simularLoading() {

        simulacionJob?.cancel()

        simulacionActiva = true

        _uiState.value =
            RecordatoriosUiState.Loading

        simulacionJob = viewModelScope.launch {

            delay(1000)

            cargarRecordatorios()
        }
    }

    // ============================================
    // 34. SIMULAR ESTADO EMPTY
    // ============================================

    fun vaciarRecordatorios() {

        simulacionJob?.cancel()

        simulacionActiva = true

        _uiState.value =
            RecordatoriosUiState.Empty
    }

    // ============================================
    // 35. SIMULAR ESTADO ERROR
    // ============================================

    fun provocarError() {

        simulacionJob?.cancel()

        simulacionActiva = true

        _uiState.value =
            RecordatoriosUiState.Error(
                mensaje =
                    "Error simulado al cargar " +
                            "los recordatorios."
            )
    }

    // ============================================
    // 36. RESTAURAR OBSERVACION REAL
    // ============================================

    fun restaurarRecordatorios() {

        cargarRecordatorios()
    }

    // ============================================
    // 37. LIBERAR RECURSOS DEL VIEWMODEL
    // ============================================

    override fun onCleared() {

        monitorConectividad.detenerMonitoreo()

        super.onCleared()
    }
}
 