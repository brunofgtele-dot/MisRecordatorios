
package com.example.misrecordatorios.data.remote

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.net.SocketTimeoutException

@RunWith(AndroidJUnit4::class)
class RetrofitClientTest {

    private val api = RetrofitClient.api

    private val tag = "RetrofitClientTest"


    // ============================================
    // PRUEBA 1: DISPONIBILIDAD DEL SERVIDOR
    // ============================================

    @Test
    fun verificarDisponibilidadServidor() {

        runBlocking {

            Log.i(tag, "Iniciando prueba de disponibilidad")

            val respuesta = api.verificarDisponibilidad()

            Log.i(tag, "Codigo HTTP: ${respuesta.code()}")

            assertEquals(200, respuesta.code())

            assertTrue(respuesta.isSuccessful)

            assertNotNull(respuesta.body())

            assertEquals(
                "UP",
                respuesta.body()?.status
            )

            assertEquals(
                "MisRecordatorios",
                respuesta.body()?.service
            )

            Log.i(
                tag,
                "PRUEBA 1 OK: Servidor disponible"
            )
        }
    }


    // ============================================
    // PRUEBA 2: CONSULTAR RECORDATORIOS
    // ============================================

    @Test
    fun consultarRecordatoriosRemotos() {

        runBlocking {

            Log.i(
                tag,
                "Iniciando consulta de recordatorios"
            )

            val respuesta = api.obtenerRecordatorios()

            assertEquals(200, respuesta.code())

            assertTrue(respuesta.isSuccessful)

            assertNotNull(respuesta.body())

            val recordatorios = requireNotNull(
                respuesta.body()
            )

            Log.i(
                tag,
                "Cantidad de registros: ${recordatorios.size}"
            )

            Log.i(
                tag,
                "PRUEBA 2 OK: Consulta remota exitosa"
            )
        }
    }


    // ============================================
    // PRUEBA 3: ERROR HTTP 400
    // ============================================

    @Test
    fun detectarErrorHttp400() {

        runBlocking {

            Log.i(tag, "Iniciando prueba HTTP 400")

            val respuesta = api.obtenerRecordatorios(
                modoPrueba = "bad_request"
            )

            assertEquals(400, respuesta.code())

            assertFalse(respuesta.isSuccessful)

            respuesta.errorBody()?.close()

            Log.i(
                tag,
                "PRUEBA 3 OK: Error HTTP 400 detectado"
            )
        }
    }


    // ============================================
    // PRUEBA 4: ERROR HTTP 500
    // ============================================

    @Test
    fun detectarErrorHttp500() {

        runBlocking {

            Log.i(tag, "Iniciando prueba HTTP 500")

            val respuesta = api.obtenerRecordatorios(
                modoPrueba = "server_error"
            )

            assertEquals(500, respuesta.code())

            assertFalse(respuesta.isSuccessful)

            respuesta.errorBody()?.close()

            Log.i(
                tag,
                "PRUEBA 4 OK: Error HTTP 500 detectado"
            )
        }
    }


    // ============================================
    // PRUEBA 5: TIMEOUT
    // ============================================

    @Test
    fun detectarTimeout() {

        runBlocking {

            Log.i(tag, "Iniciando prueba de timeout")

            val inicio = System.nanoTime()

            try {

                api.obtenerRecordatorios(
                    modoPrueba = "timeout"
                )

                fail(
                    "Se esperaba SocketTimeoutException"
                )

            } catch (e: SocketTimeoutException) {

                val duracion =
                    (System.nanoTime() - inicio) / 1_000_000

                Log.i(
                    tag,
                    "Duracion de la solicitud: $duracion ms"
                )

                Log.i(
                    tag,
                    "Excepcion detectada: ${e.javaClass.simpleName}"
                )

                Log.i(
                    tag,
                    "PRUEBA 5 OK: Timeout detectado"
                )
            }
        }
    }
}
 