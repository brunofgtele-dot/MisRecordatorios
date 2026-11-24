
package com.example.misrecordatorios.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ResultadoRemotoTest {

    // ============================================
    // 1. PRUEBAS DE RESULTADOS EXITOSOS
    // ============================================

    @Test
    fun reconocerResultadoExitosoHttp200() {

        val resultado = ResultadoRemoto.Exito(
            datos = "Consulta exitosa",
            codigoHttp = 200
        )

        assertTrue(resultado.esExitoso())
        assertFalse(resultado.esError())
        assertFalse(resultado.puedeReintentarse())

        assertEquals(
            200,
            resultado.obtenerCodigoHttp()
        )

        assertNull(resultado.obtenerMensajeError())
    }


    @Test
    fun reconocerResultadoExitosoHttp201() {

        val resultado = ResultadoRemoto.Exito(
            datos = "Recordatorio creado",
            codigoHttp = 201
        )

        assertTrue(resultado.esExitoso())
        assertFalse(resultado.esError())

        assertEquals(
            201,
            resultado.obtenerCodigoHttp()
        )

        assertEquals(
            "Recordatorio creado",
            resultado.obtenerDatosONull()
        )
    }


    @Test
    fun recuperarDatosExitosos() {

        val resultado = ResultadoRemoto.Exito(
            datos = "Estudiar Kotlin y Room",
            codigoHttp = 200
        )

        assertEquals(
            "Estudiar Kotlin y Room",
            resultado.obtenerDatosONull()
        )
    }


    @Test
    fun recuperarListaDeDatosExitosa() {

        val recordatorios = listOf(
            "Comprar medicamentos",
            "Estudiar Kotlin y Room",
            "Pagar electricidad"
        )

        val resultado = ResultadoRemoto.Exito(
            datos = recordatorios,
            codigoHttp = 200
        )

        val datos = requireNotNull(
            resultado.obtenerDatosONull()
        )

        assertEquals(3, datos.size)
        assertEquals(recordatorios, datos)

        assertTrue(resultado.esExitoso())
    }


    @Test
    fun aceptarListaVaciaExitosa() {

        val resultado = ResultadoRemoto.Exito(
            datos = emptyList<String>(),
            codigoHttp = 200
        )

        assertTrue(resultado.esExitoso())

        val datos = requireNotNull(
            resultado.obtenerDatosONull()
        )

        assertTrue(datos.isEmpty())
        assertNull(resultado.obtenerMensajeError())
    }


    // ============================================
    // 2. PRUEBAS DE ERRORES HTTP 4XX
    // ============================================

    @Test
    fun reconocerErrorClienteHttp400() {

        val resultado =
            ResultadoRemoto.Error.ErrorCliente(
                codigoHttp = 400
            )

        assertTrue(resultado.esError())
        assertFalse(resultado.esExitoso())
        assertFalse(resultado.puedeReintentarse())

        assertEquals(
            400,
            resultado.obtenerCodigoHttp()
        )

        assertNull(resultado.obtenerDatosONull())
    }


    @Test
    fun reconocerErrorClienteHttp404() {

        val resultado =
            ResultadoRemoto.Error.ErrorCliente(
                codigoHttp = 404
            )

        assertTrue(resultado.esError())
        assertFalse(resultado.puedeReintentarse())

        assertEquals(
            404,
            resultado.obtenerCodigoHttp()
        )
    }


    @Test
    fun reconocerConflictoHttp409() {

        val resultado =
            ResultadoRemoto.Error.ErrorCliente(
                codigoHttp = 409
            )

        assertTrue(resultado.esError())
        assertFalse(resultado.puedeReintentarse())

        assertEquals(
            409,
            resultado.obtenerCodigoHttp()
        )
    }


    @Test
    fun permitirReintentoHttp408() {

        val resultado =
            ResultadoRemoto.Error.ErrorCliente(
                codigoHttp = 408
            )

        assertTrue(resultado.esError())
        assertTrue(resultado.puedeReintentarse())

        assertEquals(
            408,
            resultado.obtenerCodigoHttp()
        )
    }


    @Test
    fun permitirReintentoHttp429() {

        val resultado =
            ResultadoRemoto.Error.ErrorCliente(
                codigoHttp = 429
            )

        assertTrue(resultado.esError())
        assertTrue(resultado.puedeReintentarse())

        assertEquals(
            429,
            resultado.obtenerCodigoHttp()
        )
    }


    // ============================================
    // 3. PRUEBAS DE ERRORES HTTP 5XX
    // ============================================

    @Test
    fun reconocerErrorServidorHttp500() {

        val resultado =
            ResultadoRemoto.Error.ErrorServidor(
                codigoHttp = 500
            )

        assertTrue(resultado.esError())
        assertFalse(resultado.esExitoso())
        assertTrue(resultado.puedeReintentarse())

        assertEquals(
            500,
            resultado.obtenerCodigoHttp()
        )

        assertNull(resultado.obtenerDatosONull())
    }


    @Test
    fun reconocerServidorNoDisponibleHttp503() {

        val resultado =
            ResultadoRemoto.Error.ErrorServidor(
                codigoHttp = 503
            )

        assertTrue(resultado.esError())
        assertTrue(resultado.puedeReintentarse())

        assertEquals(
            503,
            resultado.obtenerCodigoHttp()
        )
    }


    // ============================================
    // 4. PRUEBAS DE TIMEOUT Y CONEXION
    // ============================================

    @Test
    fun reconocerTimeout() {

        val resultado =
            ResultadoRemoto.Error.Timeout()

        assertTrue(resultado.esError())
        assertFalse(resultado.esExitoso())
        assertTrue(resultado.puedeReintentarse())

        assertNull(resultado.obtenerCodigoHttp())
        assertNull(resultado.obtenerDatosONull())

        assertEquals(
            "Se agoto el tiempo de espera del servidor.",
            resultado.obtenerMensajeError()
        )
    }


    @Test
    fun reconocerErrorConexion() {

        val resultado =
            ResultadoRemoto.Error.ErrorConexion()

        assertTrue(resultado.esError())
        assertFalse(resultado.esExitoso())
        assertTrue(resultado.puedeReintentarse())

        assertNull(resultado.obtenerCodigoHttp())
        assertNull(resultado.obtenerDatosONull())

        assertEquals(
            "No se pudo establecer o mantener la conexion.",
            resultado.obtenerMensajeError()
        )
    }


    // ============================================
    // 5. PRUEBAS DE RESPUESTAS INVALIDAS
    // ============================================

    @Test
    fun reconocerDatosInvalidos() {

        val resultado =
            ResultadoRemoto.Error.DatosInvalidos()

        assertTrue(resultado.esError())
        assertFalse(resultado.esExitoso())
        assertFalse(resultado.puedeReintentarse())

        assertNull(resultado.obtenerCodigoHttp())
        assertNull(resultado.obtenerDatosONull())

        assertEquals(
            "El servidor devolvio datos invalidos.",
            resultado.obtenerMensajeError()
        )
    }


    @Test
    fun reconocerRespuestaInesperadaConCodigoHttp() {

        val resultado =
            ResultadoRemoto.Error.RespuestaInesperada(
                codigoHttp = 200
            )

        assertTrue(resultado.esError())
        assertFalse(resultado.esExitoso())
        assertFalse(resultado.puedeReintentarse())

        assertEquals(
            200,
            resultado.obtenerCodigoHttp()
        )

        assertNull(resultado.obtenerDatosONull())
    }


    @Test
    fun reconocerRespuestaInesperadaSinCodigoHttp() {

        val resultado =
            ResultadoRemoto.Error.RespuestaInesperada()

        assertTrue(resultado.esError())
        assertFalse(resultado.puedeReintentarse())

        assertNull(resultado.obtenerCodigoHttp())

        assertEquals(
            "Se recibio una respuesta inesperada.",
            resultado.obtenerMensajeError()
        )
    }


    @Test
    fun reconocerErrorDesconocido() {

        val resultado =
            ResultadoRemoto.Error.ErrorDesconocido()

        assertTrue(resultado.esError())
        assertFalse(resultado.esExitoso())
        assertFalse(resultado.puedeReintentarse())

        assertNull(resultado.obtenerCodigoHttp())
        assertNull(resultado.obtenerDatosONull())

        assertEquals(
            "Ocurrio un error inesperado.",
            resultado.obtenerMensajeError()
        )
    }


    // ============================================
    // 6. PRUEBAS DE MENSAJES Y TIPOS GENERICOS
    // ============================================

    @Test
    fun conservarMensajePersonalizado() {

        val mensajePersonalizado =
            "El UUID recibido no coincide con el enviado."

        val resultado =
            ResultadoRemoto.Error.DatosInvalidos(
                mensaje = mensajePersonalizado
            )

        assertEquals(
            mensajePersonalizado,
            resultado.obtenerMensajeError()
        )

        assertFalse(resultado.puedeReintentarse())
    }


    @Test
    fun procesarResultadosGenericosConWhen() {

        val exito: ResultadoRemoto<String> =
            ResultadoRemoto.Exito(
                datos = "Registro sincronizado",
                codigoHttp = 200
            )

        val error: ResultadoRemoto<String> =
            ResultadoRemoto.Error.ErrorConexion()

        fun procesar(
            resultado: ResultadoRemoto<String>
        ): String {

            return when (resultado) {

                is ResultadoRemoto.Exito ->
                    resultado.datos

                is ResultadoRemoto.Error ->
                    resultado.mensaje
            }
        }

        assertEquals(
            "Registro sincronizado",
            procesar(exito)
        )

        assertEquals(
            "No se pudo establecer o mantener la conexion.",
            procesar(error)
        )
    }
}
 