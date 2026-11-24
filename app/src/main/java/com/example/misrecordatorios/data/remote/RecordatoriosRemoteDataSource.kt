
package com.example.misrecordatorios.data.remote

import com.example.misrecordatorios.data.local.RecordatorioEntity
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

class RecordatoriosRemoteDataSource(

    private val api: RecordatoriosApi = RetrofitClient.api

) {

    // ============================================
    // 1. VERIFICAR DISPONIBILIDAD DEL SERVIDOR
    // ============================================

    suspend fun verificarDisponibilidad(
        modoPrueba: String? = null
    ): ResultadoRemoto<HealthDto> {

        return ejecutarSolicitud(

            solicitud = {

                api.verificarDisponibilidad(
                    modoPrueba = modoPrueba
                )
            },

            transformar = { health ->

                require(health.status == "UP") {
                    "El servidor no se encuentra disponible"
                }

                require(
                    health.service == "MisRecordatorios"
                ) {
                    "El servicio remoto no es el esperado"
                }

                require(
                    !health.version.isNullOrBlank()
                ) {
                    "La version del servicio es invalida"
                }

                health
            }
        )
    }


    // ============================================
    // 2. CONSULTAR TODOS LOS RECORDATORIOS
    // ============================================

    suspend fun obtenerRecordatorios(
        modoPrueba: String? = null
    ): ResultadoRemoto<List<RecordatorioEntity>> {

        return ejecutarSolicitud(

            solicitud = {

                api.obtenerRecordatorios(
                    modoPrueba = modoPrueba
                )
            },

            transformar = { listaDto ->

                // Gson puede recibir elementos nulos
                // aunque el modelo Kotlin no los permita.

                val listaSegura: List<RecordatorioDto?> =
                    listaDto

                val entidades = listaSegura.map { dto ->

                    requireNotNull(dto) {
                        "La lista remota contiene un registro nulo"
                    }.toEntity()
                }

                // Rechazar UUID duplicados en la respuesta.

                require(
                    entidades.map { it.uuid }
                        .distinct().size == entidades.size
                ) {
                    "La respuesta remota contiene UUID duplicados"
                }

                entidades
            }
        )
    }


    // ============================================
    // 3. CONSULTAR RECORDATORIO POR UUID
    // ============================================

    suspend fun obtenerPorUuid(

        uuid: String,

        modoPrueba: String? = null

    ): ResultadoRemoto<RecordatorioEntity> {

        return ejecutarSolicitud(

            solicitud = {

                api.obtenerPorUuid(
                    uuid = uuid,
                    modoPrueba = modoPrueba
                )
            },

            transformar = { dto ->

                val entity = dto.toEntity()

                require(entity.uuid == uuid) {
                    "El UUID recibido no coincide con el solicitado"
                }

                entity
            }
        )
    }


    // ============================================
    // 4. CREAR O ACTUALIZAR RECORDATORIO
    // ============================================

    suspend fun guardarRecordatorio(

        recordatorio: RecordatorioEntity,

        modoPrueba: String? = null

    ): ResultadoRemoto<RecordatorioEntity> {

        val dtoEnviado = recordatorio.toDto()

        return ejecutarSolicitud(

            solicitud = {

                api.guardarRecordatorio(

                    uuid = recordatorio.uuid,

                    recordatorio = dtoEnviado,

                    modoPrueba = modoPrueba
                )
            },

            transformar = { dtoRecibido ->

                val entityRecibida =
                    dtoRecibido.toEntity(
                        idLocal = recordatorio.id
                    )

                // Verificar identidad del registro.

                require(
                    entityRecibida.uuid == recordatorio.uuid
                ) {
                    "El UUID recibido no coincide con el enviado"
                }

                // Verificar revision confirmada.

                require(
                    entityRecibida.revision ==
                            recordatorio.revision
                ) {
                    "La revision recibida no coincide con la enviada"
                }

                // Verificar todos los campos remotos.

                require(
                    entityRecibida.toDto() == dtoEnviado
                ) {
                    "Los datos recibidos no coinciden con los enviados"
                }

                entityRecibida
            }
        )
    }


    // ============================================
    // 5. SINCRONIZAR ELIMINACION LOGICA
    // ============================================

    suspend fun eliminarRecordatorio(

        recordatorio: RecordatorioEntity,

        modoPrueba: String? = null

    ): ResultadoRemoto<RecordatorioEntity> {

        if (!recordatorio.eliminado) {

            return ResultadoRemoto.Error.DatosInvalidos(

                mensaje =
                    "El recordatorio debe estar eliminado localmente."
            )
        }

        // La eliminacion se envia mediante PUT.
        // Se conserva el UUID y la revision local.

        return guardarRecordatorio(

            recordatorio = recordatorio,

            modoPrueba = modoPrueba
        )
    }


    // ============================================
    // 6. EJECUTAR SOLICITUD REMOTA GENERICA
    // ============================================

    private suspend fun <T, R> ejecutarSolicitud(

        solicitud: suspend () -> Response<T>,

        transformar: (T) -> R

    ): ResultadoRemoto<R> {

        return try {

            currentCoroutineContext().ensureActive()

            val respuesta = solicitud()

            val codigoHttp = respuesta.code()

            if (!respuesta.isSuccessful) {

                // Liberar el cuerpo de la respuesta de error.

                respuesta.errorBody()?.close()

                clasificarErrorHttp(codigoHttp)

            } else {

                val cuerpo = respuesta.body()

                if (cuerpo == null) {

                    ResultadoRemoto.Error.RespuestaInesperada(

                        codigoHttp = codigoHttp,

                        mensaje =
                            "La respuesta exitosa no contiene datos."
                    )

                } else {

                    val datosValidados =
                        transformar(cuerpo)

                    ResultadoRemoto.Exito(

                        datos = datosValidados,

                        codigoHttp = codigoHttp
                    )
                }
            }

        } catch (e: CancellationException) {

            // No convertir cancelaciones en errores de red.

            throw e

        } catch (e: SocketTimeoutException) {

            currentCoroutineContext().ensureActive()

            ResultadoRemoto.Error.Timeout()

        } catch (e: InterruptedIOException) {

            currentCoroutineContext().ensureActive()

            if (
                e.message?.contains(
                    "timeout",
                    ignoreCase = true
                ) == true
            ) {

                ResultadoRemoto.Error.Timeout()

            } else {

                ResultadoRemoto.Error.ErrorConexion()
            }

        } catch (e: MalformedJsonException) {

            currentCoroutineContext().ensureActive()

            ResultadoRemoto.Error.DatosInvalidos(

                mensaje =
                    "El servidor devolvio un JSON invalido."
            )

        } catch (e: JsonParseException) {

            currentCoroutineContext().ensureActive()

            ResultadoRemoto.Error.DatosInvalidos(

                mensaje =
                    "No se pudo interpretar la respuesta JSON."
            )

        } catch (e: IllegalArgumentException) {

            currentCoroutineContext().ensureActive()

            ResultadoRemoto.Error.DatosInvalidos(

                mensaje = e.message
                    ?: "Los datos remotos son invalidos."
            )

        } catch (e: IOException) {

            currentCoroutineContext().ensureActive()

            ResultadoRemoto.Error.ErrorConexion()

        } catch (e: Exception) {

            currentCoroutineContext().ensureActive()

            ResultadoRemoto.Error.ErrorDesconocido()
        }
    }


    // ============================================
    // 7. CLASIFICAR ERRORES HTTP
    // ============================================

    private fun clasificarErrorHttp(

        codigoHttp: Int

    ): ResultadoRemoto.Error {

        return when (codigoHttp) {

            in 400..499 -> {

                val mensaje = when (codigoHttp) {

                    400 ->
                        "La solicitud contiene datos invalidos."

                    401 ->
                        "La solicitud no esta autorizada."

                    403 ->
                        "El acceso al recurso fue denegado."

                    404 ->
                        "El recordatorio remoto no fue encontrado."

                    408 ->
                        "El servidor agoto el tiempo de espera."

                    409 ->
                        "Existe un conflicto de revision remoto."

                    429 ->
                        "Se realizaron demasiadas solicitudes."

                    else ->
                        "Error de cliente HTTP $codigoHttp."
                }

                ResultadoRemoto.Error.ErrorCliente(

                    codigoHttp = codigoHttp,

                    mensaje = mensaje
                )
            }

            in 500..599 -> {

                ResultadoRemoto.Error.ErrorServidor(

                    codigoHttp = codigoHttp,

                    mensaje =
                        "Error del servidor HTTP $codigoHttp."
                )
            }

            else -> {

                ResultadoRemoto.Error.RespuestaInesperada(

                    codigoHttp = codigoHttp,

                    mensaje =
                        "Codigo HTTP inesperado: $codigoHttp."
                )
            }
        }
    }
}
 