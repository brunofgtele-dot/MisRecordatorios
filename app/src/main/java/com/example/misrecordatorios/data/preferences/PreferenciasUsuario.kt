
package com.example.misrecordatorios.data.preferences

// Criterios de ordenamiento disponibles
enum class OrdenRecordatorios(val nombre: String) {

    CREACION("Orden de creación"),

    TITULO("Título"),

    PRIORIDAD("Prioridad")
}

// Modelo de preferencias del usuario
data class PreferenciasUsuario(

    val temaOscuro: Boolean = false,

    val mostrarCompletados: Boolean = true,

    val orden: OrdenRecordatorios =
        OrdenRecordatorios.CREACION
)
 