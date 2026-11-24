
package com.example.misrecordatorios.data.sync

import com.example.misrecordatorios.data.remote.ResultadoRemoto
import com.example.misrecordatorios.data.remote.obtenerCodigoHttp


// ================================================
// 1. ERROR INDIVIDUAL DE SINCRONIZACION
// ================================================

data class ErrorSincronizacion(

    val uuid: String,

    val revision: Long,

    val mensaje: String,

    val codigoHttp: Int? = null,

    val reintentable: Boolean = false

) {

    init {

        require(uuid.isNotBlank()) {
            "El UUID no puede estar vacio"
        }

        require(revision >= 1L) {
            "La revision debe ser mayor o igual a 1"
        }

        require(mensaje.isNotBlank()) {
            "El mensaje de error no puede estar vacio"
        }
    }


    // ============================================
    // CONVERTIR ERROR REMOTO EN ERROR DE SYNC
    // ============================================

    companion object {

        fun desdeErrorRemoto(

            uuid: String,

            revision: Long,

            error: ResultadoRemoto.Error

        ): ErrorSincronizacion {

            return ErrorSincronizacion(

                uuid = uuid,

                revision = revision,

                mensaje = error.mensaje,

                codigoHttp =
                    error.obtenerCodigoHttp(),

                reintentable =
                    error.reintentable
            )
        }
    }
}


// ================================================
// 2. RESUMEN GLOBAL DE SINCRONIZACION
// ================================================

data class ResumenSincronizacion(

    val pendientesIniciales: Int = 0,

    val procesados: Int = 0,

    val sincronizados: Int = 0,

    val modificadosDuranteSincronizacion: Int = 0,

    val remotosInsertados: Int = 0,

    val remotosActualizados: Int = 0,

    val remotosOmitidos: Int = 0,

    val errores: List<ErrorSincronizacion> =
        emptyList(),

    val duracionMillis: Long = 0L

) {

    // ============================================
    // VALIDAR CONSISTENCIA DEL RESUMEN
    // ============================================

    init {

        require(pendientesIniciales >= 0) {
            "Los pendientes iniciales no pueden ser negativos"
        }

        require(procesados >= 0) {
            "Los procesados no pueden ser negativos"
        }

        require(sincronizados >= 0) {
            "Los sincronizados no pueden ser negativos"
        }

        require(
            modificadosDuranteSincronizacion >= 0
        ) {
            "Los modificados no pueden ser negativos"
        }

        require(remotosInsertados >= 0) {
            "Los insertados remotos no pueden ser negativos"
        }

        require(remotosActualizados >= 0) {
            "Los actualizados remotos no pueden ser negativos"
        }

        require(remotosOmitidos >= 0) {
            "Los omitidos remotos no pueden ser negativos"
        }

        require(duracionMillis >= 0L) {
            "La duracion no puede ser negativa"
        }

        require(procesados <= pendientesIniciales) {
            "Los procesados no pueden superar los pendientes iniciales"
        }

        require(
            procesados ==
                    sincronizados +
                    modificadosDuranteSincronizacion +
                    errores.size
        ) {
            "Los contadores de sincronizacion no son consistentes"
        }
    }


    // ============================================
    // CANTIDAD TOTAL DE ERRORES INDIVIDUALES
    // ============================================

    val cantidadErrores: Int

        get() = errores.size


    // ============================================
    // ERRORES QUE ADMITEN REINTENTO
    // ============================================

    val erroresReintentables: Int

        get() = errores.count {

            it.reintentable
        }


    // ============================================
    // ERRORES QUE REQUIEREN REVISION
    // ============================================

    val erroresNoReintentables: Int

        get() = errores.count {

            !it.reintentable
        }


    // ============================================
    // REGISTROS INICIALES NO PROCESADOS
    // ============================================

    val noProcesados: Int

        get() = pendientesIniciales - procesados


    // ============================================
    // TOTAL DE CAMBIOS REMOTOS APLICADOS EN ROOM
    // ============================================

    val cambiosRemotosAplicados: Int

        get() =
            remotosInsertados + remotosActualizados


    // ============================================
    // PORCENTAJE DE ENVIOS CONFIRMADOS
    // ============================================

    val porcentajeExito: Double?

        get() {

            if (procesados == 0) {

                return null
            }

            return sincronizados.toDouble() /
                    procesados.toDouble() * 100.0
        }


    // ============================================
    // DURACION EXPRESADA EN SEGUNDOS
    // ============================================

    val duracionSegundos: Double

        get() = duracionMillis / 1000.0


    // ============================================
    // VERIFICAR SI EXISTEN ERRORES
    // ============================================

    fun tieneErrores(): Boolean {

        return errores.isNotEmpty()
    }


    // ============================================
    // VERIFICAR SI EXISTEN REINTENTOS POSIBLES
    // ============================================

    fun tieneErroresReintentables(): Boolean {

        return errores.any {

            it.reintentable
        }
    }


    // ============================================
    // VERIFICAR SI EXISTEN ENVIOS INCOMPLETOS
    // ============================================

    fun tieneEnviosIncompletos(): Boolean {

        return noProcesados > 0 ||
                cantidadErrores > 0 ||
                modificadosDuranteSincronizacion > 0
    }
}


// ================================================
// 3. RESULTADO GLOBAL DE SINCRONIZACION
// ================================================

sealed interface ResultadoSincronizacion {

    val resumen: ResumenSincronizacion


    // ============================================
    // SINCRONIZACION COMPLETADA
    // ============================================

    data class Exito(

        override val resumen: ResumenSincronizacion

    ) : ResultadoSincronizacion {

        init {

            require(
                !resumen.tieneEnviosIncompletos()
            ) {
                "Una sincronizacion exitosa no puede tener envios incompletos"
            }
        }
    }


    // ============================================
    // SINCRONIZACION PARCIAL O INCOMPLETA
    // ============================================

    data class Parcial(

        override val resumen: ResumenSincronizacion,

        val mensaje: String =
            "Existen registros pendientes de sincronizacion."

    ) : ResultadoSincronizacion {

        init {

            require(mensaje.isNotBlank()) {
                "El mensaje no puede estar vacio"
            }

            require(
                resumen.tieneEnviosIncompletos()
            ) {
                "Una sincronizacion parcial debe tener envios incompletos"
            }
        }
    }


    // ============================================
    // SIN CAMBIOS QUE SINCRONIZAR
    // ============================================

    data class SinCambios(

        override val resumen: ResumenSincronizacion =
            ResumenSincronizacion()

    ) : ResultadoSincronizacion {

        init {

            require(
                resumen.pendientesIniciales == 0
            ) {
                "No pueden existir pendientes iniciales"
            }

            require(
                resumen.cambiosRemotosAplicados == 0
            ) {
                "No pueden existir cambios remotos aplicados"
            }
        }
    }


    // ============================================
    // ERROR GENERAL DE SINCRONIZACION
    // ============================================

    data class Error(

        override val resumen: ResumenSincronizacion,

        val mensaje: String,

        val reintentable: Boolean,

        val codigoHttp: Int? = null

    ) : ResultadoSincronizacion {

        init {

            require(mensaje.isNotBlank()) {
                "El mensaje de error no puede estar vacio"
            }
        }
    }
}


// ================================================
// 4. FUNCIONES AUXILIARES
// ================================================

fun ResultadoSincronizacion.esExitoso(): Boolean {

    return this is ResultadoSincronizacion.Exito ||
            this is ResultadoSincronizacion.SinCambios
}


fun ResultadoSincronizacion.esParcial(): Boolean {

    return this is ResultadoSincronizacion.Parcial
}


fun ResultadoSincronizacion.esError(): Boolean {

    return this is ResultadoSincronizacion.Error
}


fun ResultadoSincronizacion.puedeReintentarse(): Boolean {

    return when (this) {

        is ResultadoSincronizacion.Exito ->
            false

        is ResultadoSincronizacion.SinCambios ->
            false

        is ResultadoSincronizacion.Parcial ->

            resumen.tieneErroresReintentables() ||
                    resumen.noProcesados > 0 ||
                    resumen.modificadosDuranteSincronizacion > 0

        is ResultadoSincronizacion.Error ->
            reintentable
    }
}


fun ResultadoSincronizacion.obtenerMensaje(): String {

    return when (this) {

        is ResultadoSincronizacion.Exito ->

            "Sincronizacion completada correctamente."

        is ResultadoSincronizacion.SinCambios ->

            "No existen cambios que sincronizar."

        is ResultadoSincronizacion.Parcial ->

            mensaje

        is ResultadoSincronizacion.Error ->

            mensaje
    }
}
 