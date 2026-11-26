
package com.example.misrecordatorios.data.remote

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.misrecordatorios.data.local.RecordatorioEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RecordatoriosRemoteDataSourceIntegrationTest {

    companion object {
        private const val TAG = "RemoteIntegrationTest"
    }

    private lateinit var remoteDataSource:
            RecordatoriosRemoteDataSource

    private val registrosDePrueba =
        mutableSetOf<String>()

    // ============================================
    // CONFIGURACION INICIAL
    // ============================================

    @Before
    fun prepararPrueba() {

        remoteDataSource =
            RecordatoriosRemoteDataSource(
                RetrofitClient.api
            )

        registrosDePrueba.clear()
    }

    // ============================================
    // LIMPIEZA DE REGISTROS DE PRUEBA
    // ============================================

    @After
    fun limpiarPrueba() {

        runBlocking {

            for (uuid in registrosDePrueba) {

                try {

                    val consulta =
                        remoteDataSource.obtenerPorUuid(uuid)

                    if (consulta is ResultadoRemoto.Exito) {

                        val registro = consulta.datos

                        if (!registro.eliminado) {

                            val eliminado = registro.copy(
                                eliminado = true,
                                revision = registro.revision + 1L,
                                fechaModificacion =
                                    System.currentTimeMillis(),
                                estadoSincronizacion = "PENDIENTE"
                            )

                            val resultado =
                                remoteDataSource
                                    .eliminarRecordatorio(eliminado)

                            if (resultado is ResultadoRemoto.Error) {

                                Log.w(
                                    TAG,
                                    "No se pudo limpiar $uuid: " +
                                            resultado.mensaje
                                )
                            }
                        }

                    } else if (consulta is ResultadoRemoto.Error) {

                        Log.w(
                            TAG,
                            "No se pudo consultar $uuid: " +
                                    consulta.mensaje
                        )
                    }

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    Log.w(
                        TAG,
                        "Error limpiando registro $uuid",
                        e
                    )
                }
            }

            registrosDePrueba.clear()
        }
    }

    // ============================================
    // GENERADOR DE RECORDATORIOS
    // ============================================

    private fun crearRecordatorio(
        titulo: String = "Prueba integracion remota",
        eliminado: Boolean = false,
        revision: Long = 1L
    ): RecordatorioEntity {

        val uuid = UUID.randomUUID().toString()
        val ahora = System.currentTimeMillis()

        registrosDePrueba.add(uuid)

        return RecordatorioEntity(
            id = 0,
            uuid = uuid,
            titulo = titulo,
            descripcion =
                "Registro generado por prueba instrumentada",
            categoria = "Estudios",
            prioridad = "Alta",
            estado = "Pendiente",
            ubicacion = null,
            fechaAviso = null,
            fechaCreacion = ahora,
            fechaModificacion = ahora,
            estadoSincronizacion = "PENDIENTE",
            revision = revision,
            eliminado = eliminado
        )
    }

    // ============================================
    // VALIDAR RESPUESTAS EXITOSAS
    // ============================================

    private fun <T> exigirExito(
        resultado: ResultadoRemoto<T>,
        codigoEsperado: Int
    ): T {

        if (resultado !is ResultadoRemoto.Exito) {

            throw AssertionError(
                "Se esperaba HTTP $codigoEsperado. " +
                        "Resultado obtenido: $resultado"
            )
        }

        assertEquals(
            codigoEsperado,
            resultado.codigoHttp
        )

        return resultado.datos
    }

    // ============================================
    // VALIDAR ERRORES HTTP
    // ============================================

    private fun exigirErrorHttp(
        resultado: ResultadoRemoto<*>,
        codigoEsperado: Int
    ) {

        assertTrue(
            "Se esperaba un error HTTP $codigoEsperado. " +
                    "Resultado obtenido: $resultado",
            resultado is ResultadoRemoto.Error
        )

        assertEquals(
            codigoEsperado,
            resultado.obtenerCodigoHttp()
        )
    }

    // ============================================
    // PRUEBA 01
    // DISPONIBILIDAD DEL SERVIDOR
    // ============================================

    @Test
    fun verificarDisponibilidadServidor() {

        runBlocking {

            val resultado =
                remoteDataSource.verificarDisponibilidad()

            val health =
                exigirExito(resultado, 200)

            assertEquals("UP", health.status)

            assertEquals(
                "MisRecordatorios",
                health.service
            )

            assertEquals("1.0", health.version)

            Log.i(
                TAG,
                "PR-01: Servidor disponible"
            )
        }
    }

    // ============================================
    // PRUEBA 02
    // CONSULTAR RECORDATORIOS REMOTOS
    // ============================================

    @Test
    fun consultarRecordatoriosRemotos() {

        runBlocking {

            val resultado =
                remoteDataSource.obtenerRecordatorios()

            val registros =
                exigirExito(resultado, 200)

            val uuids =
                registros.map { it.uuid }

            assertEquals(
                uuids.size,
                uuids.distinct().size
            )

            registros.forEach { registro ->

                assertTrue(
                    registro.titulo.isNotBlank()
                )

                assertTrue(
                    registro.revision >= 1L
                )

                assertEquals(
                    "SINCRONIZADO",
                    registro.estadoSincronizacion
                )
            }

            Log.i(
                TAG,
                "PR-02: ${registros.size} registros consultados"
            )
        }
    }

    // ============================================
    // PRUEBA 03
    // CONSULTAR UN REGISTRO POR UUID
    // ============================================

    @Test
    fun consultarRegistroPorUuid() {

        runBlocking {

            val registro = crearRecordatorio(
                titulo = "PR-03 Consulta UUID"
            )

            exigirExito(
                remoteDataSource.guardarRecordatorio(registro),
                201
            )

            val resultado =
                remoteDataSource.obtenerPorUuid(registro.uuid)

            val remoto =
                exigirExito(resultado, 200)

            assertEquals(
                registro.uuid,
                remoto.uuid
            )

            assertEquals(
                registro.titulo,
                remoto.titulo
            )

            assertEquals(
                registro.revision,
                remoto.revision
            )

            Log.i(
                TAG,
                "PR-03: UUID consultado correctamente"
            )
        }
    }

    // ============================================
    // PRUEBA 04
    // CREAR UN REGISTRO REMOTO
    // ============================================

    @Test
    fun crearRegistroRemoto() {

        runBlocking {

            val registro = crearRecordatorio(
                titulo = "PR-04 Creacion remota"
            )

            val resultado =
                remoteDataSource.guardarRecordatorio(registro)

            val remoto =
                exigirExito(resultado, 201)

            assertEquals(
                registro.uuid,
                remoto.uuid
            )

            assertEquals(
                registro.titulo,
                remoto.titulo
            )

            assertEquals(
                registro.descripcion,
                remoto.descripcion
            )

            assertEquals(
                registro.categoria,
                remoto.categoria
            )

            assertEquals(
                registro.prioridad,
                remoto.prioridad
            )

            assertEquals(
                1L,
                remoto.revision
            )

            assertFalse(remoto.eliminado)

            assertEquals(
                "SINCRONIZADO",
                remoto.estadoSincronizacion
            )

            Log.i(
                TAG,
                "PR-04: Registro creado HTTP 201"
            )
        }
    }

    // ============================================
    // PRUEBA 05
    // ACTUALIZAR UN REGISTRO REMOTO
    // ============================================

    @Test
    fun actualizarRegistroRemoto() {

        runBlocking {

            val original = crearRecordatorio(
                titulo = "PR-05 Titulo original"
            )

            exigirExito(
                remoteDataSource.guardarRecordatorio(original),
                201
            )

            val actualizado = original.copy(
                titulo = "PR-05 Titulo actualizado",
                descripcion = "Descripcion modificada",
                categoria = "Trabajo",
                prioridad = "Media",
                estado = "Completado",
                revision = 2L,
                fechaModificacion =
                    System.currentTimeMillis()
            )

            val resultado =
                remoteDataSource.guardarRecordatorio(actualizado)

            val remoto =
                exigirExito(resultado, 200)

            assertEquals(
                original.uuid,
                remoto.uuid
            )

            assertEquals(
                actualizado.titulo,
                remoto.titulo
            )

            assertEquals(
                actualizado.descripcion,
                remoto.descripcion
            )

            assertEquals(
                "Trabajo",
                remoto.categoria
            )

            assertEquals(
                "Media",
                remoto.prioridad
            )

            assertEquals(
                "Completado",
                remoto.estado
            )

            assertEquals(
                2L,
                remoto.revision
            )

            assertEquals(
                original.fechaCreacion,
                remoto.fechaCreacion
            )

            Log.i(
                TAG,
                "PR-05: Actualizacion HTTP 200"
            )
        }
    }

    // ============================================
    // PRUEBA 06
    // COMPROBAR PERSISTENCIA REMOTA
    // ============================================

    @Test
    fun comprobarPersistenciaRemota() {

        runBlocking {

            val registro = crearRecordatorio(
                titulo = "PR-06 Persistencia remota"
            )

            exigirExito(
                remoteDataSource.guardarRecordatorio(registro),
                201
            )

            val nuevoDataSource =
                RecordatoriosRemoteDataSource(
                    RetrofitClient.api
                )

            val resultado =
                nuevoDataSource.obtenerPorUuid(registro.uuid)

            val recuperado =
                exigirExito(resultado, 200)

            assertEquals(
                registro.toDto(),
                recuperado.toDto()
            )

            val listado = exigirExito(
                nuevoDataSource.obtenerRecordatorios(),
                200
            )

            assertEquals(
                1,
                listado.count {
                    it.uuid == registro.uuid
                }
            )

            Log.i(
                TAG,
                "PR-06: Registro recuperado desde el servidor"
            )
        }
    }

    // ============================================
    // PRUEBA 07
    // IDEMPOTENCIA Y AUSENCIA DE DUPLICADOS
    // ============================================

    @Test
    fun guardarRegistroRepetidoSinDuplicados() {

        runBlocking {

            val registro = crearRecordatorio(
                titulo = "PR-07 Idempotencia"
            )

            exigirExito(
                remoteDataSource.guardarRecordatorio(registro),
                201
            )

            repeat(3) {

                val resultado =
                    remoteDataSource.guardarRecordatorio(registro)

                val remoto =
                    exigirExito(resultado, 200)

                assertEquals(
                    registro.uuid,
                    remoto.uuid
                )

                assertEquals(
                    1L,
                    remoto.revision
                )
            }

            val registros = exigirExito(
                remoteDataSource.obtenerRecordatorios(),
                200
            )

            assertEquals(
                1,
                registros.count {
                    it.uuid == registro.uuid
                }
            )

            Log.i(
                TAG,
                "PR-07: Cuatro envios, un unico registro"
            )
        }
    }

    // ============================================
    // PRUEBA 08
    // RECHAZAR UNA REVISION OBSOLETA
    // ============================================

    @Test
    fun rechazarRevisionObsoleta() {

        runBlocking {

            val original = crearRecordatorio(
                titulo = "PR-08 Revision original"
            )

            exigirExito(
                remoteDataSource.guardarRecordatorio(original),
                201
            )

            val actualizado = original.copy(
                titulo = "PR-08 Revision actualizada",
                revision = 2L,
                fechaModificacion =
                    System.currentTimeMillis()
            )

            exigirExito(
                remoteDataSource.guardarRecordatorio(actualizado),
                200
            )

            val resultado =
                remoteDataSource.guardarRecordatorio(original)

            exigirErrorHttp(
                resultado,
                409
            )

            // REFERENCIA CORREGIDA

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorCliente
            )

            val remoto = exigirExito(
                remoteDataSource.obtenerPorUuid(original.uuid),
                200
            )

            assertEquals(
                2L,
                remoto.revision
            )

            assertEquals(
                actualizado.titulo,
                remoto.titulo
            )

            Log.i(
                TAG,
                "PR-08: Revision obsoleta rechazada HTTP 409"
            )
        }
    }

    // ============================================
    // PRUEBA 09
    // CONFLICTO CON LA MISMA REVISION
    // ============================================

    @Test
    fun rechazarConflictoDeRevision() {

        runBlocking {

            val original = crearRecordatorio(
                titulo = "PR-09 Registro original"
            )

            exigirExito(
                remoteDataSource.guardarRecordatorio(original),
                201
            )

            val incompatible = original.copy(
                titulo = "PR-09 Cambio incompatible"
            )

            val resultado =
                remoteDataSource.guardarRecordatorio(incompatible)

            exigirErrorHttp(
                resultado,
                409
            )

            val remoto = exigirExito(
                remoteDataSource.obtenerPorUuid(original.uuid),
                200
            )

            assertEquals(
                original.titulo,
                remoto.titulo
            )

            assertEquals(
                1L,
                remoto.revision
            )

            Log.i(
                TAG,
                "PR-09: Conflicto de revision HTTP 409"
            )
        }
    }

    // ============================================
    // PRUEBA 10
    // ELIMINACION LOGICA REMOTA
    // ============================================

    @Test
    fun eliminarRegistroLogicamente() {

        runBlocking {

            val original = crearRecordatorio(
                titulo = "PR-10 Eliminacion logica"
            )

            exigirExito(
                remoteDataSource.guardarRecordatorio(original),
                201
            )

            val eliminado = original.copy(
                eliminado = true,
                revision = 2L,
                fechaModificacion =
                    System.currentTimeMillis()
            )

            val resultado =
                remoteDataSource.eliminarRecordatorio(eliminado)

            val remoto =
                exigirExito(resultado, 200)

            assertTrue(remoto.eliminado)

            assertEquals(
                2L,
                remoto.revision
            )

            assertEquals(
                original.uuid,
                remoto.uuid
            )

            val consulta = exigirExito(
                remoteDataSource.obtenerPorUuid(original.uuid),
                200
            )

            assertTrue(consulta.eliminado)

            assertEquals(
                2L,
                consulta.revision
            )

            Log.i(
                TAG,
                "PR-10: Eliminacion logica confirmada"
            )
        }
    }

    // ============================================
    // PRUEBA 11
    // ELIMINAR SIN CREACION REMOTA PREVIA
    // ============================================

    @Test
    fun sincronizarEliminacionSinCreacionPrevia() {

        runBlocking {

            val eliminado = crearRecordatorio(
                titulo = "PR-11 Eliminado antes de sincronizar",
                eliminado = true,
                revision = 2L
            )

            val resultado =
                remoteDataSource.eliminarRecordatorio(eliminado)

            val remoto =
                exigirExito(resultado, 201)

            assertTrue(remoto.eliminado)

            assertEquals(
                2L,
                remoto.revision
            )

            assertEquals(
                eliminado.uuid,
                remoto.uuid
            )

            val consulta = exigirExito(
                remoteDataSource.obtenerPorUuid(eliminado.uuid),
                200
            )

            assertTrue(consulta.eliminado)

            val registros = exigirExito(
                remoteDataSource.obtenerRecordatorios(),
                200
            )

            assertEquals(
                1,
                registros.count {
                    it.uuid == eliminado.uuid
                }
            )

            Log.i(
                TAG,
                "PR-11: Tombstone creado sin registro previo"
            )
        }
    }

    // ============================================
    // PRUEBA 12
    // ERROR HTTP 400
    // ============================================

    @Test
    fun detectarSolicitudIncorrecta() {

        runBlocking {

            val resultado =
                remoteDataSource.obtenerRecordatorios(
                    modoPrueba = "bad_request"
                )

            exigirErrorHttp(
                resultado,
                400
            )

            // REFERENCIA CORREGIDA

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorCliente
            )

            assertFalse(
                resultado.puedeReintentarse()
            )

            Log.i(
                TAG,
                "PR-12: HTTP 400 controlado"
            )
        }
    }

    // ============================================
    // PRUEBA 13
    // ERROR HTTP 404
    // ============================================

    @Test
    fun detectarRegistroInexistente() {

        runBlocking {

            val uuidInexistente =
                UUID.randomUUID().toString()

            val resultado =
                remoteDataSource.obtenerPorUuid(uuidInexistente)

            exigirErrorHttp(
                resultado,
                404
            )

            // REFERENCIA CORREGIDA

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorCliente
            )

            assertFalse(
                resultado.puedeReintentarse()
            )

            Log.i(
                TAG,
                "PR-13: HTTP 404 controlado"
            )
        }
    }

    // ============================================
    // PRUEBA 14
    // ERROR HTTP 500
    // ============================================

    @Test
    fun detectarErrorInternoServidor() {

        runBlocking {

            val resultado =
                remoteDataSource.obtenerRecordatorios(
                    modoPrueba = "server_error"
                )

            exigirErrorHttp(
                resultado,
                500
            )

            // REFERENCIA CORREGIDA

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorServidor
            )

            assertTrue(
                resultado.puedeReintentarse()
            )

            exigirExito(
                remoteDataSource.verificarDisponibilidad(),
                200
            )

            Log.i(
                TAG,
                "PR-14: HTTP 500 controlado y recuperado"
            )
        }
    }

    // ============================================
    // PRUEBA 15
    // TIMEOUT Y RECUPERACION
    // ============================================

    @Test
    fun detectarTimeoutYRecuperacion() {

        runBlocking {

            val resultado =
                remoteDataSource.obtenerRecordatorios(
                    modoPrueba = "timeout"
                )

            // REFERENCIA CORREGIDA

            assertTrue(
                "Se esperaba Timeout. " +
                        "Resultado obtenido: $resultado",
                resultado is
                        ResultadoRemoto.Error.Timeout
            )

            assertTrue(
                resultado.puedeReintentarse()
            )

            val recuperacion =
                remoteDataSource.obtenerRecordatorios()

            exigirExito(
                recuperacion,
                200
            )

            Log.i(
                TAG,
                "PR-15: Timeout controlado y conexion recuperada"
            )
        }
    }
}
 