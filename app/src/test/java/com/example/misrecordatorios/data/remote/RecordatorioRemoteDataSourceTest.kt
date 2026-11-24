
package com.example.misrecordatorios.data.remote

import com.example.misrecordatorios.data.local.RecordatorioEntity
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

class RecordatoriosRemoteDataSourceTest {

    // ============================================
    // 1. DATOS DE PRUEBA
    // ============================================

    private val uuidPrueba =
        "550e8400-e29b-41d4-a716-446655440000"

    private val segundoUuid =
        "550e8400-e29b-41d4-a716-446655440001"

    private fun crearEntityValida(): RecordatorioEntity {

        return RecordatorioEntity(

            id = 15,

            uuid = uuidPrueba,

            titulo = "Estudiar Kotlin y Room",

            descripcion = "Preparar Actividad 3",

            categoria = "Estudios",

            prioridad = "Alta",

            estado = "Pendiente",

            ubicacion = "Casa",

            fechaAviso = "2026-12-01",

            fechaCreacion = 1795478400000L,

            fechaModificacion = 1795478500000L,

            estadoSincronizacion = "PENDIENTE",

            revision = 3L,

            eliminado = false
        )
    }

    private fun crearDtoValido(): RecordatorioDto {

        return crearEntityValida().toDto()
    }


    // ============================================
    // 2. API SIMULADA
    // ============================================

    private class FakeRecordatoriosApi :
        RecordatoriosApi {

        var respuestaHealth: Response<HealthDto> =
            Response.success(
                HealthDto(
                    status = "UP",
                    service = "MisRecordatorios",
                    version = "1.0"
                )
            )

        var respuestaLista:
                Response<List<RecordatorioDto>> =
            Response.success(emptyList())

        var respuestaIndividual:
                Response<RecordatorioDto>? = null

        var respuestaGuardado:
                Response<RecordatorioDto>? = null

        var excepcion: Exception? = null

        var ultimoUuid: String? = null

        var ultimoDto: RecordatorioDto? = null

        var ultimoModoPrueba: String? = null

        var cantidadPut = 0

        var cantidadDelete = 0


        private fun comprobarExcepcion() {

            excepcion?.let {

                throw it
            }
        }


        override suspend fun verificarDisponibilidad(

            modoPrueba: String?

        ): Response<HealthDto> {

            ultimoModoPrueba = modoPrueba

            comprobarExcepcion()

            return respuestaHealth
        }


        override suspend fun obtenerRecordatorios(

            modoPrueba: String?

        ): Response<List<RecordatorioDto>> {

            ultimoModoPrueba = modoPrueba

            comprobarExcepcion()

            return respuestaLista
        }


        override suspend fun obtenerPorUuid(

            uuid: String,

            modoPrueba: String?

        ): Response<RecordatorioDto> {

            ultimoUuid = uuid

            ultimoModoPrueba = modoPrueba

            comprobarExcepcion()

            return respuestaIndividual
                ?: Response.error(
                    404,
                    "No encontrado".toResponseBody()
                )
        }


        override suspend fun guardarRecordatorio(

            uuid: String,

            recordatorio: RecordatorioDto,

            modoPrueba: String?

        ): Response<RecordatorioDto> {

            cantidadPut++

            ultimoUuid = uuid

            ultimoDto = recordatorio

            ultimoModoPrueba = modoPrueba

            comprobarExcepcion()

            return respuestaGuardado
                ?: Response.success(
                    200,
                    recordatorio
                )
        }


        override suspend fun eliminarRecordatorio(

            uuid: String,

            revision: Long,

            modoPrueba: String?

        ): Response<RecordatorioDto> {

            cantidadDelete++

            ultimoUuid = uuid

            ultimoModoPrueba = modoPrueba

            comprobarExcepcion()

            return Response.error(
                405,
                "DELETE no esperado".toResponseBody()
            )
        }
    }


    // ============================================
    // 3. INICIALIZAR DATASOURCE
    // ============================================

    private val api = FakeRecordatoriosApi()

    private val dataSource =
        RecordatoriosRemoteDataSource(api)


    // ============================================
    // 4. PRUEBAS DE DISPONIBILIDAD
    // ============================================

    @Test
    fun verificarServidorDisponible() {

        runBlocking {

            val resultado =
                dataSource.verificarDisponibilidad()

            assertTrue(resultado.esExitoso())

            assertEquals(
                200,
                resultado.obtenerCodigoHttp()
            )

            val health = requireNotNull(
                resultado.obtenerDatosONull()
            )

            assertEquals("UP", health.status)

            assertEquals(
                "MisRecordatorios",
                health.service
            )

            assertEquals("1.0", health.version)
        }
    }


    @Test
    fun rechazarServidorConEstadoInvalido() {

        runBlocking {

            api.respuestaHealth =
                Response.success(
                    HealthDto(
                        status = "DOWN",
                        service = "MisRecordatorios",
                        version = "1.0"
                    )
                )

            val resultado =
                dataSource.verificarDisponibilidad()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )
        }
    }


    @Test
    fun rechazarServicioRemotoIncorrecto() {

        runBlocking {

            api.respuestaHealth =
                Response.success(
                    HealthDto(
                        status = "UP",
                        service = "OtroServicio",
                        version = "1.0"
                    )
                )

            val resultado =
                dataSource.verificarDisponibilidad()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )
        }
    }


    // ============================================
    // 5. PRUEBAS DE CONSULTA
    // ============================================

    @Test
    fun obtenerListaDeRecordatoriosCorrectamente() {

        runBlocking {

            val primero = crearDtoValido()

            val segundo = primero.copy(
                uuid = segundoUuid,
                titulo = "Pagar electricidad"
            )

            api.respuestaLista =
                Response.success(
                    listOf(primero, segundo)
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(resultado.esExitoso())

            val entidades = requireNotNull(
                resultado.obtenerDatosONull()
            )

            assertEquals(2, entidades.size)

            assertEquals(
                uuidPrueba,
                entidades[0].uuid
            )

            assertEquals(
                segundoUuid,
                entidades[1].uuid
            )

            assertEquals(
                "SINCRONIZADO",
                entidades[0].estadoSincronizacion
            )
        }
    }


    @Test
    fun obtenerListaVaciaCorrectamente() {

        runBlocking {

            api.respuestaLista =
                Response.success(emptyList())

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(resultado.esExitoso())

            val entidades = requireNotNull(
                resultado.obtenerDatosONull()
            )

            assertTrue(entidades.isEmpty())
        }
    }


    @Test
    fun rechazarListaConUuidDuplicados() {

        runBlocking {

            val dto = crearDtoValido()

            api.respuestaLista =
                Response.success(
                    listOf(dto, dto)
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )

            assertNull(
                resultado.obtenerDatosONull()
            )
        }
    }


    @Test
    fun rechazarListaConDatosInvalidos() {

        runBlocking {

            val dtoValido = crearDtoValido()

            val dtoInvalido = dtoValido.copy(
                uuid = segundoUuid,
                categoria = "Desconocida"
            )

            api.respuestaLista =
                Response.success(
                    listOf(dtoValido, dtoInvalido)
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )

            assertNull(
                resultado.obtenerDatosONull()
            )
        }
    }


    @Test
    fun rechazarListaConElementoNulo() {

        runBlocking {

            // Simular un JSON que contiene null,
            // pese al tipo no nullable de Kotlin.

            @Suppress("UNCHECKED_CAST")
            val listaInvalida =
                listOf<RecordatorioDto?>(
                    crearDtoValido(),
                    null
                ) as List<RecordatorioDto>

            api.respuestaLista =
                Response.success(listaInvalida)

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )
        }
    }


    @Test
    fun obtenerRecordatorioPorUuidCorrectamente() {

        runBlocking {

            api.respuestaIndividual =
                Response.success(
                    crearDtoValido()
                )

            val resultado =
                dataSource.obtenerPorUuid(
                    uuid = uuidPrueba
                )

            assertTrue(resultado.esExitoso())

            val entity = requireNotNull(
                resultado.obtenerDatosONull()
            )

            assertEquals(uuidPrueba, entity.uuid)

            assertEquals(
                uuidPrueba,
                api.ultimoUuid
            )

            assertEquals(0, entity.id)

            assertEquals(
                "SINCRONIZADO",
                entity.estadoSincronizacion
            )
        }
    }


    @Test
    fun rechazarUuidDiferenteAlSolicitado() {

        runBlocking {

            api.respuestaIndividual =
                Response.success(
                    crearDtoValido().copy(
                        uuid = segundoUuid
                    )
                )

            val resultado =
                dataSource.obtenerPorUuid(
                    uuid = uuidPrueba
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )
        }
    }


    // ============================================
    // 6. PRUEBAS DE GUARDADO Y ELIMINACION
    // ============================================

    @Test
    fun crearRecordatorioHttp201() {

        runBlocking {

            val entity = crearEntityValida()

            api.respuestaGuardado =
                Response.success(
                    201,
                    entity.toDto()
                )

            val resultado =
                dataSource.guardarRecordatorio(
                    recordatorio = entity
                )

            assertTrue(resultado.esExitoso())

            assertEquals(
                201,
                resultado.obtenerCodigoHttp()
            )

            val recibido = requireNotNull(
                resultado.obtenerDatosONull()
            )

            assertEquals(
                entity.copy(
                    estadoSincronizacion =
                        "SINCRONIZADO"
                ),
                recibido
            )

            assertEquals(1, api.cantidadPut)

            assertEquals(
                entity.toDto(),
                api.ultimoDto
            )
        }
    }


    @Test
    fun actualizarRecordatorioHttp200() {

        runBlocking {

            val entity = crearEntityValida().copy(
                titulo = "Estudiar Retrofit",
                revision = 4L
            )

            val resultado =
                dataSource.guardarRecordatorio(
                    recordatorio = entity
                )

            assertTrue(resultado.esExitoso())

            assertEquals(
                200,
                resultado.obtenerCodigoHttp()
            )

            val recibido = requireNotNull(
                resultado.obtenerDatosONull()
            )

            assertEquals(15, recibido.id)

            assertEquals(4L, recibido.revision)

            assertEquals(
                "Estudiar Retrofit",
                recibido.titulo
            )

            assertEquals(
                uuidPrueba,
                api.ultimoUuid
            )
        }
    }


    @Test
    fun rechazarGuardadoConUuidIncorrecto() {

        runBlocking {

            val entity = crearEntityValida()

            api.respuestaGuardado =
                Response.success(
                    entity.toDto().copy(
                        uuid = segundoUuid
                    )
                )

            val resultado =
                dataSource.guardarRecordatorio(
                    recordatorio = entity
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )
        }
    }


    @Test
    fun rechazarGuardadoConRevisionIncorrecta() {

        runBlocking {

            val entity = crearEntityValida()

            api.respuestaGuardado =
                Response.success(
                    entity.toDto().copy(
                        revision = 2L
                    )
                )

            val resultado =
                dataSource.guardarRecordatorio(
                    recordatorio = entity
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )
        }
    }


    @Test
    fun rechazarGuardadoConContenidoDiferente() {

        runBlocking {

            val entity = crearEntityValida()

            api.respuestaGuardado =
                Response.success(
                    entity.toDto().copy(
                        titulo = "Titulo modificado"
                    )
                )

            val resultado =
                dataSource.guardarRecordatorio(
                    recordatorio = entity
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )
        }
    }


    @Test
    fun sincronizarEliminacionLogicaMediantePut() {

        runBlocking {

            val entity = crearEntityValida().copy(
                eliminado = true,
                revision = 5L
            )

            val resultado =
                dataSource.eliminarRecordatorio(
                    recordatorio = entity
                )

            assertTrue(resultado.esExitoso())

            val recibido = requireNotNull(
                resultado.obtenerDatosONull()
            )

            assertTrue(recibido.eliminado)

            assertEquals(5L, recibido.revision)

            assertEquals(1, api.cantidadPut)

            assertEquals(0, api.cantidadDelete)

            assertEquals(
                true,
                api.ultimoDto?.eliminado
            )

            assertEquals(
                uuidPrueba,
                api.ultimoUuid
            )
        }
    }


    @Test
    fun rechazarEliminacionDeRegistroActivo() {

        runBlocking {

            val entity = crearEntityValida().copy(
                eliminado = false
            )

            val resultado =
                dataSource.eliminarRecordatorio(
                    recordatorio = entity
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )

            assertEquals(0, api.cantidadPut)

            assertEquals(0, api.cantidadDelete)
        }
    }


    // ============================================
    // 7. PRUEBAS DE ERRORES HTTP
    // ============================================

    @Test
    fun clasificarErrorHttp400() {

        runBlocking {

            api.respuestaLista =
                Response.error(
                    400,
                    "Solicitud invalida".toResponseBody()
                )

            val resultado =
                dataSource.obtenerRecordatorios(
                    modoPrueba = "bad_request"
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorCliente
            )

            assertEquals(
                400,
                resultado.obtenerCodigoHttp()
            )

            assertFalse(
                resultado.puedeReintentarse()
            )

            assertEquals(
                "bad_request",
                api.ultimoModoPrueba
            )
        }
    }


    @Test
    fun clasificarErrorHttp404() {

        runBlocking {

            val resultado =
                dataSource.obtenerPorUuid(
                    uuid = uuidPrueba
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorCliente
            )

            assertEquals(
                404,
                resultado.obtenerCodigoHttp()
            )

            assertFalse(
                resultado.puedeReintentarse()
            )
        }
    }


    @Test
    fun clasificarConflictoHttp409() {

        runBlocking {

            api.respuestaGuardado =
                Response.error(
                    409,
                    "Conflicto de revision".toResponseBody()
                )

            val resultado =
                dataSource.guardarRecordatorio(
                    recordatorio = crearEntityValida()
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorCliente
            )

            assertEquals(
                409,
                resultado.obtenerCodigoHttp()
            )

            assertFalse(
                resultado.puedeReintentarse()
            )
        }
    }


    @Test
    fun clasificarErrorHttp429() {

        runBlocking {

            api.respuestaLista =
                Response.error(
                    429,
                    "Demasiadas solicitudes".toResponseBody()
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorCliente
            )

            assertEquals(
                429,
                resultado.obtenerCodigoHttp()
            )

            assertTrue(
                resultado.puedeReintentarse()
            )
        }
    }


    @Test
    fun clasificarErrorHttp500() {

        runBlocking {

            api.respuestaLista =
                Response.error(
                    500,
                    "Error interno".toResponseBody()
                )

            val resultado =
                dataSource.obtenerRecordatorios(
                    modoPrueba = "server_error"
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorServidor
            )

            assertEquals(
                500,
                resultado.obtenerCodigoHttp()
            )

            assertTrue(
                resultado.puedeReintentarse()
            )
        }
    }


    @Test
    fun rechazarRespuestaExitosaSinCuerpo() {

        runBlocking {

            api.respuestaLista =
                Response.success<List<RecordatorioDto>>(
                    204,
                    null
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.RespuestaInesperada
            )

            assertEquals(
                204,
                resultado.obtenerCodigoHttp()
            )

            assertNull(
                resultado.obtenerDatosONull()
            )
        }
    }


    // ============================================
    // 8. PRUEBAS DE EXCEPCIONES
    // ============================================

    @Test
    fun clasificarSocketTimeoutException() {

        runBlocking {

            api.excepcion =
                SocketTimeoutException(
                    "Read timed out"
                )

            val resultado =
                dataSource.obtenerRecordatorios(
                    modoPrueba = "timeout"
                )

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.Timeout
            )

            assertTrue(
                resultado.puedeReintentarse()
            )

            assertNull(
                resultado.obtenerCodigoHttp()
            )
        }
    }


    @Test
    fun clasificarInterruptedIOExceptionTimeout() {

        runBlocking {

            api.excepcion =
                InterruptedIOException(
                    "timeout"
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.Timeout
            )

            assertTrue(
                resultado.puedeReintentarse()
            )
        }
    }


    @Test
    fun clasificarErrorDeConexion() {

        runBlocking {

            api.excepcion =
                IOException(
                    "Connection refused"
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorConexion
            )

            assertTrue(
                resultado.puedeReintentarse()
            )

            assertNull(
                resultado.obtenerDatosONull()
            )
        }
    }


    @Test
    fun clasificarJsonMalformado() {

        runBlocking {

            api.excepcion =
                MalformedJsonException(
                    "JSON malformado"
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )

            assertFalse(
                resultado.puedeReintentarse()
            )
        }
    }


    @Test
    fun clasificarJsonSyntaxException() {

        runBlocking {

            api.excepcion =
                JsonSyntaxException(
                    "Formato JSON incorrecto"
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.DatosInvalidos
            )

            assertFalse(
                resultado.puedeReintentarse()
            )
        }
    }


    @Test
    fun clasificarExcepcionDesconocida() {

        runBlocking {

            api.excepcion =
                IllegalStateException(
                    "Error inesperado"
                )

            val resultado =
                dataSource.obtenerRecordatorios()

            assertTrue(
                resultado is
                        ResultadoRemoto.Error.ErrorDesconocido
            )

            assertFalse(
                resultado.puedeReintentarse()
            )
        }
    }


    @Test
    fun propagarCancellationException() {

        runBlocking {

            api.excepcion =
                CancellationException(
                    "Operacion cancelada"
                )

            try {

                dataSource.obtenerRecordatorios()

                fail(
                    "Se esperaba CancellationException"
                )

            } catch (e: CancellationException) {

                assertEquals(
                    "Operacion cancelada",
                    e.message
                )
            }
        }
    }
}
 