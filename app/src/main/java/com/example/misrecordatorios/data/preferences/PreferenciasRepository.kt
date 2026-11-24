
package com.example.misrecordatorios.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Instancia única de DataStore
private val Context.dataStore by preferencesDataStore(
    name = "preferencias_usuario"
)

class PreferenciasRepository(context: Context) {

    private val dataStore =
        context.applicationContext.dataStore

    // Claves utilizadas para almacenar las preferencias
    private object Claves {

        val TEMA_OSCURO =
            booleanPreferencesKey("tema_oscuro")

        val MOSTRAR_COMPLETADOS =
            booleanPreferencesKey("mostrar_completados")

        val ORDEN =
            stringPreferencesKey("orden_recordatorios")
    }

    // Lectura reactiva de las preferencias
    val preferencias: Flow<PreferenciasUsuario> =
        dataStore.data

            .catch { exception ->

                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }

            .map { datos ->

                val ordenGuardado =
                    datos[Claves.ORDEN]

                val orden = OrdenRecordatorios.entries
                    .firstOrNull {
                        it.name == ordenGuardado
                    }
                    ?: OrdenRecordatorios.CREACION

                PreferenciasUsuario(

                    temaOscuro =
                        datos[Claves.TEMA_OSCURO]
                            ?: false,

                    mostrarCompletados =
                        datos[Claves.MOSTRAR_COMPLETADOS]
                            ?: true,

                    orden = orden
                )
            }

    // Guardar preferencia de tema
    suspend fun cambiarTemaOscuro(
        activado: Boolean
    ) {
        dataStore.edit { datos ->

            datos[Claves.TEMA_OSCURO] = activado
        }
    }

    // Guardar preferencia de visualización
    suspend fun cambiarMostrarCompletados(
        mostrar: Boolean
    ) {
        dataStore.edit { datos ->

            datos[Claves.MOSTRAR_COMPLETADOS] = mostrar
        }
    }

    // Guardar criterio de ordenamiento
    suspend fun cambiarOrden(
        orden: OrdenRecordatorios
    ) {
        dataStore.edit { datos ->

            datos[Claves.ORDEN] = orden.name
        }
    }

    // Restaurar las preferencias originales
    suspend fun restaurarPreferencias() {

        dataStore.edit { datos ->

            datos.clear()
        }
    }
}
 