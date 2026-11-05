

package com.example.misrecordatorios
object DatosSimulados {
    // LIST: almacena los recordatorios simulados
    val recordatorios: List<Recordatorio> = listOf(
        Recordatorio(
            id = 1,
            titulo = "Comprar medicamentos",
            descripcion = "Pasar por la farmacia",
            categoria = Categoria.Personal,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Pendiente
        ),
        Recordatorio(
            id = 2,
            titulo = "Reunión de trabajo",
            descripcion = "Revisar avances del proyecto",
            categoria = Categoria.Trabajo,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.EnProgreso
        ),
        Recordatorio(
            id = 3,
            titulo = "Pagar electricidad",
            descripcion = "Pagar la cuenta mensual",
            categoria = Categoria.Hogar,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Completado
        ),
        Recordatorio(
            id = 4,
            titulo = "Estudiar Kotlin",
            descripcion = "Repasar colecciones y clases",
            categoria = Categoria.Estudios,
            prioridad = Prioridad.Baja,
            estado = EstadoRecordatorio.Pendiente
        ),
        Recordatorio(
            id = 5,
            titulo = "Preparar informe",
            descripcion = "Completar informe semanal",
            categoria = Categoria.Trabajo,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Pendiente
        )
    )
    // SET: evita categorías repetidas
    val categoriasUnicas: Set<Categoria> =
        recordatorios.map { it.categoria }.toSet()
    // MAP: asocia cada categoría con etiqueta y color
    val coloresCategorias: Map<Categoria, Long> = mapOf(
        Categoria.Trabajo to 0xFF1976D2,
        Categoria.Personal to 0xFF388E3C,
        Categoria.Hogar to 0xFFF57C00,
        Categoria.Estudios to 0xFF7B1FA2
    )
    // MAP: etiquetas descriptivas
    val etiquetasCategorias: Map<Categoria, String> = mapOf(
        Categoria.Trabajo to "Trabajo",
        Categoria.Personal to "Personal",
        Categoria.Hogar to "Hogar",
        Categoria.Estudios to "Estudios"
    )
    // Filtrar recordatorios por estado
    fun filtrarPorEstado(
        estado: EstadoRecordatorio
    ): List<Recordatorio> {
        return recordatorios.filter { it.estado == estado }
    }
    // Filtrar recordatorios por prioridad
    fun filtrarPorPrioridad(
        prioridad: Prioridad
    ): List<Recordatorio> {
        return recordatorios.filter { it.prioridad == prioridad }
    }
    // Filtrar recordatorios por estado y prioridad
    fun filtrarPorEstadoYPrioridad(
        estado: EstadoRecordatorio,
        prioridad: Prioridad
    ): List<Recordatorio> {
        return recordatorios.filter {
            it.estado == estado && it.prioridad == prioridad
        }
    }
}
 
 