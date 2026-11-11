
package com.example.misrecordatorios.data

import com.example.misrecordatorios.Recordatorio
import com.example.misrecordatorios.DatosSimulados
import com.example.misrecordatorios.EstadoRecordatorio
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RecordatoriosRepository {

    // Lista observable de recordatorios en memoria
    private val _recordatorios = MutableStateFlow(
        DatosSimulados.recordatorios.toList()
    )

    val recordatorios: StateFlow<List<Recordatorio>> =
        _recordatorios.asStateFlow()

    // Consultar todos los recordatorios
    suspend fun obtenerRecordatorios(): List<Recordatorio> {
        delay(500)
        return _recordatorios.value
    }

    // Consultar un recordatorio por su ID
    suspend fun obtenerPorId(id: Int): Recordatorio? {
        delay(200)
        return _recordatorios.value.find { it.id == id }
    }

    // Guardar un nuevo recordatorio
    suspend fun guardar(recordatorio: Recordatorio) {
        delay(500)

        require(_recordatorios.value.none {
            it.id == recordatorio.id
        }) {
            "Ya existe un recordatorio con ese ID"
        }

        _recordatorios.update { lista ->
            lista + recordatorio
        }
    }

    // Generar el siguiente ID disponible
    fun siguienteId(): Int {
        return (_recordatorios.value.maxOfOrNull {
            it.id
        } ?: 0) + 1
    }

    // Actualizar el estado de un recordatorio
    suspend fun actualizarEstado(
        id: Int,
        nuevoEstado: EstadoRecordatorio
    ) {
        delay(300)

        _recordatorios.update { lista ->
            lista.map { recordatorio ->
                if (recordatorio.id == id) {
                    recordatorio.copy(
                        estado = nuevoEstado
                    )
                } else {
                    recordatorio
                }
            }
        }
    }

    // Eliminar un recordatorio
    suspend fun eliminar(id: Int) {
        delay(300)

        _recordatorios.update { lista ->
            lista.filterNot { it.id == id }
        }
    }

    // Vaciar los datos para probar el estado vacío
    suspend fun vaciar() {
        _recordatorios.value = emptyList()
    }

    // Restaurar los datos simulados
    suspend fun restaurar() {
        _recordatorios.value =
            DatosSimulados.recordatorios.toList()
    }
}
 