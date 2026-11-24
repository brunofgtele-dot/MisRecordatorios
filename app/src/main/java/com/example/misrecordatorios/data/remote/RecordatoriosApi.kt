
package com.example.misrecordatorios.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// ============================================
// 1. DTO DE DISPONIBILIDAD DEL SERVIDOR
// ============================================

data class HealthDto(

    @SerializedName("status")
    val status: String?,

    @SerializedName("service")
    val service: String?,

    @SerializedName("version")
    val version: String?
)

// ============================================
// 2. INTERFAZ RETROFIT
// ============================================

interface RecordatoriosApi {

    // ========================================
    // 3. VERIFICAR DISPONIBILIDAD
    // ========================================

    @GET("health")
    suspend fun verificarDisponibilidad(

        @Header("X-Test-Mode")
        modoPrueba: String? = null

    ): Response<HealthDto>


    // ========================================
    // 4. CONSULTAR TODOS LOS RECORDATORIOS
    // ========================================

    @GET("recordatorios")
    suspend fun obtenerRecordatorios(

        @Header("X-Test-Mode")
        modoPrueba: String? = null

    ): Response<List<RecordatorioDto>>


    // ========================================
    // 5. CONSULTAR RECORDATORIO POR UUID
    // ========================================

    @GET("recordatorios/{uuid}")
    suspend fun obtenerPorUuid(

        @Path("uuid")
        uuid: String,

        @Header("X-Test-Mode")
        modoPrueba: String? = null

    ): Response<RecordatorioDto>


    // ========================================
    // 6. CREAR O ACTUALIZAR RECORDATORIO
    // ========================================

    @PUT("recordatorios/{uuid}")
    suspend fun guardarRecordatorio(

        @Path("uuid")
        uuid: String,

        @Body
        recordatorio: RecordatorioDto,

        @Header("X-Test-Mode")
        modoPrueba: String? = null

    ): Response<RecordatorioDto>


    // ========================================
    // 7. ELIMINAR RECORDATORIO
    // ========================================

    @DELETE("recordatorios/{uuid}")
    suspend fun eliminarRecordatorio(

        @Path("uuid")
        uuid: String,

        @Query("revision")
        revision: Long,

        @Header("X-Test-Mode")
        modoPrueba: String? = null

    ): Response<RecordatorioDto>
}
 