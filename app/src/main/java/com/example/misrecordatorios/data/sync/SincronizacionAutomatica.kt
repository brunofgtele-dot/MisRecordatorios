
package com.example.misrecordatorios.data.sync

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SincronizacionAutomatica(
    private val scope: CoroutineScope,
    private val conectado: StateFlow<Boolean>,
    private val cantidadPendientes: StateFlow<Int>,
    private val sincronizando: StateFlow<Boolean>,
    private val ejecutarSincronizacion:
    suspend () -> ResultadoSincronizacion
) {

    companion object {

        private const val TAG = "SincronizacionAuto"

        private const val MAX_INTENTOS = 3

        private const val ESPERA_INICIAL = 1000L

        private const val ESPERA_REINTENTO = 5000L
    }

    private var monitoreoJob: Job? = null

    private var procesamientoJob: Job? = null

    private var canalSolicitudes =
        Channel<Unit>(Channel.CONFLATED)

    // ============================================
    // 1. INICIAR SINCRONIZACION AUTOMATICA
    // ============================================

    fun iniciar() {

        if (estaActiva()) {

            Log.d(
                TAG,
                "Sincronizacion automatica ya activa"
            )

            return
        }

        canalSolicitudes.close()

        val canal =
            Channel<Unit>(Channel.CONFLATED)

        canalSolicitudes = canal

        procesamientoJob = scope.launch {

            for (solicitud in canal) {

                if (!conectado.value) {

                    continue
                }

                delay(ESPERA_INICIAL)

                sincronizarConReintentos()
            }
        }

        monitoreoJob = scope.launch {

            var conexionAnterior: Boolean? = null

            var pendientesAnteriores = 0

            combine(
                conectado,
                cantidadPendientes
            ) { hayConexion, pendientes ->

                Pair(hayConexion, pendientes)

            }.collect { (hayConexion, pendientes) ->

                val conexionRecuperada =
                    hayConexion &&
                            conexionAnterior != true

                val nuevosPendientes =
                    pendientes > pendientesAnteriores

                if (!hayConexion) {

                    Log.d(
                        TAG,
                        "Sin conexion. Esperando recuperacion de red"
                    )

                } else if (
                    pendientes > 0 &&
                    (
                            conexionRecuperada ||
                                    nuevosPendientes
                            )
                ) {

                    Log.i(
                        TAG,
                        "Detectados $pendientes registros pendientes"
                    )

                    canal.trySend(Unit)
                }

                conexionAnterior = hayConexion

                pendientesAnteriores = pendientes
            }
        }

        Log.i(
            TAG,
            "Sincronizacion automatica iniciada"
        )
    }

    // ============================================
    // 2. NOTIFICAR CAMBIOS LOCALES
    // ============================================

    fun notificarCambioLocal() {

        if (!estaActiva()) {

            return
        }

        Log.d(
            TAG,
            "Cambio local detectado"
        )

        canalSolicitudes.trySend(Unit)
    }

    // ============================================
    // 3. SINCRONIZAR CON REINTENTOS CONTROLADOS
    // ============================================

    private suspend fun sincronizarConReintentos() {

        var intento = 1

        while (intento <= MAX_INTENTOS) {

            if (!conectado.value) {

                Log.w(
                    TAG,
                    "Sincronizacion suspendida por perdida de red"
                )

                return
            }

            if (sincronizando.value) {

                Log.d(
                    TAG,
                    "Esperando sincronizacion en ejecucion"
                )

                sincronizando.first { activa ->

                    !activa
                }
            }

            if (!conectado.value) {

                return
            }

            if (cantidadPendientes.value <= 0) {

                Log.d(
                    TAG,
                    "No existen registros pendientes"
                )

                return
            }

            Log.i(
                TAG,
                "Intento automatico $intento de $MAX_INTENTOS"
            )

            val resultado = try {

                ejecutarSincronizacion()

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Error inesperado durante la sincronizacion",
                    e
                )

                return
            }

            val requiereReintento =
                when (resultado) {

                    is ResultadoSincronizacion.Exito -> {

                        Log.i(
                            TAG,
                            "Sincronizacion automatica completada"
                        )

                        resultado.resumen
                            .modificadosDuranteSincronizacion > 0
                    }

                    is ResultadoSincronizacion.SinCambios -> {

                        Log.i(
                            TAG,
                            "Sincronizacion completada sin cambios"
                        )

                        false
                    }

                    is ResultadoSincronizacion.Parcial -> {

                        Log.w(
                            TAG,
                            "Sincronizacion parcial: ${resultado.mensaje}"
                        )

                        resultado.resumen.errores.any {

                            it.reintentable

                        } || resultado.resumen
                            .modificadosDuranteSincronizacion > 0
                    }

                    is ResultadoSincronizacion.Error -> {

                        Log.w(
                            TAG,
                            "Error de sincronizacion: ${resultado.mensaje}"
                        )

                        resultado.reintentable
                    }
                }

            if (!requiereReintento) {

                return
            }

            if (intento >= MAX_INTENTOS) {

                Log.w(
                    TAG,
                    "Limite de intentos alcanzado"
                )

                return
            }

            if (!conectado.value) {

                return
            }

            val espera =
                ESPERA_REINTENTO * intento

            Log.i(
                TAG,
                "Proximo intento en ${espera / 1000} segundos"
            )

            delay(espera)

            intento++
        }
    }

    // ============================================
    // 4. DETENER SINCRONIZACION AUTOMATICA
    // ============================================

    fun detener() {

        monitoreoJob?.cancel()

        procesamientoJob?.cancel()

        canalSolicitudes.close()

        monitoreoJob = null

        procesamientoJob = null

        Log.i(
            TAG,
            "Sincronizacion automatica detenida"
        )
    }

    // ============================================
    // 5. CONSULTAR ESTADO
    // ============================================

    fun estaActiva(): Boolean {

        return monitoreoJob?.isActive == true &&
                procesamientoJob?.isActive == true
    }
}
 