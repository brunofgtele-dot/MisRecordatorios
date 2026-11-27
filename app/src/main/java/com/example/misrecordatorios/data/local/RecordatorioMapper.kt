
package com.example.misrecordatorios.data.local

import com.example.misrecordatorios.Categoria
import com.example.misrecordatorios.Prioridad
import com.example.misrecordatorios.EstadoRecordatorio
import com.example.misrecordatorios.Recordatorio
import com.example.misrecordatorios.data.sync.EstadoSincronizacion

// =============================================
// 1. CONVERTIR ENTIDAD ROOM A MODELO DE DOMINIO
// =============================================

fun RecordatorioEntity.toDomain(): Recordatorio {

    return Recordatorio(

        id = id,

        titulo = titulo,

        descripcion = descripcion,

        categoria = convertirCategoria(categoria),

        prioridad = convertirPrioridad(prioridad),

        estado = convertirEstado(estado),

        ubicacion = ubicacion,

        fechaAviso = fechaAviso,

        estadoSincronizacion =
            convertirEstadoSincronizacion(
                estadoSincronizacion
            )
    )
}

// =============================================
// 2. CONVERTIR MODELO DE DOMINIO A NUEVA ENTIDAD
// =============================================

fun Recordatorio.toEntity(): RecordatorioEntity {

    val fechaActual = System.currentTimeMillis()

    return RecordatorioEntity(

        // Room genera automaticamente el ID
        id = 0,

        titulo = titulo,

        descripcion = descripcion,

        categoria = categoria.nombre,

        prioridad = prioridad.nombre,

        estado = estado.nombre,

        ubicacion = ubicacion,

        fechaAviso = fechaAviso,

        fechaCreacion = fechaActual,

        fechaModificacion = fechaActual,

        estadoSincronizacion =
            EstadoSincronizacion.PENDIENTE.valor,

        revision = 1L,

        eliminado = false
    )
}

// =============================================
// 3. CONVERTIR UNA LISTA DE ENTIDADES
// =============================================

fun List<RecordatorioEntity>.toDomainList():
        List<Recordatorio> {

    return this

        .filterNot { it.eliminado }

        .map { entidad ->

            entidad.toDomain()
        }
}

// =============================================
// 4. CONVERTIR CATEGORIA
// =============================================

private fun convertirCategoria(
    valor: String
): Categoria {

    return when (valor.trim().lowercase()) {

        "trabajo" ->
            Categoria.Trabajo

        "personal" ->
            Categoria.Personal

        "hogar" ->
            Categoria.Hogar

        "estudios" ->
            Categoria.Estudios

        else -> throw IllegalArgumentException(
            "Categoria desconocida: $valor"
        )
    }
}

// =============================================
// 5. CONVERTIR PRIORIDAD
// =============================================

private fun convertirPrioridad(
    valor: String
): Prioridad {

    return when (valor.trim().lowercase()) {

        "alta" ->
            Prioridad.Alta

        "media" ->
            Prioridad.Media

        "baja" ->
            Prioridad.Baja

        else -> throw IllegalArgumentException(
            "Prioridad desconocida: $valor"
        )
    }
}

// =============================================
// 6. CONVERTIR ESTADO
// =============================================

private fun convertirEstado(
    valor: String
): EstadoRecordatorio {

    return when (valor.trim().lowercase()) {

        "pendiente" ->
            EstadoRecordatorio.Pendiente

        "en progreso" ->
            EstadoRecordatorio.EnProgreso

        "completado" ->
            EstadoRecordatorio.Completado

        else -> throw IllegalArgumentException(
            "Estado desconocido: $valor"
        )
    }
}

// =============================================
// 7. CONVERTIR ESTADO DE SINCRONIZACION
// =============================================

private fun convertirEstadoSincronizacion(
    valor: String
): EstadoSincronizacion {

    return EstadoSincronizacion.desdeValorONull(
        valor.trim().uppercase()
    ) ?: EstadoSincronizacion.ERROR
}
 