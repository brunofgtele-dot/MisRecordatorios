
package com.example.misrecordatorios.data

import com.example.misrecordatorios.Categoria
import com.example.misrecordatorios.EstadoRecordatorio
import com.example.misrecordatorios.Prioridad
import com.example.misrecordatorios.Recordatorio

import com.example.misrecordatorios.data.local.RecordatorioDao
import com.example.misrecordatorios.data.local.RecordatorioEntity
import com.example.misrecordatorios.data.local.RecordatoriosDatabase
import com.example.misrecordatorios.data.local.toDomain
import com.example.misrecordatorios.data.local.toDomainList
import com.example.misrecordatorios.data.local.toEntity

import com.example.misrecordatorios.data.sync.RecordatoriosSyncManager
import com.example.misrecordatorios.data.sync.ResultadoSincronizacion

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class RecordatoriosRepository(

    private val dao: RecordatorioDao,

    private val syncManager: RecordatoriosSyncManager? = null

) {

    // ============================================
    // 1. CONSTRUCTOR CON BASE DE DATOS ROOM
    // ============================================

    constructor(

        database: RecordatoriosDatabase

    ) : this(

        dao = database.recordatorioDao(),

        syncManager = RecordatoriosSyncManager(
            database = database
        )
    )


    // ============================================
    // 2. CONTROL DE SINCRONIZACION
    // ============================================

    private val mutexRepositorio =
        Mutex()


    private val _sincronizando =
        MutableStateFlow(false)


    val sincronizando: StateFlow<Boolean> =
        _sincronizando.asStateFlow()


    private val _ultimoResultadoSincronizacion =
        MutableStateFlow<ResultadoSincronizacion?>(
            null
        )


    val ultimoResultadoSincronizacion:
            StateFlow<ResultadoSincronizacion?> =
        _ultimoResultadoSincronizacion.asStateFlow()


    // ============================================
    // 3. OBSERVAR RECORDATORIOS LOCALES
    // ============================================

    val recordatorios: Flow<List<Recordatorio>> =

        dao.observarTodos().map {

                entidades ->

            entidades.toDomainList()
        }


    // ============================================
    // 4. OBTENER TODOS LOS RECORDATORIOS
    // ============================================

    suspend fun obtenerRecordatorios():
            List<Recordatorio> {

        return recordatorios.first()
    }


    // ============================================
    // 5. OBTENER RECORDATORIO POR ID
    // ============================================

    suspend fun obtenerPorId(

        id: Int

    ): Recordatorio? {

        return dao.obtenerPorId(id)
            ?.toDomain()
    }


    // ============================================
    // 6. OBSERVAR RECORDATORIO POR ID
    // ============================================

    fun observarPorId(

        id: Int

    ): Flow<Recordatorio?> {

        return dao.observarPorId(id).map {

                entidad ->

            entidad?.toDomain()
        }
    }


    // ============================================
    // 7. GUARDAR RECORDATORIO LOCAL
    // ============================================

    suspend fun guardar(

        recordatorio: Recordatorio

    ): Long {

        val entidad =
            recordatorio.toEntity()

        return dao.insertar(entidad)
    }


    // ============================================
    // 8. ACTUALIZAR ESTADO DEL RECORDATORIO
    // ============================================

    suspend fun actualizarEstado(

        id: Int,

        nuevoEstado: EstadoRecordatorio

    ) {

        val filasActualizadas =
            dao.actualizarEstado(

                id = id,

                nuevoEstado =
                    nuevoEstado.nombre
            )

        check(filasActualizadas > 0) {

            "No se encontro el recordatorio con ID $id"
        }
    }


    // ============================================
    // 9. ACTUALIZAR DATOS DEL RECORDATORIO
    // ============================================

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


        val filasActualizadas =
            dao.actualizarDatos(

                id = id,

                titulo =
                    titulo.trim(),

                descripcion =
                    descripcion
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        },

                categoria =
                    categoria.nombre,

                prioridad =
                    prioridad.nombre
            )


        check(filasActualizadas > 0) {

            "No se encontro el recordatorio con ID $id"
        }
    }


    // ============================================
    // 10. ELIMINAR RECORDATORIO LOGICAMENTE
    // ============================================

    suspend fun eliminar(

        id: Int

    ) {

        val filasActualizadas =
            dao.eliminar(id)


        check(filasActualizadas > 0) {

            "No se encontro el recordatorio con ID $id"
        }
    }


    // ============================================
    // 11. FILTRAR POR ESTADO
    // ============================================

    fun observarPorEstado(

        estado: EstadoRecordatorio

    ): Flow<List<Recordatorio>> {

        return dao.observarPorEstado(
            estado.nombre
        ).map {

                entidades ->

            entidades.toDomainList()
        }
    }


    // ============================================
    // 12. FILTRAR POR CATEGORIA
    // ============================================

    fun observarPorCategoria(

        categoria: Categoria

    ): Flow<List<Recordatorio>> {

        return dao.observarPorCategoria(
            categoria.nombre
        ).map {

                entidades ->

            entidades.toDomainList()
        }
    }


    // ============================================
    // 13. FILTRAR POR PRIORIDAD
    // ============================================

    fun observarPorPrioridad(

        prioridad: Prioridad

    ): Flow<List<Recordatorio>> {

        return dao.observarPorPrioridad(
            prioridad.nombre
        ).map {

                entidades ->

            entidades.toDomainList()
        }
    }


    // ============================================
    // 14. OBSERVAR CANTIDAD DE PENDIENTES
    // ============================================

    val cantidadPendientes: Flow<Int> =

        dao.observarCantidadPendientes()


    // ============================================
    // 15. OBTENER PENDIENTES DE SINCRONIZACION
    // ============================================

    suspend fun obtenerPendientesSincronizacion():
            List<RecordatorioEntity> {

        return dao.obtenerPendientesSincronizacion()
    }


    // ============================================
    // 16. MARCAR RECORDATORIO SINCRONIZADO
    // ============================================

    suspend fun marcarSincronizado(

        uuid: String,

        revision: Long

    ): Boolean {

        return dao.marcarSincronizado(

            uuid = uuid,

            revision = revision

        ) > 0
    }


    // ============================================
    // 17. MARCAR ERROR DE SINCRONIZACION
    // ============================================

    suspend fun marcarErrorSincronizacion(

        uuid: String,

        revision: Long

    ): Boolean {

        return dao.marcarErrorSincronizacion(

            uuid = uuid,

            revision = revision

        ) > 0
    }


    // ============================================
    // 18. CONTAR RECORDATORIOS VISIBLES
    // ============================================

    suspend fun contarRecordatorios(): Int {

        return dao.contarRecordatorios()
    }


    // ============================================
    // 19. CONTAR REGISTROS TOTALES
    // ============================================

    suspend fun contarRegistrosTotales(): Int {

        return dao.contarRegistrosTotales()
    }


    // ============================================
    // 20. VERIFICAR DISPONIBILIDAD DEL SYNC MANAGER
    // ============================================

    fun tieneSincronizadorConfigurado(): Boolean {

        return syncManager != null
    }


    // ============================================
    // 21. EJECUTAR SINCRONIZACION LOCAL-FIRST
    // ============================================

    suspend fun sincronizar(

        reintentarErrores: Boolean = true

    ): ResultadoSincronizacion {

        val manager = checkNotNull(syncManager) {

            "El sincronizador no esta configurado " +
                    "en el repositorio."
        }


        return mutexRepositorio.withLock {

            _sincronizando.value = true

            try {

                val resultado =
                    manager.sincronizar(

                        reintentarErrores =
                            reintentarErrores
                    )


                _ultimoResultadoSincronizacion.value =
                    resultado


                resultado

            } finally {

                _sincronizando.value = false
            }
        }
    }


    // ============================================
    // 22. LIMPIAR ULTIMO RESULTADO
    // ============================================

    fun limpiarUltimoResultadoSincronizacion() {

        _ultimoResultadoSincronizacion.value =
            null
    }
}
 