
package com.example.misrecordatorios.data.remote


// ================================================
// 1. RESULTADO GENERICO DE UNA OPERACION REMOTA
// ================================================

sealed interface ResultadoRemoto<out T> {


    // ============================================
    // 2. OPERACION EXITOSA
    // ============================================

    data class Exito<T>(

        val datos: T,

        val codigoHttp: Int

    ) : ResultadoRemoto<T>


    // ============================================
    // 3. RESULTADOS DE ERROR
    // ============================================

    sealed interface Error : ResultadoRemoto<Nothing> {

        val mensaje: String

        val reintentable: Boolean


        // ----------------------------------------
        // ERROR HTTP 4XX
        // ----------------------------------------

        data class ErrorCliente(

            val codigoHttp: Int,

            override val mensaje: String =
                "La solicitud fue rechazada por el servidor."

        ) : Error {

            override val reintentable: Boolean
                get() = codigoHttp == 408 ||
                        codigoHttp == 429
        }


        // ----------------------------------------
        // ERROR HTTP 5XX
        // ----------------------------------------

        data class ErrorServidor(

            val codigoHttp: Int,

            override val mensaje: String =
                "El servidor no pudo procesar la solicitud."

        ) : Error {

            override val reintentable: Boolean = true
        }


        // ----------------------------------------
        // TIEMPO DE ESPERA AGOTADO
        // ----------------------------------------

        data class Timeout(

            override val mensaje: String =
                "Se agoto el tiempo de espera del servidor."

        ) : Error {

            override val reintentable: Boolean = true
        }


        // ----------------------------------------
        // ERROR DE CONEXION
        // ----------------------------------------

        data class ErrorConexion(

            override val mensaje: String =
                "No se pudo establecer o mantener la conexion."

        ) : Error {

            override val reintentable: Boolean = true
        }


        // ----------------------------------------
        // DATOS REMOTOS INVALIDOS
        // ----------------------------------------

        data class DatosInvalidos(

            override val mensaje: String =
                "El servidor devolvio datos invalidos."

        ) : Error {

            override val reintentable: Boolean = false
        }


        // ----------------------------------------
        // RESPUESTA INESPERADA
        // ----------------------------------------

        data class RespuestaInesperada(

            val codigoHttp: Int? = null,

            override val mensaje: String =
                "Se recibio una respuesta inesperada."

        ) : Error {

            override val reintentable: Boolean = false
        }


        // ----------------------------------------
        // ERROR DESCONOCIDO
        // ----------------------------------------

        data class ErrorDesconocido(

            override val mensaje: String =
                "Ocurrio un error inesperado."

        ) : Error {

            override val reintentable: Boolean = false
        }
    }
}


// ================================================
// 4. VERIFICAR SI EL RESULTADO ES EXITOSO
// ================================================

fun ResultadoRemoto<*>.esExitoso(): Boolean {

    return this is ResultadoRemoto.Exito<*>
}


// ================================================
// 5. VERIFICAR SI EL RESULTADO ES UN ERROR
// ================================================

fun ResultadoRemoto<*>.esError(): Boolean {

    return this is ResultadoRemoto.Error
}


// ================================================
// 6. VERIFICAR SI SE PUEDE REINTENTAR
// ================================================

fun ResultadoRemoto<*>.puedeReintentarse(): Boolean {

    return when (this) {

        is ResultadoRemoto.Exito -> false

        is ResultadoRemoto.Error -> reintentable
    }
}


// ================================================
// 7. OBTENER MENSAJE DE ERROR
// ================================================

fun ResultadoRemoto<*>.obtenerMensajeError(): String? {

    return when (this) {

        is ResultadoRemoto.Exito -> null

        is ResultadoRemoto.Error -> mensaje
    }
}


// ================================================
// 8. OBTENER DATOS DE UNA RESPUESTA EXITOSA
// ================================================

fun <T> ResultadoRemoto<T>.obtenerDatosONull(): T? {

    return when (this) {

        is ResultadoRemoto.Exito -> datos

        is ResultadoRemoto.Error -> null
    }
}


// ================================================
// 9. OBTENER CODIGO HTTP
// ================================================

fun ResultadoRemoto<*>.obtenerCodigoHttp(): Int? {

    return when (this) {

        is ResultadoRemoto.Exito ->
            codigoHttp

        is ResultadoRemoto.Error.ErrorCliente ->
            codigoHttp

        is ResultadoRemoto.Error.ErrorServidor ->
            codigoHttp

        is ResultadoRemoto.Error.RespuestaInesperada ->
            codigoHttp

        else -> null
    }
}
 