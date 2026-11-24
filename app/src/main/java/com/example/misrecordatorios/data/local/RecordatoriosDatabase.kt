
package com.example.misrecordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecordatorioEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RecordatoriosDatabase : RoomDatabase() {

    // Acceso al DAO de recordatorios
    abstract fun recordatorioDao(): RecordatorioDao

    companion object {

        // Nombre del archivo SQLite
        private const val NOMBRE_DATABASE =
            "mis_recordatorios.db"

        // Instancia unica de la base de datos
        @Volatile
        private var INSTANCE: RecordatoriosDatabase? = null

        // Obtener o crear la instancia de Room
        fun obtenerInstancia(
            context: Context
        ): RecordatoriosDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(

                    context.applicationContext,

                    RecordatoriosDatabase::class.java,

                    NOMBRE_DATABASE

                ).build().also { database ->

                    INSTANCE = database
                }
            }
        }
    }
}
 