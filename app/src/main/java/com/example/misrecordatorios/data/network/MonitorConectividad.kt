
package com.example.misrecordatorios.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MonitorConectividad(context: Context) {

    companion object {

        private const val TAG = "MonitorConectividad"
    }

    // Utilizar el contexto de la aplicacion para
    // evitar referencias innecesarias a una Activity.

    private val connectivityManager =
        context.applicationContext.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

    // Control de acceso concurrente.

    private val bloqueo = Any()

    // Indica si el callback se encuentra registrado.

    private var monitoreoActivo = false

    // ============================================
    // ESTADO DE CONECTIVIDAD
    // ============================================

    private val _conectado =
        MutableStateFlow(verificarConexionActual())

    val conectado: StateFlow<Boolean> =
        _conectado.asStateFlow()

    // ============================================
    // CALLBACK DE CONECTIVIDAD
    // ============================================

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {

                actualizarEstadoDesdeCallback()
            }

            override fun onLost(network: Network) {

                actualizarEstadoDesdeCallback()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {

                actualizarEstadoDesdeCallback()
            }
        }

    // ============================================
    // INICIAR MONITOREO
    // ============================================

    fun iniciarMonitoreo() {

        synchronized(bloqueo) {

            if (monitoreoActivo) {

                Log.d(
                    TAG,
                    "El monitoreo ya se encuentra activo"
                )

                return
            }

            try {

                connectivityManager
                    .registerDefaultNetworkCallback(
                        networkCallback
                    )

                monitoreoActivo = true

                actualizarEstadoConexion()

                Log.i(
                    TAG,
                    "Monitoreo de conectividad iniciado"
                )

            } catch (e: SecurityException) {

                Log.e(
                    TAG,
                    "Permisos insuficientes para monitorear la red",
                    e
                )

                _conectado.value = false

            } catch (e: RuntimeException) {

                Log.e(
                    TAG,
                    "No se pudo iniciar el monitoreo",
                    e
                )

                _conectado.value = false
            }
        }
    }

    // ============================================
    // DETENER MONITOREO
    // ============================================

    fun detenerMonitoreo() {

        synchronized(bloqueo) {

            if (!monitoreoActivo) {

                return
            }

            monitoreoActivo = false

            try {

                connectivityManager
                    .unregisterNetworkCallback(
                        networkCallback
                    )

                Log.i(
                    TAG,
                    "Monitoreo de conectividad detenido"
                )

            } catch (e: IllegalArgumentException) {

                Log.w(
                    TAG,
                    "El callback ya no estaba registrado",
                    e
                )

            } catch (e: RuntimeException) {

                Log.e(
                    TAG,
                    "Error al detener el monitoreo",
                    e
                )
            }
        }
    }

    // ============================================
    // VERIFICAR CONEXION ACTUAL
    // ============================================

    fun verificarConexionActual(): Boolean {

        return try {

            val redActiva =
                connectivityManager.activeNetwork
                    ?: return false

            val capacidades =
                connectivityManager.getNetworkCapabilities(
                    redActiva
                ) ?: return false

            capacidades.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "No se pudo consultar el estado de red",
                e
            )

            false
        }
    }

    // ============================================
    // ACTUALIZAR ESTADO DESDE CALLBACK
    // ============================================

    private fun actualizarEstadoDesdeCallback() {

        synchronized(bloqueo) {

            if (monitoreoActivo) {

                actualizarEstadoConexion()
            }
        }
    }

    // ============================================
    // ACTUALIZAR ESTADO DE CONECTIVIDAD
    // ============================================

    private fun actualizarEstadoConexion() {

        val nuevoEstado =
            verificarConexionActual()

        val estadoAnterior =
            _conectado.value

        _conectado.value = nuevoEstado

        if (estadoAnterior != nuevoEstado) {

            if (nuevoEstado) {

                Log.i(
                    TAG,
                    "Conexion de red recuperada"
                )

            } else {

                Log.w(
                    TAG,
                    "Conexion de red perdida"
                )
            }
        }
    }

    // ============================================
    // CONSULTAR ESTADO DEL MONITOREO
    // ============================================

    fun estaMonitoreando(): Boolean {

        return synchronized(bloqueo) {

            monitoreoActivo
        }
    }
}
 