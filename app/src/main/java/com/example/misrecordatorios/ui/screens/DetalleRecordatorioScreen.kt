
package com.example.misrecordatorios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.misrecordatorios.Categoria
import com.example.misrecordatorios.Prioridad
import com.example.misrecordatorios.EstadoRecordatorio
import com.example.misrecordatorios.Recordatorio
import com.example.misrecordatorios.viewmodel.RecordatoriosViewModel
import com.example.misrecordatorios.viewmodel.RecordatoriosUiState
import com.example.misrecordatorios.viewmodel.OperacionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleRecordatorioScreen(
    recordatorioId: Int,
    viewModel: RecordatoriosViewModel,
    onVolver: () -> Unit
) {

    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    val operacionState by
    viewModel.operacionState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    var mostrarEdicion by rememberSaveable {
        mutableStateOf(false)
    }

    var mostrarConfirmacionEliminar by rememberSaveable {
        mutableStateOf(false)
    }

    val procesando =
        operacionState == OperacionUiState.Procesando

    val recordatorio = when (val estado = uiState) {

        is RecordatoriosUiState.Success ->
            estado.recordatorios.find {
                it.id == recordatorioId
            }

        else -> null
    }

    // =========================================
    // CONTROL DE RESULTADOS
    // =========================================


    LaunchedEffect(operacionState) {

        when (val resultado = operacionState) {

            OperacionUiState.Actualizado -> {

                mostrarEdicion = false

                snackbarHostState.showSnackbar(
                    message = "Recordatorio actualizado correctamente.",
                    duration = SnackbarDuration.Short
                )

                viewModel.reiniciarOperacion()
            }

            OperacionUiState.Eliminado -> {

                mostrarConfirmacionEliminar = false

                viewModel.reiniciarOperacion()

                onVolver()
            }

            is OperacionUiState.Error -> {

                snackbarHostState.showSnackbar(
                    message = resultado.mensaje,
                    duration = SnackbarDuration.Short
                )

                viewModel.reiniciarOperacion()
            }

            else -> Unit
        }
    }


    Scaffold(

        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },

        topBar = {

            TopAppBar(

                title = {
                    Text("Detalle recordatorio")
                },

                navigationIcon = {

                    TextButton(
                        onClick = onVolver,
                        enabled = !procesando
                    ) {
                        Text("Volver")
                    }
                }
            )
        }

    ) { paddingValues ->

        when (val estado = uiState) {

            RecordatoriosUiState.Loading -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is RecordatoriosUiState.Error -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(estado.mensaje)

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.reintentar()
                        }
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            else -> {

                if (recordatorio != null) {

                    ContenidoDetalle(

                        recordatorio = recordatorio,

                        procesando = procesando,

                        modifier = Modifier
                            .padding(paddingValues),

                        onCambiarEstado = { nuevoEstado ->

                            viewModel.actualizarEstado(
                                id = recordatorio.id,
                                nuevoEstado = nuevoEstado
                            )
                        },

                        onEditar = {
                            mostrarEdicion = true
                        },

                        onEliminar = {
                            mostrarConfirmacionEliminar =
                                true
                        }
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        if (procesando) {

                            CircularProgressIndicator()

                        } else {

                            Text(
                                "El recordatorio no esta disponible."
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================
    // DIALOGO DE EDICION
    // =========================================

    if (mostrarEdicion && recordatorio != null) {

        DialogoEditarRecordatorio(

            recordatorio = recordatorio,

            procesando = procesando,

            onCancelar = {

                if (!procesando) {
                    mostrarEdicion = false
                }
            },

            onGuardar = {
                    titulo,
                    descripcion,
                    categoria,
                    prioridad ->

                viewModel.actualizarDatos(

                    id = recordatorio.id,

                    titulo = titulo,

                    descripcion = descripcion,

                    categoria = categoria,

                    prioridad = prioridad
                )
            }
        )
    }

    // =========================================
    // CONFIRMACION DE ELIMINACION
    // =========================================

    if (mostrarConfirmacionEliminar &&
        recordatorio != null
    ) {

        AlertDialog(

            onDismissRequest = {

                if (!procesando) {

                    mostrarConfirmacionEliminar =
                        false
                }
            },

            title = {
                Text("Eliminar recordatorio")
            },

            text = {

                Text(
                    "¿Deseas eliminar " +
                            "\"${recordatorio.titulo}\"? " +
                            "Esta accion no se puede deshacer."
                )
            },

            confirmButton = {

                TextButton(

                    enabled = !procesando,

                    onClick = {

                        viewModel.eliminarRecordatorio(
                            recordatorio.id
                        )
                    }

                ) {

                    Text(
                        text = if (procesando) {
                            "Eliminando..."
                        } else {
                            "Eliminar"
                        },

                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {

                TextButton(

                    enabled = !procesando,

                    onClick = {

                        mostrarConfirmacionEliminar =
                            false
                    }

                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// =============================================
// CONTENIDO PRINCIPAL DEL DETALLE
// =============================================

@Composable
private fun ContenidoDetalle(

    recordatorio: Recordatorio,

    procesando: Boolean,

    modifier: Modifier = Modifier,

    onCambiarEstado: (EstadoRecordatorio) -> Unit,

    onEditar: () -> Unit,

    onEliminar: () -> Unit

) {

    val estados = listOf(

        EstadoRecordatorio.Pendiente,

        EstadoRecordatorio.EnProgreso,

        EstadoRecordatorio.Completado
    )

    Column(

        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)

    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(

                modifier = Modifier.padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)

            ) {

                Text(

                    text = recordatorio.titulo,

                    style =
                        MaterialTheme.typography.headlineSmall
                )

                Text(

                    text = recordatorio.descripcion
                        ?: "Sin descripcion",

                    style =
                        MaterialTheme.typography.bodyLarge
                )

                HorizontalDivider()

                Text(
                    "ID: ${recordatorio.id}"
                )

                Text(
                    "Categoria: ${recordatorio.categoria.nombre}"
                )

                Text(
                    "Prioridad: ${recordatorio.prioridad.nombre}"
                )

                Text(
                    "Estado: ${recordatorio.estado.nombre}"
                )

                recordatorio.ubicacion?.let {

                    Text("Ubicacion: $it")
                }

                recordatorio.fechaAviso?.let {

                    Text("Fecha de aviso: $it")
                }
            }
        }

        SelectorOpcion(

            etiqueta = "Cambiar estado",

            seleccionado = recordatorio.estado,

            opciones = estados,

            nombre = { it.nombre },

            habilitado = !procesando,

            onSeleccionar = { nuevoEstado ->

                if (nuevoEstado != recordatorio.estado) {

                    onCambiarEstado(nuevoEstado)
                }
            }
        )

        Button(

            onClick = onEditar,

            enabled = !procesando,

            modifier = Modifier.fillMaxWidth()

        ) {
            Text("Editar recordatorio")
        }

        OutlinedButton(

            onClick = onEliminar,

            enabled = !procesando,

            modifier = Modifier.fillMaxWidth(),

            colors = ButtonDefaults.outlinedButtonColors(

                contentColor =
                    MaterialTheme.colorScheme.error
            )

        ) {
            Text("Eliminar recordatorio")
        }

        if (procesando) {

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =============================================
// DIALOGO PARA EDITAR RECORDATORIO
// =============================================

@Composable
private fun DialogoEditarRecordatorio(

    recordatorio: Recordatorio,

    procesando: Boolean,

    onCancelar: () -> Unit,

    onGuardar: (
        String,
        String?,
        Categoria,
        Prioridad
    ) -> Unit

) {

    val categorias = listOf(

        Categoria.Trabajo,

        Categoria.Personal,

        Categoria.Hogar,

        Categoria.Estudios
    )

    val prioridades = listOf(

        Prioridad.Alta,

        Prioridad.Media,

        Prioridad.Baja
    )

    var titulo by rememberSaveable(recordatorio.id) {

        mutableStateOf(recordatorio.titulo)
    }

    var descripcion by rememberSaveable(recordatorio.id) {

        mutableStateOf(
            recordatorio.descripcion.orEmpty()
        )
    }

    var categoriaNombre by rememberSaveable(recordatorio.id) {

        mutableStateOf(recordatorio.categoria.nombre)
    }

    var prioridadNombre by rememberSaveable(recordatorio.id) {

        mutableStateOf(recordatorio.prioridad.nombre)
    }

    val categoria = categorias.firstOrNull {

        it.nombre == categoriaNombre

    } ?: recordatorio.categoria

    val prioridad = prioridades.firstOrNull {

        it.nombre == prioridadNombre

    } ?: recordatorio.prioridad

    val tituloValido =
        titulo.isNotBlank() &&
                titulo.trim().length <= 100

    AlertDialog(

        onDismissRequest = {

            if (!procesando) {
                onCancelar()
            }
        },

        title = {
            Text("Editar recordatorio")
        },

        text = {

            Column(

                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)

            ) {

                OutlinedTextField(

                    value = titulo,

                    onValueChange = {
                        titulo = it
                    },

                    label = {
                        Text("Titulo")
                    },

                    singleLine = true,

                    enabled = !procesando,

                    isError = !tituloValido,

                    supportingText = {

                        Text(
                            "${titulo.trim().length}/100 caracteres"
                        )
                    },

                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = descripcion,

                    onValueChange = {
                        descripcion = it
                    },

                    label = {
                        Text("Descripcion")
                    },

                    enabled = !procesando,

                    minLines = 2,

                    maxLines = 4,

                    modifier = Modifier.fillMaxWidth()
                )

                SelectorOpcion(

                    etiqueta = "Categoria",

                    seleccionado = categoria,

                    opciones = categorias,

                    nombre = { it.nombre },

                    habilitado = !procesando,

                    onSeleccionar = {

                        categoriaNombre = it.nombre
                    }
                )

                SelectorOpcion(

                    etiqueta = "Prioridad",

                    seleccionado = prioridad,

                    opciones = prioridades,

                    nombre = { it.nombre },

                    habilitado = !procesando,

                    onSeleccionar = {

                        prioridadNombre = it.nombre
                    }
                )
            }
        },

        confirmButton = {

            Button(

                enabled = tituloValido && !procesando,

                onClick = {

                    onGuardar(

                        titulo.trim(),

                        descripcion.trim()
                            .takeIf { it.isNotBlank() },

                        categoria,

                        prioridad
                    )
                }

            ) {

                Text(
                    if (procesando) {
                        "Guardando..."
                    } else {
                        "Guardar"
                    }
                )
            }
        },

        dismissButton = {

            TextButton(

                enabled = !procesando,

                onClick = onCancelar

            ) {
                Text("Cancelar")
            }
        }
    )
}

// =============================================
// SELECTOR REUTILIZABLE
// =============================================

@Composable
private fun <T> SelectorOpcion(

    etiqueta: String,

    seleccionado: T,

    opciones: List<T>,

    nombre: (T) -> String,

    habilitado: Boolean = true,

    onSeleccionar: (T) -> Unit

) {

    var expandido by remember {

        mutableStateOf(false)
    }

    Column(

        modifier = Modifier.fillMaxWidth(),

        verticalArrangement =
            Arrangement.spacedBy(4.dp)

    ) {

        Text(

            text = etiqueta,

            style =
                MaterialTheme.typography.labelLarge
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedButton(

                enabled = habilitado,

                onClick = {
                    expandido = true
                },

                modifier = Modifier.fillMaxWidth()

            ) {

                Text(
                    text = nombre(seleccionado),

                    modifier = Modifier.weight(1f)
                )

                Text("▼")
            }

            DropdownMenu(

                expanded = expandido && habilitado,

                onDismissRequest = {
                    expandido = false
                }

            ) {

                opciones.forEach { opcion ->

                    DropdownMenuItem(

                        text = {
                            Text(nombre(opcion))
                        },

                        onClick = {

                            expandido = false

                            onSeleccionar(opcion)
                        }
                    )
                }
            }
        }
    }
}
 