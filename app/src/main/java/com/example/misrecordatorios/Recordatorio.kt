package com.example.misrecordatorios
// Categorías disponibles para los recordatorios
sealed class Categoria(val nombre: String) {
    data object Trabajo : Categoria("Trabajo")
    data object Personal : Categoria("Personal")
    data object Hogar : Categoria("Hogar")
    data object Estudios : Categoria("Estudios")
}
// Prioridades de los recordatorios
sealed class Prioridad(val nombre: String) {
    data object Alta : Prioridad("Alta")
    data object Media : Prioridad("Media")
    data object Baja : Prioridad("Baja")
}
// Estados posibles
sealed class EstadoRecordatorio(val nombre: String) {
    data object Pendiente : EstadoRecordatorio("Pendiente")
    data object EnProgreso : EstadoRecordatorio("En progreso")
    data object Completado : EstadoRecordatorio("Completado")
}
// Modelo inmutable de recordatorio
data class Recordatorio(
    val id: Int,
    val titulo: String,
    val descripcion: String? = null,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio,
    val ubicacion: String? = null,
    val fechaAviso: String? = null
) {
    init {
        require(id > 0) {
            "El ID debe ser mayor que cero"
        }
        require(titulo.isNotBlank()) {
            "El título no puede estar vacío"
        }
        require(titulo.length <= 100) {
            "El título no puede superar 100 caracteres"
        }
    }
}

