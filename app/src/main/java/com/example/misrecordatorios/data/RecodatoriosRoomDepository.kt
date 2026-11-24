package com.example.misrecordatorios.data

import com.example.misrecordatorios.Categoria
import com.example.misrecordatorios.EstadoRecordatorio
import com.example.misrecordatorios.Prioridad
import com.example.misrecordatorios.Recordatorio
import com.example.misrecordatorios.data.local.RecordatorioDao
import com.example.misrecordatorios.data.local.RecordatorioEntity
import com.example.misrecordatorios.data.local.toDomain
import com.example.misrecordatorios.data.local.toDomainList
import com.example.misrecordatorios.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RecordatoriosRoomRepository(

    private val dao: RecordatorioDao

) {

    // =========================================
    // 1. OBSERVAR RECORDATORIOS
    // =========================================

    val recordatorios: Flow<List<Recordatorio>> =

        dao.observarTodos().map { entidades ->

            entidades.toDomainList()
        }

    // =========================================
    // 2. CONSULTAR TODOS LOS RECORDATORIOS
    // =========================================

    suspend fun obtenerRecordatorios():
            List<Recordatorio> {

        return recordatorios.first()
    }

    // =========================================
    // 3. CONSULTAR RECORDATORIO POR ID
    // =========================================

    suspend fun obtenerPorId(
        id: Int
    ): Recordatorio? {

        return dao.obtenerPorId(id)?.toDomain()
    }

    fun observarPorId(
        id: Int
    ): Flow<Recordatorio?> {

        return dao.observarPorId(id).map { entidad ->

            entidad?.toDomain()
        }
    }

    // =========================================
    // 4. CREAR RECORDATORIOS
    // =========================================

    suspend fun guardar(
        recordatorio: Recordatorio
    ): Long {

        val entidad = recordatorio.toEntity()

        return dao.insertar(entidad)
    }

    // =========================================
    // 5. ACTUALIZAR ESTADO
    // =========================================

    suspend fun actualizarEstado(

        id: Int,

        nuevoEstado: EstadoRecordatorio

    ) {

        val filasActualizadas = dao.actualizarEstado(

            id = id,

            nuevoEstado = nuevoEstado.nombre
        )

        check(filasActualizadas > 0) {

            "No se encontro el recordatorio con ID $id"
        }
    }

    // =========================================
    // 6. ACTUALIZAR DATOS DEL RECORDATORIO
    // =========================================

    suspend fun actualizarDatos(

        id: Int,

        titulo: String,

        descripcion: String?,

        categoria: Categoria,

        prioridad: Prioridad

    ) {

        require(titulo.isNotBlank()) {

            "El titulo no puede estar vacio"
        }

        require(titulo.length <= 100) {

            "El titulo no puede superar 100 caracteres"
        }

        val filasActualizadas = dao.actualizarDatos(

            id = id,

            titulo = titulo.trim(),

            descripcion = descripcion
                ?.trim()
                ?.takeIf { it.isNotBlank() },

            categoria = categoria.nombre,

            prioridad = prioridad.nombre
        )

        check(filasActualizadas > 0) {

            "No se encontro el recordatorio con ID $id"
        }
    }

    // =========================================
    // 7. ELIMINAR RECORDATORIOS
    // =========================================

    suspend fun eliminar(
        id: Int
    ) {

        val filasActualizadas = dao.eliminar(id)

        check(filasActualizadas > 0) {

            "No se encontro el recordatorio con ID $id"
        }
    }

    // =========================================
    // 8. FILTRAR POR ESTADO
    // =========================================

    fun observarPorEstado(

        estado: EstadoRecordatorio

    ): Flow<List<Recordatorio>> {

        return dao.observarPorEstado(

            estado.nombre

        ).map { entidades ->

            entidades.toDomainList()
        }
    }

    // =========================================
    // 9. FILTRAR POR CATEGORIA
    // =========================================

    fun observarPorCategoria(

        categoria: Categoria

    ): Flow<List<Recordatorio>> {

        return dao.observarPorCategoria(

            categoria.nombre

        ).map { entidades ->

            entidades.toDomainList()
        }
    }

    // =========================================
    // 10. FILTRAR POR PRIORIDAD
    // =========================================

    fun observarPorPrioridad(

        prioridad: Prioridad

    ): Flow<List<Recordatorio>> {

        return dao.observarPorPrioridad(

            prioridad.nombre

        ).map { entidades ->

            entidades.toDomainList()
        }
    }

    // =========================================
    // 11. CONTROL DE SINCRONIZACION
    // =========================================

    val cantidadPendientes: Flow<Int> =

        dao.observarCantidadPendientes()

    suspend fun obtenerPendientesSincronizacion():
            List<RecordatorioEntity> {

        return dao.obtenerPendientesSincronizacion()
    }

    suspend fun marcarSincronizado(

        uuid: String,

        revision: Long

    ): Boolean {

        return dao.marcarSincronizado(

            uuid = uuid,

            revision = revision

        ) > 0
    }

    suspend fun marcarErrorSincronizacion(

        uuid: String,

        revision: Long

    ): Boolean {

        return dao.marcarErrorSincronizacion(

            uuid = uuid,

            revision = revision

        ) > 0
    }

    // =========================================
    // 12. CONSULTAS DE CONTROL
    // =========================================

    suspend fun contarRecordatorios(): Int {

        return dao.contarRecordatorios()
    }

    suspend fun contarRegistrosTotales(): Int {

        return dao.contarRegistrosTotales()
    }
}
 