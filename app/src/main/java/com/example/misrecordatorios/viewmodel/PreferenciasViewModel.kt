
package com.example.misrecordatorios.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.misrecordatorios.data.preferences.OrdenRecordatorios
import com.example.misrecordatorios.data.preferences.PreferenciasRepository
import com.example.misrecordatorios.data.preferences.PreferenciasUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PreferenciasViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Repositorio de preferencias persistentes
    private val repository =
        PreferenciasRepository(application)

    // Estado reactivo de las preferencias
    val preferencias: StateFlow<PreferenciasUsuario> =
        repository.preferencias.stateIn(

            scope = viewModelScope,

            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5000
            ),

            initialValue = PreferenciasUsuario()
        )

    // Estado para informar errores a la interfaz
    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()

    // Cambiar el tema de la aplicacion
    fun cambiarTemaOscuro(activado: Boolean) {

        ejecutarCambio {

            repository.cambiarTemaOscuro(activado)
        }
    }

    // Mostrar u ocultar recordatorios completados
    fun cambiarMostrarCompletados(mostrar: Boolean) {

        ejecutarCambio {

            repository.cambiarMostrarCompletados(mostrar)
        }
    }

    // Cambiar criterio de ordenamiento
    fun cambiarOrden(orden: OrdenRecordatorios) {

        ejecutarCambio {

            repository.cambiarOrden(orden)
        }
    }

    // Restaurar valores predeterminados
    fun restaurarPreferencias() {

        ejecutarCambio {

            repository.restaurarPreferencias()
        }
    }

    // Limpiar mensaje de error
    fun limpiarError() {

        _error.value = null
    }

    // Ejecutar operaciones de forma asincrona
    // y controlar posibles errores de escritura
    private fun ejecutarCambio(
        operacion: suspend () -> Unit
    ) {

        viewModelScope.launch {

            try {

                _error.value = null

                operacion()

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _error.value =
                    "No se pudieron guardar las preferencias."
            }
        }
    }
}
 