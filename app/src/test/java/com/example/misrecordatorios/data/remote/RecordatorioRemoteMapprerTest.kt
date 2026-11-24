
package com.example.misrecordatorios.data.remote

import com.example.misrecordatorios.data.local.RecordatorioEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class RecordatorioRemoteMapperTest {

    // ============================================
    // 1. DATOS DE PRUEBA
    // ============================================

    private val uuidPrueba =
        "550e8400-e29b-41d4-a716-446655440000"

    private fun crearEntityValida(): RecordatorioEntity {

        return RecordatorioEntity(

            id = 15,

            uuid = uuidPrueba,

            titulo = "Estudiar Kotlin y Room",

            descripcion = "Preparar Actividad 3",

            categoria = "Estudios",

            prioridad = "Alta",

            estado = "Pendiente",

            ubicacion = "Casa",

            fechaAviso = "2026-12-01",

            fechaCreacion = 1795478400000L,

            fechaModificacion = 1795478500000L,

            estadoSincronizacion = "PENDIENTE",

            revision = 3L,

            eliminado = false
        )
    }

    private fun crearDtoValido(): RecordatorioDto {

        return crearEntityValida().toDto()
    }


    // ============================================
    // 2. FUNCION AUXILIAR PARA VALIDAR ERRORES
    // ============================================

    private fun esperarError(
        mensajeEsperado: String,
        accion: () -> Unit
    ) {

        try {

            accion()

            fail(
                "Se esperaba IllegalArgumentException"
            )

        } catch (e: IllegalArgumentException) {

            assertTrue(
                "Mensaje recibido: ${e.message}",
                e.message.orEmpty()
                    .contains(mensajeEsperado)
            )
        }
    }


    // ============================================
    // 3. PRUEBAS DE CONVERSION CORRECTA
    // ============================================

    @Test
    fun convertirEntityADtoCorrectamente() {

        val entity = crearEntityValida()

        val dto = entity.toDto()

        assertEquals(entity.uuid, dto.uuid)
        assertEquals(entity.titulo, dto.titulo)
        assertEquals(entity.descripcion, dto.descripcion)
        assertEquals(entity.categoria, dto.categoria)
        assertEquals(entity.prioridad, dto.prioridad)
        assertEquals(entity.estado, dto.estado)
        assertEquals(entity.ubicacion, dto.ubicacion)
        assertEquals(entity.fechaAviso, dto.fechaAviso)
        assertEquals(entity.fechaCreacion, dto.fechaCreacion)
        assertEquals(entity.fechaModificacion, dto.fechaModificacion)
        assertEquals(entity.revision, dto.revision)
        assertEquals(entity.eliminado, dto.eliminado)
    }


    @Test
    fun convertirDtoAEntityCorrectamente() {

        val dto = crearDtoValido()

        val entity = dto.toEntity()

        assertEquals(0, entity.id)

        assertEquals(
            crearEntityValida().copy(
                id = 0,
                estadoSincronizacion = "SINCRONIZADO"
            ),
            entity
        )

        assertFalse(entity.eliminado)
    }


    @Test
    fun conservarIdLocalExistente() {

        val dto = crearDtoValido()

        val entity = dto.toEntity(
            idLocal = 15
        )

        assertEquals(15, entity.id)
        assertEquals(uuidPrueba, entity.uuid)
    }


    @Test
    fun conservarUuidOriginal() {

        val entityOriginal = crearEntityValida()

        val dto = entityOriginal.toDto()

        val entityConvertida = dto.toEntity(
            idLocal = entityOriginal.id
        )

        assertEquals(
            entityOriginal.uuid,
            dto.uuid
        )

        assertEquals(
            entityOriginal.uuid,
            entityConvertida.uuid
        )
    }


    @Test
    fun conservarRevisionOriginal() {

        val entityOriginal = crearEntityValida()

        val dto = entityOriginal.toDto()

        val entityConvertida = dto.toEntity()

        assertEquals(
            3L,
            requireNotNull(dto.revision)
        )

        assertEquals(
            3L,
            entityConvertida.revision
        )
    }


    @Test
    fun conservarEliminacionLogica() {

        val entityEliminada = crearEntityValida()
            .copy(
                eliminado = true,
                revision = 4L
            )

        val dto = entityEliminada.toDto()

        val entityConvertida = dto.toEntity()

        assertEquals(true, dto.eliminado)

        assertTrue(entityConvertida.eliminado)

        assertEquals(
            4L,
            entityConvertida.revision
        )

        assertEquals(
            uuidPrueba,
            entityConvertida.uuid
        )
    }


    @Test
    fun aceptarCamposOpcionalesNulos() {

        val dto = crearDtoValido().copy(

            descripcion = null,

            ubicacion = null,

            fechaAviso = null
        )

        val entity = dto.toEntity()

        assertNull(entity.descripcion)
        assertNull(entity.ubicacion)
        assertNull(entity.fechaAviso)
    }


    @Test
    fun convertirListasCorrectamente() {

        val primera = crearEntityValida()

        val segunda = crearEntityValida().copy(

            id = 16,

            uuid =
                "550e8400-e29b-41d4-a716-446655440001",

            titulo = "Pagar electricidad"
        )

        val entidades = listOf(
            primera,
            segunda
        )

        val listaDto = entidades.toDtoList()

        val listaConvertida =
            listaDto.toEntityList()

        assertEquals(2, listaDto.size)

        assertEquals(2, listaConvertida.size)

        assertEquals(
            primera.uuid,
            listaConvertida[0].uuid
        )

        assertEquals(
            segunda.uuid,
            listaConvertida[1].uuid
        )

        assertEquals(
            segunda.titulo,
            listaConvertida[1].titulo
        )
    }


    @Test
    fun convertirListasVaciasCorrectamente() {

        val entidades =
            emptyList<RecordatorioEntity>()

        val dtos =
            emptyList<RecordatorioDto>()

        assertTrue(
            entidades.toDtoList().isEmpty()
        )

        assertTrue(
            dtos.toEntityList().isEmpty()
        )
    }


    // ============================================
    // 4. PRUEBAS DE DATOS INVALIDOS
    // ============================================

    @Test
    fun rechazarUuidInvalido() {

        val dto = crearDtoValido().copy(
            uuid = "12345"
        )

        esperarError("UUID remoto invalido") {

            dto.toEntity()
        }
    }


    @Test
    fun rechazarUuidNoCanonico() {

        val dto = crearDtoValido().copy(
            uuid = uuidPrueba.uppercase()
        )

        esperarError("formato canonico") {

            dto.toEntity()
        }
    }


    @Test
    fun rechazarTituloVacio() {

        val dto = crearDtoValido().copy(
            titulo = ""
        )

        esperarError("no puede estar vacio") {

            dto.toEntity()
        }
    }


    @Test
    fun rechazarTituloMayorA100Caracteres() {

        val dto = crearDtoValido().copy(
            titulo = "A".repeat(101)
        )

        esperarError("100 caracteres") {

            dto.toEntity()
        }
    }


    @Test
    fun rechazarCategoriaInvalida() {

        val dto = crearDtoValido().copy(
            categoria = "Desconocida"
        )

        esperarError("Categoria remota invalida") {

            dto.toEntity()
        }
    }


    @Test
    fun rechazarPrioridadInvalida() {

        val dto = crearDtoValido().copy(
            prioridad = "Urgente"
        )

        esperarError("Prioridad remota invalida") {

            dto.toEntity()
        }
    }


    @Test
    fun rechazarEstadoInvalido() {

        val dto = crearDtoValido().copy(
            estado = "Incorrecto"
        )

        esperarError("Estado remoto invalido") {

            dto.toEntity()
        }
    }


    @Test
    fun rechazarFechasNegativas() {

        val dtoCreacion = crearDtoValido().copy(
            fechaCreacion = -100L
        )

        val dtoModificacion = crearDtoValido().copy(
            fechaModificacion = -100L
        )

        esperarError("fecha de creacion es invalida") {

            dtoCreacion.toEntity()
        }

        esperarError("fecha de modificacion es invalida") {

            dtoModificacion.toEntity()
        }
    }


    @Test
    fun rechazarRevisionInvalida() {

        val dto = crearDtoValido().copy(
            revision = 0L
        )

        esperarError("revision remota debe ser") {

            dto.toEntity()
        }
    }


    @Test
    fun rechazarCamposObligatoriosNulos() {

        val dto = crearDtoValido()

        val casos = listOf(

            dto.copy(uuid = null) to
                    "UUID remoto es obligatorio",

            dto.copy(titulo = null) to
                    "titulo remoto es obligatorio",

            dto.copy(categoria = null) to
                    "categoria remota es obligatoria",

            dto.copy(prioridad = null) to
                    "prioridad remota es obligatoria",

            dto.copy(estado = null) to
                    "estado remoto es obligatorio",

            dto.copy(fechaCreacion = null) to
                    "fecha de creacion es obligatoria",

            dto.copy(fechaModificacion = null) to
                    "fecha de modificacion es obligatoria",

            dto.copy(revision = null) to
                    "revision remota es obligatoria",

            dto.copy(eliminado = null) to
                    "indicador eliminado es obligatorio"
        )

        casos.forEach { (dtoInvalido, mensaje) ->

            esperarError(mensaje) {

                dtoInvalido.toEntity()
            }
        }
    }


    @Test
    fun rechazarIdLocalNegativo() {

        val dto = crearDtoValido()

        esperarError("ID local no puede ser negativo") {

            dto.toEntity(
                idLocal = -1
            )
        }
    }
}
 