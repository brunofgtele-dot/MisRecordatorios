
package com.example.misrecordatorios.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "recordatorios",
    indices = [
        Index(
            value = ["uuid"],
            unique = true
        ),
        Index(value = ["estado"]),
        Index(value = ["categoria"]),
        Index(value = ["estadoSincronizacion"])
    ]
)
data class RecordatorioEntity(

    // Identificador local generado por Room
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Identificador global para sincronizacion
    val uuid: String = UUID.randomUUID().toString(),

    // Datos principales
    val titulo: String,

    val descripcion: String? = null,

    val categoria: String,

    val prioridad: String,

    val estado: String = "Pendiente",

    // Datos opcionales
    val ubicacion: String? = null,

    val fechaAviso: String? = null,

    // Fecha de creacion
    val fechaCreacion: Long = System.currentTimeMillis(),

    // Fecha de ultima modificacion
    val fechaModificacion: Long = System.currentTimeMillis(),

    // Estado de sincronizacion remota
    val estadoSincronizacion: String = "PENDIENTE",

    // Control de cambios concurrentes
    val revision: Long = 1L,

    // Eliminacion logica para sincronizacion offline
    val eliminado: Boolean = false
)
 