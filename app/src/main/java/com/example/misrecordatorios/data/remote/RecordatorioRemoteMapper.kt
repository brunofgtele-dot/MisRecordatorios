
package com.example.misrecordatorios.data.remote

import com.example.misrecordatorios.data.local.RecordatorioEntity
import java.util.UUID


// ================================================
// 1. VALORES VALIDOS DEL MODELO
// ================================================

private val categoriasValidas = setOf(
    "Trabajo",
    "Personal",
    "Hogar",
    "Estudios"
)

private val prioridadesValidas = setOf(
    "Alta",
    "Media",
    "Baja"
)

private val estadosValidos = setOf(
    "Pendiente",
    "En progreso",
    "Completado"
)


// ================================================
// 2. CONVERTIR ENTITY A DTO
// ================================================

fun RecordatorioEntity.toDto(): RecordatorioDto {

    return RecordatorioDto(

        uuid = uuid,

        titulo = titulo,

        descripcion = descripcion,

        categoria = categoria,

        prioridad = prioridad,

        estado = estado,

        ubicacion = ubicacion,

        fechaAviso = fechaAviso,

        fechaCreacion = fechaCreacion,

        fechaModificacion = fechaModificacion,

        revision = revision,

        eliminado = eliminado
    )
}


// ================================================
// 3. CONVERTIR DTO A ENTITY
// ================================================

fun RecordatorioDto.toEntity(
    idLocal: Int = 0
): RecordatorioEntity {

    require(idLocal >= 0) {
        "El ID local no puede ser negativo"
    }


    // Validar UUID

    val uuidValido = validarUuid(uuid)


    // Validar titulo

    val tituloValido = requireNotNull(titulo) {
        "El titulo remoto es obligatorio"
    }

    require(tituloValido.isNotBlank()) {
        "El titulo remoto no puede estar vacio"
    }

    require(tituloValido.length <= 100) {
        "El titulo remoto no puede superar 100 caracteres"
    }

    require(tituloValido == tituloValido.trim()) {
        "El titulo remoto contiene espacios externos"
    }


    // Validar categoria

    val categoriaValida = requireNotNull(categoria) {
        "La categoria remota es obligatoria"
    }

    require(categoriaValida in categoriasValidas) {
        "Categoria remota invalida: $categoriaValida"
    }


    // Validar prioridad

    val prioridadValida = requireNotNull(prioridad) {
        "La prioridad remota es obligatoria"
    }

    require(prioridadValida in prioridadesValidas) {
        "Prioridad remota invalida: $prioridadValida"
    }


    // Validar estado

    val estadoValido = requireNotNull(estado) {
        "El estado remoto es obligatorio"
    }

    require(estadoValido in estadosValidos) {
        "Estado remoto invalido: $estadoValido"
    }


    // Validar fecha de creacion

    val fechaCreacionValida = requireNotNull(
        fechaCreacion
    ) {
        "La fecha de creacion es obligatoria"
    }

    require(fechaCreacionValida >= 0L) {
        "La fecha de creacion es invalida"
    }


    // Validar fecha de modificacion

    val fechaModificacionValida = requireNotNull(
        fechaModificacion
    ) {
        "La fecha de modificacion es obligatoria"
    }

    require(fechaModificacionValida >= 0L) {
        "La fecha de modificacion es invalida"
    }


    // Validar revision

    val revisionValida = requireNotNull(revision) {
        "La revision remota es obligatoria"
    }

    require(revisionValida >= 1L) {
        "La revision remota debe ser mayor o igual a 1"
    }


    // Validar eliminacion logica

    val eliminadoValido = requireNotNull(eliminado) {
        "El indicador eliminado es obligatorio"
    }


    // Construir entidad local validada

    return RecordatorioEntity(

        id = idLocal,

        uuid = uuidValido,

        titulo = tituloValido,

        descripcion = descripcion,

        categoria = categoriaValida,

        prioridad = prioridadValida,

        estado = estadoValido,

        ubicacion = ubicacion,

        fechaAviso = fechaAviso,

        fechaCreacion = fechaCreacionValida,

        fechaModificacion = fechaModificacionValida,

        estadoSincronizacion = "SINCRONIZADO",

        revision = revisionValida,

        eliminado = eliminadoValido
    )
}


// ================================================
// 4. CONVERTIR LISTA DE ENTIDADES A DTO
// ================================================

fun List<RecordatorioEntity>.toDtoList():
        List<RecordatorioDto> {

    return map { recordatorio ->

        recordatorio.toDto()
    }
}


// ================================================
// 5. CONVERTIR LISTA REMOTA A ENTIDADES
// ================================================

fun List<RecordatorioDto>.toEntityList():
        List<RecordatorioEntity> {

    return map { recordatorio ->

        recordatorio.toEntity()
    }
}


// ================================================
// 6. VALIDAR UUID
// ================================================

private fun validarUuid(valor: String?): String {

    val uuidRecibido = requireNotNull(valor) {
        "El UUID remoto es obligatorio"
    }

    require(uuidRecibido.isNotBlank()) {
        "El UUID remoto no puede estar vacio"
    }

    val uuidConvertido = try {

        UUID.fromString(uuidRecibido)

    } catch (e: IllegalArgumentException) {

        throw IllegalArgumentException(
            "UUID remoto invalido: $uuidRecibido",
            e
        )
    }

    require(
        uuidConvertido.toString() == uuidRecibido
    ) {
        "El UUID remoto no tiene formato canonico"
    }

    return uuidRecibido
}
 