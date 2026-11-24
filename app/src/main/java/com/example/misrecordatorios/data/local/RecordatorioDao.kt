
package com.example.misrecordatorios.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioDao {

    // =========================================
    // 1. CREAR RECORDATORIOS
    // =========================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(
        recordatorio: RecordatorioEntity
    ): Long

    // =========================================
    // 2. CONSULTAR RECORDATORIOS
    // =========================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE eliminado = 0
        ORDER BY fechaCreacion ASC, id ASC
    """)
    fun observarTodos(): Flow<List<RecordatorioEntity>>

    @Query("""
        SELECT * FROM recordatorios
        WHERE id = :id
        AND eliminado = 0
        LIMIT 1
    """)
    fun observarPorId(
        id: Int
    ): Flow<RecordatorioEntity?>

    @Query("""
        SELECT * FROM recordatorios
        WHERE id = :id
        AND eliminado = 0
        LIMIT 1
    """)
    suspend fun obtenerPorId(
        id: Int
    ): RecordatorioEntity?

    @Query("""
        SELECT * FROM recordatorios
        WHERE uuid = :uuid
        LIMIT 1
    """)
    suspend fun obtenerPorUuid(
        uuid: String
    ): RecordatorioEntity?

    // =========================================
    // 3. FILTRAR RECORDATORIOS
    // =========================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE estado = :estado
        AND eliminado = 0
        ORDER BY fechaCreacion ASC, id ASC
    """)
    fun observarPorEstado(
        estado: String
    ): Flow<List<RecordatorioEntity>>

    @Query("""
        SELECT * FROM recordatorios
        WHERE categoria = :categoria
        AND eliminado = 0
        ORDER BY fechaCreacion ASC, id ASC
    """)
    fun observarPorCategoria(
        categoria: String
    ): Flow<List<RecordatorioEntity>>

    @Query("""
        SELECT * FROM recordatorios
        WHERE prioridad = :prioridad
        AND eliminado = 0
        ORDER BY fechaCreacion ASC, id ASC
    """)
    fun observarPorPrioridad(
        prioridad: String
    ): Flow<List<RecordatorioEntity>>

    // =========================================
    // 4. ACTUALIZAR RECORDATORIOS
    // =========================================

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun actualizar(
        recordatorio: RecordatorioEntity
    ): Int

    @Query("""
        UPDATE recordatorios
        SET estado = :nuevoEstado,
            fechaModificacion = :fecha,
            estadoSincronizacion = 'PENDIENTE',
            revision = revision + 1
        WHERE id = :id
        AND eliminado = 0
    """)
    suspend fun actualizarEstado(
        id: Int,
        nuevoEstado: String,
        fecha: Long = System.currentTimeMillis()
    ): Int

    @Query("""
        UPDATE recordatorios
        SET titulo = :titulo,
            descripcion = :descripcion,
            categoria = :categoria,
            prioridad = :prioridad,
            fechaModificacion = :fecha,
            estadoSincronizacion = 'PENDIENTE',
            revision = revision + 1
        WHERE id = :id
        AND eliminado = 0
    """)
    suspend fun actualizarDatos(
        id: Int,
        titulo: String,
        descripcion: String?,
        categoria: String,
        prioridad: String,
        fecha: Long = System.currentTimeMillis()
    ): Int

    // =========================================
    // 5. ELIMINAR RECORDATORIOS
    // =========================================

    // Eliminacion logica para permitir
    // sincronizacion posterior sin conexion.

    @Query("""
        UPDATE recordatorios
        SET eliminado = 1,
            fechaModificacion = :fecha,
            estadoSincronizacion = 'PENDIENTE',
            revision = revision + 1
        WHERE id = :id
        AND eliminado = 0
    """)
    suspend fun eliminar(
        id: Int,
        fecha: Long = System.currentTimeMillis()
    ): Int

    // Eliminacion fisica solo despues de
    // confirmar la sincronizacion remota.

    @Query("""
        DELETE FROM recordatorios
        WHERE uuid = :uuid
        AND revision = :revision
        AND eliminado = 1
        AND estadoSincronizacion = 'SINCRONIZADO'
    """)
    suspend fun eliminarDefinitivamente(
        uuid: String,
        revision: Long
    ): Int

    // =========================================
    // 6. SINCRONIZACION REMOTA
    // =========================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE estadoSincronizacion IN (
            'PENDIENTE',
            'ERROR'
        )
        ORDER BY fechaModificacion ASC, id ASC
    """)
    suspend fun obtenerPendientesSincronizacion():
            List<RecordatorioEntity>

    @Query("""
        SELECT COUNT(*) FROM recordatorios
        WHERE estadoSincronizacion IN (
            'PENDIENTE',
            'ERROR'
        )
    """)
    fun observarCantidadPendientes(): Flow<Int>

    @Query("""
        UPDATE recordatorios
        SET estadoSincronizacion = 'SINCRONIZADO'
        WHERE uuid = :uuid
        AND revision = :revision
    """)
    suspend fun marcarSincronizado(
        uuid: String,
        revision: Long
    ): Int

    @Query("""
        UPDATE recordatorios
        SET estadoSincronizacion = 'ERROR'
        WHERE uuid = :uuid
        AND revision = :revision
        AND estadoSincronizacion != 'SINCRONIZADO'
    """)
    suspend fun marcarErrorSincronizacion(
        uuid: String,
        revision: Long
    ): Int

    // =========================================
    // 7. CONSULTAS DE CONTROL
    // =========================================

    @Query("""
        SELECT COUNT(*) FROM recordatorios
        WHERE eliminado = 0
    """)
    suspend fun contarRecordatorios(): Int

    @Query("""
        SELECT COUNT(*) FROM recordatorios
    """)
    suspend fun contarRegistrosTotales(): Int
}
 