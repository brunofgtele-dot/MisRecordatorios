package com.example.misrecordatorios.data.sync

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.misrecordatorios.data.local.RecordatorioDao
import com.example.misrecordatorios.data.local.RecordatorioEntity
import com.example.misrecordatorios.data.local.RecordatoriosDatabase

import com.example.misrecordatorios.data.remote.RecordatorioDto
import com.example.misrecordatorios.data.remote.RecordatoriosApi
import com.example.misrecordatorios.data.remote.RecordatoriosRemoteDataSource
import com.example.misrecordatorios.data.remote.toDto

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


@RunWith(AndroidJUnit4::class)
class RecordatoriosSyncManagerIntegrationTest {

    // ============================================
    // 1. COMPONENTES DE PRUEBA
    // ============================================

    private lateinit var database: RecordatoriosDatabase

    private lateinit var dao: RecordatorioDao

    private lateinit var servidor: MockWebServer

    private lateinit var clienteHttp: OkHttpClient

    private lateinit var syncManager: RecordatoriosSyncManager

    private lateinit var gson: Gson


    // Almacenamiento remoto aislado por prueba.

    private val registrosRemotos =
        ConcurrentHashMap<String, RecordatorioDto>()


    // Control de errores HTTP simulados.

    private val fallos500Restantes =
        AtomicInteger(0)


    // Control de timeout simulado.

    private val timeoutsRestantes =
        AtomicInteger(0)


    // Contador de solicitudes PUT.

    private val solicitudesPut =
        AtomicInteger(0)


    // ============================================
    // 2. PREPARACION DE CADA PRUEBA
    // ============================================

    @Before
    fun prepararPrueba() {

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext


        database = Room.inMemoryDatabaseBuilder(

            context,

            RecordatoriosDatabase::class.java

        ).build()


        dao = database.recordatorioDao()


        gson = GsonBuilder()
            .serializeNulls()
            .create()


        registrosRemotos.clear()

        fallos500Restantes.set(0)

        timeoutsRestantes.set(0)

        solicitudesPut.set(0)


        servidor = MockWebServer()


        servidor.dispatcher =

            object : Dispatcher() {

                override fun dispatch(

                    request: RecordedRequest

                ): MockResponse {

                    return procesarSolicitud(request)
                }
            }


        servidor.start()


        clienteHttp = OkHttpClient.Builder()

            .connectTimeout(
                3,
                TimeUnit.SECONDS
            )

            .readTimeout(
                2,
                TimeUnit.SECONDS
            )

            .writeTimeout(
                3,
                TimeUnit.SECONDS
            )

            .callTimeout(
                5,
                TimeUnit.SECONDS
            )

            .retryOnConnectionFailure(false)

            .build()


        val api = Retrofit.Builder()

            .baseUrl(
                servidor.url("/")
            )

            .client(
                clienteHttp
            )

            .addConverterFactory(

                GsonConverterFactory.create(gson)
            )

            .build()

            .create(
                RecordatoriosApi::class.java
            )


        val remoteDataSource =
            RecordatoriosRemoteDataSource(api)


        syncManager = RecordatoriosSyncManager(

            database = database,

            remoteDataSource = remoteDataSource
        )
    }


    // ============================================
    // 3. LIMPIEZA DESPUES DE CADA PRUEBA
    // ============================================

    @After
    fun finalizarPrueba() {

        try {

            if (::clienteHttp.isInitialized) {

                clienteHttp.dispatcher.cancelAll()

                clienteHttp.connectionPool.evictAll()
            }

            if (::servidor.isInitialized) {

                servidor.shutdown()
            }

        } finally {

            if (::clienteHttp.isInitialized) {

                clienteHttp.dispatcher
                    .executorService
                    .shutdown()
            }

            if (::database.isInitialized) {

                database.close()
            }

            registrosRemotos.clear()
        }
    }


    // ============================================
    // 4. SIMULADOR DEL SERVICIO REMOTO
    // ============================================

    private fun procesarSolicitud(

        request: RecordedRequest

    ): MockResponse {

        val ruta =
            request.requestUrl?.encodedPath.orEmpty()


        if (

            fallos500Restantes.getAndUpdate {

                if (it > 0) it - 1 else 0

            } > 0

        ) {

            return respuestaJson(

                codigo = 500,

                contenido = mapOf(

                    "error" to "Error interno simulado"
                )
            )
        }


        if (

            timeoutsRestantes.getAndUpdate {

                if (it > 0) it - 1 else 0

            } > 0

        ) {

            return respuestaJson(

                codigo = 503,

                contenido = mapOf(

                    "error" to "Timeout simulado"
                )

            ).setHeadersDelay(

                4,

                TimeUnit.SECONDS
            )
        }


        return when {


            // GET /health

            request.method == "GET" &&
                    ruta == "/health" -> {

                respuestaJson(

                    codigo = 200,

                    contenido = mapOf(

                        "status" to "UP",

                        "service" to "MisRecordatorios",

                        "version" to "1.0"
                    )
                )
            }


            // GET /recordatorios

            request.method == "GET" &&
                    ruta == "/recordatorios" -> {

                respuestaJson(

                    codigo = 200,

                    contenido = registrosRemotos
                        .values
                        .sortedBy { it.uuid }
                )
            }


            // GET /recordatorios/{uuid}

            request.method == "GET" &&
                    ruta.startsWith("/recordatorios/") -> {

                val uuid =
                    ruta.substringAfterLast("/")


                val registro =
                    registrosRemotos[uuid]


                if (registro == null) {

                    respuestaJson(

                        codigo = 404,

                        contenido = mapOf(

                            "error" to "No encontrado"
                        )
                    )

                } else {

                    respuestaJson(

                        codigo = 200,

                        contenido = registro
                    )
                }
            }


            // PUT /recordatorios/{uuid}

            request.method == "PUT" &&
                    ruta.startsWith("/recordatorios/") -> {

                solicitudesPut.incrementAndGet()


                val uuid =
                    ruta.substringAfterLast("/")


                val dto = gson.fromJson(

                    request.body.readUtf8(),

                    RecordatorioDto::class.java
                )


                procesarPut(

                    uuid = uuid,

                    dto = dto
                )
            }


            else -> {

                respuestaJson(

                    codigo = 404,

                    contenido = mapOf(

                        "error" to "Ruta no encontrada"
                    )
                )
            }
        }
    }


    // ============================================
    // 5. CONTROL DE UUID Y REVISIONES REMOTAS
    // ============================================

    private fun procesarPut(

        uuid: String,

        dto: RecordatorioDto

    ): MockResponse {

        synchronized(registrosRemotos) {

            val revision = dto.revision


            if (

                dto.uuid != uuid ||

                revision == null ||

                revision < 1L

            ) {

                return respuestaJson(

                    codigo = 400,

                    contenido = mapOf(

                        "error" to "Datos invalidos"
                    )
                )
            }


            val anterior =
                registrosRemotos[uuid]


            if (anterior != null) {


                val revisionAnterior =
                    anterior.revision ?: 0L


                if (revision < revisionAnterior) {

                    return respuestaJson(

                        codigo = 409,

                        contenido = mapOf(

                            "error" to "Revision obsoleta"
                        )
                    )
                }


                if (revision == revisionAnterior) {


                    if (anterior != dto) {

                        return respuestaJson(

                            codigo = 409,

                            contenido = mapOf(

                                "error" to "Conflicto de revision"
                            )
                        )
                    }


                    return respuestaJson(

                        codigo = 200,

                        contenido = anterior
                    )
                }


                if (

                    anterior.eliminado == true &&

                    dto.eliminado != true

                ) {

                    return respuestaJson(

                        codigo = 409,

                        contenido = mapOf(

                            "error" to "Registro eliminado"
                        )
                    )
                }


                if (

                    anterior.fechaCreacion !=

                    dto.fechaCreacion

                ) {

                    return respuestaJson(

                        codigo = 409,

                        contenido = mapOf(

                            "error" to "Fecha de creacion invalida"
                        )
                    )
                }
            }


            registrosRemotos[uuid] = dto


            return respuestaJson(

                codigo =

                    if (anterior == null) {

                        201

                    } else {

                        200
                    },

                contenido = dto
            )
        }
    }


    // ============================================
    // 6. GENERAR RESPUESTAS JSON
    // ============================================

    private fun respuestaJson(

        codigo: Int,

        contenido: Any

    ): MockResponse {

        return MockResponse()

            .setResponseCode(codigo)

            .setHeader(

                "Content-Type",

                "application/json; charset=utf-8"
            )

            .setBody(

                gson.toJson(contenido)
            )
    }


    // ============================================
    // 7. GENERAR RECORDATORIOS DE PRUEBA
    // ============================================

    private fun crearRecordatorio(

        titulo: String = "Recordatorio de prueba",

        uuid: String = UUID.randomUUID().toString(),

        revision: Long = 1L,

        estadoSincronizacion: String = "PENDIENTE",

        eliminado: Boolean = false

    ): RecordatorioEntity {

        val ahora =
            System.currentTimeMillis()


        return RecordatorioEntity(

            id = 0,

            uuid = uuid,

            titulo = titulo,

            descripcion = "Prueba automatizada de sincronizacion",

            categoria = "Estudios",

            prioridad = "Alta",

            estado = "Pendiente",

            ubicacion = null,

            fechaAviso = null,

            fechaCreacion = ahora,

            fechaModificacion = ahora,

            estadoSincronizacion = estadoSincronizacion,

            revision = revision,

            eliminado = eliminado
        )
    }


    // ============================================
    // PRUEBA 01
    // SINCRONIZAR UN REGISTRO NUEVO
    // ============================================

    @Test
    fun sincronizarRegistroNuevo() {

        runBlocking {

            val registro =
                crearRecordatorio(

                    titulo = "Prueba Sync 120 - Nuevo"
                )


            dao.insertar(registro)


            val resultado =
                syncManager.sincronizar()


            assertTrue(

                resultado is ResultadoSincronizacion.Exito
            )


            val local =
                dao.obtenerPorUuid(registro.uuid)


            assertNotNull(local)


            assertEquals(

                "SINCRONIZADO",

                local?.estadoSincronizacion
            )


            assertEquals(

                registro.titulo,

                registrosRemotos[registro.uuid]?.titulo
            )


            assertEquals(

                1,

                resultado.resumen.sincronizados
            )


            assertEquals(

                1,

                registrosRemotos.size
            )
        }
    }


    // ============================================
    // PRUEBA 02
    // SINCRONIZAR VARIOS REGISTROS
    // ============================================

    @Test
    fun sincronizarVariosRegistros() {

        runBlocking {

            val registros =

                (1..3).map { numero ->

                    crearRecordatorio(

                        titulo = "Registro multiple $numero"
                    )
                }


            registros.forEach {

                dao.insertar(it)
            }


            val resultado =
                syncManager.sincronizar()


            assertTrue(

                resultado is ResultadoSincronizacion.Exito
            )


            assertEquals(

                3,

                resultado.resumen.sincronizados
            )


            assertEquals(

                3,

                registrosRemotos.size
            )


            assertEquals(

                3,

                dao.contarRegistrosTotales()
            )


            assertTrue(

                dao.obtenerPendientesSincronizacion()
                    .isEmpty()
            )


            registros.forEach { registro ->

                assertEquals(

                    "SINCRONIZADO",

                    dao.obtenerPorUuid(registro.uuid)
                        ?.estadoSincronizacion
                )
            }
        }
    }


    // ============================================
    // PRUEBA 03
    // SINCRONIZAR ACTUALIZACION
    // ============================================

    @Test
    fun sincronizarActualizacion() {

        runBlocking {

            val registro =
                crearRecordatorio(

                    titulo = "Titulo original"
                )


            val id =
                dao.insertar(registro).toInt()


            syncManager.sincronizar()


            val filas =
                dao.actualizarDatos(

                    id = id,

                    titulo = "Titulo actualizado",

                    descripcion = "Descripcion actualizada",

                    categoria = "Trabajo",

                    prioridad = "Media"
                )


            assertEquals(1, filas)


            val resultado =
                syncManager.sincronizar()


            assertTrue(

                resultado is ResultadoSincronizacion.Exito
            )


            val remoto =
                registrosRemotos[registro.uuid]


            assertEquals(

                "Titulo actualizado",

                remoto?.titulo
            )


            assertEquals(

                "Trabajo",

                remoto?.categoria
            )


            assertEquals(

                2L,

                remoto?.revision
            )


            assertEquals(

                1,

                registrosRemotos.size
            )


            assertEquals(

                "SINCRONIZADO",

                dao.obtenerPorUuid(registro.uuid)
                    ?.estadoSincronizacion
            )
        }
    }


    // ============================================
    // PRUEBA 04
    // SINCRONIZAR ELIMINACION LOGICA
    // ============================================

    @Test
    fun sincronizarEliminacionLogica() {

        runBlocking {

            val registro =
                crearRecordatorio(

                    titulo = "Registro para eliminar"
                )


            val id =
                dao.insertar(registro).toInt()


            syncManager.sincronizar()


            assertEquals(

                1,

                dao.eliminar(id)
            )


            val resultado =
                syncManager.sincronizar()


            assertTrue(

                resultado is ResultadoSincronizacion.Exito
            )


            val remoto =
                registrosRemotos[registro.uuid]


            assertEquals(

                true,

                remoto?.eliminado
            )


            assertEquals(

                2L,

                remoto?.revision
            )


            assertNull(

                dao.obtenerPorId(id)
            )


            assertEquals(

                0,

                dao.contarRecordatorios()
            )


            val local =
                dao.obtenerPorUuid(registro.uuid)


            assertEquals(

                true,

                local?.eliminado
            )


            assertEquals(

                "SINCRONIZADO",

                local?.estadoSincronizacion
            )
        }
    }


    // ============================================
    // PRUEBA 05
    // DESCARGAR REGISTROS REMOTOS
    // ============================================

    @Test
    fun descargarRegistroRemoto() {

        runBlocking {

            val registro =
                crearRecordatorio(

                    titulo = "Registro creado remotamente",

                    estadoSincronizacion = "SINCRONIZADO"
                )


            registrosRemotos[registro.uuid] =
                registro.toDto()


            assertEquals(

                0,

                dao.contarRegistrosTotales()
            )


            val resultado =
                syncManager.sincronizar()


            assertTrue(

                resultado is ResultadoSincronizacion.Exito
            )


            val local =
                dao.obtenerPorUuid(registro.uuid)


            assertNotNull(local)


            assertEquals(

                registro.titulo,

                local?.titulo
            )


            assertEquals(

                "SINCRONIZADO",

                local?.estadoSincronizacion
            )


            assertEquals(

                1,

                resultado.resumen.remotosInsertados
            )


            assertEquals(

                0,

                solicitudesPut.get()
            )
        }
    }


    // ============================================
    // PRUEBA 06
    // SINCRONIZACION SIN CAMBIOS
    // ============================================

    @Test
    fun sincronizacionSinCambios() {

        runBlocking {

            val resultado =
                syncManager.sincronizar()


            assertTrue(

                resultado is ResultadoSincronizacion.SinCambios
            )


            assertEquals(

                0,

                resultado.resumen.pendientesIniciales
            )


            assertEquals(

                0,

                resultado.resumen.sincronizados
            )


            assertEquals(

                0,

                dao.contarRegistrosTotales()
            )


            assertEquals(

                0,

                solicitudesPut.get()
            )
        }
    }


    // ============================================
    // PRUEBA 07
    // IDEMPOTENCIA Y AUSENCIA DE DUPLICADOS
    // ============================================

    @Test
    fun sincronizacionesRepetidasSinDuplicados() {

        runBlocking {

            val registro =
                crearRecordatorio(

                    titulo = "Registro idempotente"
                )


            dao.insertar(registro)


            val primerResultado =
                syncManager.sincronizar()


            assertTrue(

                primerResultado is ResultadoSincronizacion.Exito
            )


            val idOriginal =
                dao.obtenerPorUuid(registro.uuid)?.id


            repeat(3) {

                val resultado =
                    syncManager.sincronizar()


                assertTrue(

                    resultado is ResultadoSincronizacion.SinCambios
                )
            }


            assertEquals(

                1,

                dao.contarRegistrosTotales()
            )


            assertEquals(

                1,

                registrosRemotos.size
            )


            assertEquals(

                1,

                solicitudesPut.get()
            )


            assertEquals(

                idOriginal,

                dao.obtenerPorUuid(registro.uuid)?.id
            )
        }
    }


    // ============================================
    // PRUEBA 08
    // RECUPERACION ANTE HTTP 500
    // ============================================

    @Test
    fun recuperarSincronizacionDespuesDeHttp500() {

        runBlocking {

            val registro =
                crearRecordatorio(

                    titulo = "Registro con error HTTP 500"
                )


            dao.insertar(registro)


            fallos500Restantes.set(1)


            val resultadoError =
                syncManager.sincronizar()


            assertTrue(

                resultadoError is ResultadoSincronizacion.Error ||

                        resultadoError is ResultadoSincronizacion.Parcial
            )


            assertNotNull(

                dao.obtenerPorUuid(registro.uuid)
            )


            assertNotEquals(

                "SINCRONIZADO",

                dao.obtenerPorUuid(registro.uuid)
                    ?.estadoSincronizacion
            )


            assertEquals(

                0,

                registrosRemotos.size
            )


            val resultadoRecuperacion =
                syncManager.sincronizar(

                    reintentarErrores = true
                )


            assertTrue(

                resultadoRecuperacion
                        is ResultadoSincronizacion.Exito
            )


            assertEquals(

                "SINCRONIZADO",

                dao.obtenerPorUuid(registro.uuid)
                    ?.estadoSincronizacion
            )


            assertEquals(

                1,

                registrosRemotos.size
            )
        }
    }


    // ============================================
    // PRUEBA 09
    // RECUPERACION ANTE TIMEOUT
    // ============================================

    @Test
    fun recuperarSincronizacionDespuesDeTimeout() {

        runBlocking {

            val registro =
                crearRecordatorio(

                    titulo = "Registro con timeout"
                )


            dao.insertar(registro)


            timeoutsRestantes.set(1)


            val resultadoError =
                syncManager.sincronizar()


            assertTrue(

                resultadoError is ResultadoSincronizacion.Error ||

                        resultadoError is ResultadoSincronizacion.Parcial
            )


            assertTrue(

                resultadoError.puedeReintentarse()
            )


            assertNotNull(

                dao.obtenerPorUuid(registro.uuid)
            )


            assertNotEquals(

                "SINCRONIZADO",

                dao.obtenerPorUuid(registro.uuid)
                    ?.estadoSincronizacion
            )


            val resultadoRecuperacion =
                syncManager.sincronizar(

                    reintentarErrores = true
                )


            assertTrue(

                resultadoRecuperacion
                        is ResultadoSincronizacion.Exito
            )


            assertEquals(

                "SINCRONIZADO",

                dao.obtenerPorUuid(registro.uuid)
                    ?.estadoSincronizacion
            )


            assertEquals(

                1,

                registrosRemotos.size
            )
        }
    }


    // ============================================
    // PRUEBA 10
    // PROTEGER REVISIONES LOCALES
    // ============================================

    @Test
    fun noSobrescribirRevisionLocalMasReciente() {

        runBlocking {

            val local =
                crearRecordatorio(

                    titulo = "Version local reciente",

                    revision = 3L,

                    estadoSincronizacion = "SINCRONIZADO"
                )


            dao.insertar(local)


            val remotoAntiguo = local.copy(

                titulo = "Version remota antigua",

                revision = 2L
            )


            registrosRemotos[local.uuid] =
                remotoAntiguo.toDto()


            syncManager.sincronizar()


            val registroFinal =
                dao.obtenerPorUuid(local.uuid)


            assertEquals(

                "Version local reciente",

                registroFinal?.titulo
            )


            assertEquals(

                3L,

                registroFinal?.revision
            )


            assertEquals(

                1,

                dao.contarRegistrosTotales()
            )


            assertEquals(

                0,

                solicitudesPut.get()
            )
        }
    }


    // ============================================
    // PRUEBA 11
    // CONSERVAR CAMBIOS LOCALES ANTE CONFLICTOS
    // ============================================

    @Test
    fun conservarCambiosLocalesPendientes() {

        runBlocking {

            val local =
                crearRecordatorio(

                    titulo = "Cambio local pendiente",

                    revision = 2L
                )


            dao.insertar(local)


            val remoto = local.copy(

                titulo = "Cambio remoto incompatible",

                revision = 3L,

                estadoSincronizacion = "SINCRONIZADO"
            )


            registrosRemotos[local.uuid] =
                remoto.toDto()


            val resultado =
                syncManager.sincronizar()


            assertTrue(

                resultado is ResultadoSincronizacion.Error ||

                        resultado is ResultadoSincronizacion.Parcial
            )


            val registroFinal =
                dao.obtenerPorUuid(local.uuid)


            assertEquals(

                "Cambio local pendiente",

                registroFinal?.titulo
            )


            assertEquals(

                2L,

                registroFinal?.revision
            )


            assertNotEquals(

                "SINCRONIZADO",

                registroFinal?.estadoSincronizacion
            )


            assertEquals(

                "Cambio remoto incompatible",

                registrosRemotos[local.uuid]?.titulo
            )


            assertEquals(

                3L,

                registrosRemotos[local.uuid]?.revision
            )


            assertEquals(

                1,

                dao.contarRegistrosTotales()
            )
        }
    }


    // ============================================
    // PRUEBA 12
    // SINCRONIZACIONES CONCURRENTES
    // ============================================

    @Test
    fun sincronizacionesConcurrentesSinDuplicados() {

        runBlocking {

            val registros =

                (1..3).map { numero ->

                    crearRecordatorio(

                        titulo = "Registro concurrente $numero"
                    )
                }


            registros.forEach {

                dao.insertar(it)
            }


            val resultados =

                (1..5).map {

                    async(Dispatchers.IO) {

                        syncManager.sincronizar()
                    }

                }.awaitAll()


            assertEquals(

                5,

                resultados.size
            )


            assertTrue(

                resultados.all { resultado ->

                    resultado is ResultadoSincronizacion.Exito ||

                            resultado is ResultadoSincronizacion.SinCambios
                }
            )


            assertEquals(

                3,

                resultados.sumOf {

                    it.resumen.sincronizados
                }
            )


            assertEquals(

                3,

                solicitudesPut.get()
            )


            assertEquals(

                3,

                dao.contarRegistrosTotales()
            )


            assertEquals(

                3,

                registrosRemotos.size
            )


            assertTrue(

                dao.obtenerPendientesSincronizacion()
                    .isEmpty()
            )


            registros.forEach { registro ->

                assertEquals(

                    "SINCRONIZADO",

                    dao.obtenerPorUuid(registro.uuid)
                        ?.estadoSincronizacion
                )
            }
        }
    }
}
