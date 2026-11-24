
package com.example.misrecordatorios.data.sync

import androidx.room.withTransaction
import com.example.misrecordatorios.data.local.RecordatorioEntity
import com.example.misrecordatorios.data.local.RecordatoriosDatabase
import com.example.misrecordatorios.data.remote.RecordatoriosRemoteDataSource
import com.example.misrecordatorios.data.remote.ResultadoRemoto
import com.example.misrecordatorios.data.remote.obtenerCodigoHttp
import com.example.misrecordatorios.data.remote.toDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class RecordatoriosSyncManager(

    private val database: RecordatoriosDatabase,

    private val remoteDataSource: RecordatoriosRemoteDataSource =
        RecordatoriosRemoteDataSource()

) {

    // ============================================
    // 1. ACCESO A ROOM
    // ============================================

    private val dao =
        database.recordatorioDao()


    // ============================================
    // 2. MUTEX COMPARTIDO
    // ============================================

    companion object {

        private val mutexSincronizacion =
            Mutex()
    }


    // ============================================
    // 3. CONTADORES INTERNOS
    // ============================================

    private class Contadores {

        var pendientesIniciales = 0

        var procesados = 0

        var sincronizados = 0

        var modificadosDuranteSincronizacion = 0

        var remotosInsertados = 0

        var remotosActualizados = 0

        var remotosOmitidos = 0

        val errores =
            mutableListOf<ErrorSincronizacion>()


        fun generarResumen(
            inicio: Long
        ): ResumenSincronizacion {

            val duracionMillis =
                ((System.nanoTime() - inicio) /
                        1_000_000L).coerceAtLeast(0L)

            return ResumenSincronizacion(

                pendientesIniciales =
                    pendientesIniciales,

                procesados =
                    procesados,

                sincronizados =
                    sincronizados,

                modificadosDuranteSincronizacion =
                    modificadosDuranteSincronizacion,

                remotosInsertados =
                    remotosInsertados,

                remotosActualizados =
                    remotosActualizados,

                remotosOmitidos =
                    remotosOmitidos,

                errores =
                    errores.toList(),

                duracionMillis =
                    duracionMillis
            )
        }
    }


    // ============================================
    // 4. RESULTADO DE INTEGRACION REMOTA
    // ============================================

    private data class ResultadoIntegracion(

        val insertados: Int,

        val actualizados: Int,

        val omitidos: Int
    )


    // ============================================
    // 5. PUNTO DE ENTRADA DE SINCRONIZACION
    // ============================================

    suspend fun sincronizar(

        reintentarErrores: Boolean = true

    ): ResultadoSincronizacion {

        return mutexSincronizacion.withLock {

            ejecutarSincronizacion(
                reintentarErrores
            )
        }
    }


    // ============================================
    // 6. EJECUTAR SINCRONIZACION
    // ============================================

    private suspend fun ejecutarSincronizacion(

        reintentarErrores: Boolean

    ): ResultadoSincronizacion {

        val inicio =
            System.nanoTime()

        val contadores =
            Contadores()

        try {

            // ====================================
            // 6.1 CONSULTAR PENDIENTES LOCALES
            // ====================================

            val pendientes =
                dao.obtenerPendientesSincronizacion()

            contadores.pendientesIniciales =
                pendientes.size


            // ====================================
            // 6.2 ENVIAR CAMBIOS LOCALES
            // ====================================

            for (recordatorio in pendientes) {

                if (
                    !reintentarErrores &&
                    recordatorio.estadoSincronizacion ==
                    EstadoSincronizacion.ERROR.valor
                ) {

                    continue
                }

                val errorGeneral =
                    sincronizarRecordatorio(
                        recordatorio,
                        contadores
                    )

                if (errorGeneral != null) {

                    return ResultadoSincronizacion.Error(

                        resumen =
                            contadores.generarResumen(inicio),

                        mensaje =
                            errorGeneral.mensaje,

                        reintentable =
                            errorGeneral.reintentable,

                        codigoHttp =
                            errorGeneral.obtenerCodigoHttp()
                    )
                }
            }


            // ====================================
            // 6.3 DESCARGAR REGISTROS REMOTOS
            // ====================================

            when (
                val resultadoRemoto =
                    remoteDataSource.obtenerRecordatorios()
            ) {

                is ResultadoRemoto.Exito -> {

                    val integracion =
                        integrarRegistrosRemotos(
                            resultadoRemoto.datos
                        )

                    contadores.remotosInsertados =
                        integracion.insertados

                    contadores.remotosActualizados =
                        integracion.actualizados

                    contadores.remotosOmitidos =
                        integracion.omitidos
                }

                is ResultadoRemoto.Error -> {

                    return ResultadoSincronizacion.Error(

                        resumen =
                            contadores.generarResumen(inicio),

                        mensaje =
                            resultadoRemoto.mensaje,

                        reintentable =
                            resultadoRemoto.reintentable,

                        codigoHttp =
                            resultadoRemoto.obtenerCodigoHttp()
                    )
                }
            }


            // ====================================
            // 6.4 GENERAR RESUMEN FINAL
            // ====================================

            val resumen =
                contadores.generarResumen(inicio)


            // ====================================
            // 6.5 CLASIFICAR RESULTADO GLOBAL
            // ====================================

            return when {

                resumen.tieneEnviosIncompletos() -> {

                    ResultadoSincronizacion.Parcial(

                        resumen = resumen,

                        mensaje =
                            "Existen registros pendientes " +
                                    "de sincronizacion."
                    )
                }

                resumen.pendientesIniciales == 0 &&
                        resumen.cambiosRemotosAplicados == 0 -> {

                    ResultadoSincronizacion.SinCambios(
                        resumen = resumen
                    )
                }

                else -> {

                    ResultadoSincronizacion.Exito(
                        resumen = resumen
                    )
                }
            }


        } catch (e: CancellationException) {

            // La cancelacion de corrutinas
            // debe propagarse correctamente.

            throw e


        } catch (e: Exception) {

            // Error general de almacenamiento
            // o procesamiento inesperado.

            return ResultadoSincronizacion.Error(

                resumen =
                    contadores.generarResumen(inicio),

                mensaje =
                    "No se pudo completar la " +
                            "sincronizacion local.",

                reintentable = false
            )
        }
    }


    // ============================================
    // 7. SINCRONIZAR UN RECORDATORIO
    // ============================================

    private suspend fun sincronizarRecordatorio(

        recordatorio: RecordatorioEntity,

        contadores: Contadores

    ): ResultadoRemoto.Error? {

        // Las eliminaciones se transmiten como
        // registros eliminados logicamente.

        val resultado =

            if (recordatorio.eliminado) {

                remoteDataSource.eliminarRecordatorio(
                    recordatorio
                )

            } else {

                remoteDataSource.guardarRecordatorio(
                    recordatorio
                )
            }


        // ========================================
        // 7.1 PROCESAR RESPUESTA REMOTA
        // ========================================

        when (resultado) {

            is ResultadoRemoto.Exito -> {

                val confirmado =
                    resultado.datos


                // Validacion adicional:
                // la respuesta debe corresponder
                // exactamente al registro enviado.

                if (
                    confirmado.toDto() !=
                    recordatorio.toDto()
                ) {

                    registrarErrorIndividual(

                        recordatorio = recordatorio,

                        error =
                            ResultadoRemoto.Error.DatosInvalidos(
                                mensaje =
                                    "La respuesta no coincide " +
                                            "con el registro enviado."
                            ),

                        contadores = contadores
                    )

                    return null
                }


                // Confirmar solamente si la revision
                // local sigue siendo la enviada.

                val filasActualizadas =
                    dao.marcarSincronizado(

                        uuid =
                            recordatorio.uuid,

                        revision =
                            recordatorio.revision
                    )

                if (filasActualizadas > 0) {

                    contadores.sincronizados++

                } else {

                    contadores
                        .modificadosDuranteSincronizacion++
                }

                contadores.procesados++
            }


            is ResultadoRemoto.Error -> {

                registrarErrorIndividual(

                    recordatorio = recordatorio,

                    error = resultado,

                    contadores = contadores
                )


                // Interrumpir la ejecucion ante
                // problemas generales de comunicacion.

                if (
                    resultado is
                            ResultadoRemoto.Error.ErrorConexion ||

                    resultado is
                            ResultadoRemoto.Error.Timeout ||

                    resultado is
                            ResultadoRemoto.Error.ErrorServidor ||

                    (
                            resultado is
                                    ResultadoRemoto.Error.ErrorCliente &&

                                    (
                                            resultado.codigoHttp == 408 ||
                                                    resultado.codigoHttp == 429
                                            )
                            )
                ) {

                    return resultado
                }
            }
        }

        return null
    }


    // ============================================
    // 8. REGISTRAR ERROR INDIVIDUAL
    // ============================================

    private suspend fun registrarErrorIndividual(

        recordatorio: RecordatorioEntity,

        error: ResultadoRemoto.Error,

        contadores: Contadores

    ) {

        val filasActualizadas =
            dao.marcarErrorSincronizacion(

                uuid =
                    recordatorio.uuid,

                revision =
                    recordatorio.revision
            )


        if (filasActualizadas > 0) {

            contadores.errores.add(

                ErrorSincronizacion.desdeErrorRemoto(

                    uuid =
                        recordatorio.uuid,

                    revision =
                        recordatorio.revision,

                    error =
                        error
                )
            )

        } else {

            // La revision cambio durante el envio.
            // No debemos marcar la nueva revision
            // como ERROR por una respuesta anterior.

            contadores
                .modificadosDuranteSincronizacion++
        }

        contadores.procesados++
    }


    // ============================================
    // 9. INTEGRAR REGISTROS REMOTOS EN ROOM
    // ============================================

    private suspend fun integrarRegistrosRemotos(

        registrosRemotos: List<RecordatorioEntity>

    ): ResultadoIntegracion {

        return database.withTransaction {

            var insertados = 0

            var actualizados = 0

            var omitidos = 0


            // ====================================
            // 9.1 VALIDAR UUID UNICOS
            // ====================================

            require(

                registrosRemotos
                    .map { it.uuid }
                    .distinct()
                    .size == registrosRemotos.size

            ) {

                "La respuesta remota contiene UUID duplicados"
            }


            // ====================================
            // 9.2 PROCESAR REGISTROS REMOTOS
            // ====================================

            for (remoto in registrosRemotos) {

                val local =
                    dao.obtenerPorUuid(
                        remoto.uuid
                    )


                // =================================
                // CASO A: REGISTRO NUEVO
                // =================================

                if (local == null) {

                    dao.insertar(

                        remoto.copy(

                            id = 0,

                            estadoSincronizacion =
                                EstadoSincronizacion
                                    .SINCRONIZADO.valor
                        )
                    )

                    insertados++

                    continue
                }


                // =================================
                // CASO B: CAMBIOS LOCALES PENDIENTES
                // =================================

                val estadoLocal =
                    EstadoSincronizacion.desdeValor(
                        local.estadoSincronizacion
                    )

                if (
                    estadoLocal.requiereSincronizacion()
                ) {

                    // No sobrescribir modificaciones
                    // locales que todavia no han sido
                    // confirmadas por el servidor.

                    omitidos++

                    continue
                }


                // =================================
                // CASO C: REVISION REMOTA ANTIGUA
                // =================================

                if (
                    remoto.revision < local.revision
                ) {

                    omitidos++

                    continue
                }


                // =================================
                // CASO D: MISMA REVISION
                // =================================

                if (
                    remoto.revision == local.revision
                ) {

                    check(
                        remoto.toDto() == local.toDto()
                    ) {

                        "Conflicto de contenido para " +
                                "UUID ${local.uuid}"
                    }

                    omitidos++

                    continue
                }


                // =================================
                // CASO E: PROTEGER ELIMINACION
                // =================================

                if (
                    local.eliminado &&
                    !remoto.eliminado
                ) {

                    // Evitar resucitar un registro
                    // eliminado logicamente.

                    omitidos++

                    continue
                }


                // =================================
                // CASO F: REVISION REMOTA NUEVA
                // =================================

                check(
                    remoto.fechaCreacion ==
                            local.fechaCreacion
                ) {

                    "La fecha de creacion remota " +
                            "no coincide con la local"
                }

                val filasActualizadas =
                    dao.actualizar(

                        remoto.copy(

                            id = local.id,

                            estadoSincronizacion =
                                EstadoSincronizacion
                                    .SINCRONIZADO.valor
                        )
                    )

                check(filasActualizadas == 1) {

                    "No se pudo actualizar el registro remoto"
                }

                actualizados++
            }


            // ====================================
            // 9.3 RESULTADO DE LA TRANSACCION
            // ====================================

            ResultadoIntegracion(

                insertados = insertados,

                actualizados = actualizados,

                omitidos = omitidos
            )
        }
    }
}
 