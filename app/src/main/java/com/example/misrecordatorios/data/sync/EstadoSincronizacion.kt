
package com.example.misrecordatorios.data.sync


// ================================================
// 1. ESTADOS DE SINCRONIZACION DE UN RECORDATORIO
// ================================================

enum class EstadoSincronizacion(

    val valor: String,

    val descripcion: String

) {

    // ============================================
    // REGISTRO PENDIENTE DE SINCRONIZACION
    // ============================================

    PENDIENTE(

        valor = "PENDIENTE",

        descripcion = "Pendiente de sincronizacion"
    ),


    // ============================================
    // REGISTRO CONFIRMADO POR EL SERVIDOR
    // ============================================

    SINCRONIZADO(

        valor = "SINCRONIZADO",

        descripcion = "Sincronizado correctamente"
    ),


    // ============================================
    // REGISTRO CON ERROR DE SINCRONIZACION
    // ============================================

    ERROR(

        valor = "ERROR",

        descripcion = "Error de sincronizacion"
    );


    // ============================================
    // 2. VERIFICAR SI REQUIERE SINCRONIZACION
    // ============================================

    fun requiereSincronizacion(): Boolean {

        return when (this) {

            PENDIENTE -> true

            ERROR -> true

            SINCRONIZADO -> false
        }
    }


    // ============================================
    // 3. VERIFICAR SI ESTA SINCRONIZADO
    // ============================================

    fun estaSincronizado(): Boolean {

        return this == SINCRONIZADO
    }


    // ============================================
    // 4. VERIFICAR SI PRESENTA ERROR
    // ============================================

    fun tieneError(): Boolean {

        return this == ERROR
    }


    // ============================================
    // 5. CONVERTIR VALOR ALMACENADO EN ROOM
    // ============================================

    companion object {

        fun desdeValor(
            valor: String
        ): EstadoSincronizacion {

            return entries.firstOrNull {

                it.valor == valor

            } ?: throw IllegalArgumentException(

                "Estado de sincronizacion invalido: $valor"
            )
        }


        // ========================================
        // CONVERSION SEGURA PARA CONSULTAS
        // ========================================

        fun desdeValorONull(
            valor: String?
        ): EstadoSincronizacion? {

            return entries.firstOrNull {

                it.valor == valor
            }
        }
    }
}
 