
package com.example.misrecordatorios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.misrecordatorios.Categoria
import com.example.misrecordatorios.Prioridad
import com.example.misrecordatorios.viewmodel.GuardadoUiState
import com.example.misrecordatorios.viewmodel.RecordatoriosViewModel
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearRecordatorioScreen(
    viewModel: RecordatoriosViewModel,
    onGuardarExitoso: () -> Unit,
    onCancelar: () -> Unit
) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }

    var categoria by remember {
        mutableStateOf<Categoria>(Categoria.Personal)
    }

    var prioridad by remember {
        mutableStateOf<Prioridad>(Prioridad.Media)
    }

    var mostrarCategorias by remember { mutableStateOf(false) }
    var mostrarPrioridades by remember { mutableStateOf(false) }

    val guardadoState by
    viewModel.guardadoState.collectAsStateWithLifecycle()

    val guardando = guardadoState is GuardadoUiState.Saving
    val tituloValido =
        titulo.isNotBlank() && titulo.length <= 100

    LaunchedEffect(guardadoState) {
        if (guardadoState is GuardadoUiState.Saved) {
            viewModel.reiniciarGuardado()
            onGuardarExitoso()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo recordatorio") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Completa los datos del recordatorio",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = titulo,
                onValueChange = {
                    if (it.length <= 100) {
                        titulo = it
                    }
                },
                label = { Text("Título *") },
                placeholder = { Text("Ej. Comprar materiales") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !guardando,
                isError = titulo.isNotEmpty() &&
                        titulo.isBlank(),
                supportingText = {
                    if (titulo.isNotEmpty() && titulo.isBlank()) {
                        Text(
                            text = "El título no puede contener solo espacios",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("${titulo.length}/100 caracteres")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !guardando
            )

            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleSmall
            )

            ExposedDropdownMenuBox(
                expanded = mostrarCategorias,
                onExpandedChange = {
                    if (!guardando) {
                        mostrarCategorias = !mostrarCategorias
                    }
                }
            ) {
                OutlinedTextField(
                    value = categoria.nombre,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona una categoría") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = mostrarCategorias
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        ),
                    enabled = !guardando
                )

                ExposedDropdownMenu(
                    expanded = mostrarCategorias,
                    onDismissRequest = {
                        mostrarCategorias = false
                    }
                ) {
                    listOf(
                        Categoria.Personal,
                        Categoria.Trabajo,
                        Categoria.Hogar,
                        Categoria.Estudios
                    ).forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion.nombre) },
                            onClick = {
                                categoria = opcion
                                mostrarCategorias = false
                            }
                        )
                    }
                }
            }

            Text(
                text = "Prioridad",
                style = MaterialTheme.typography.titleSmall
            )

            ExposedDropdownMenuBox(
                expanded = mostrarPrioridades,
                onExpandedChange = {
                    if (!guardando) {
                        mostrarPrioridades = !mostrarPrioridades
                    }
                }
            ) {
                OutlinedTextField(
                    value = prioridad.nombre,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona una prioridad") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = mostrarPrioridades
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        ),
                    enabled = !guardando
                )

                ExposedDropdownMenu(
                    expanded = mostrarPrioridades,
                    onDismissRequest = {
                        mostrarPrioridades = false
                    }
                ) {
                    listOf(
                        Prioridad.Alta,
                        Prioridad.Media,
                        Prioridad.Baja
                    ).forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion.nombre) },
                            onClick = {
                                prioridad = opcion
                                mostrarPrioridades = false
                            }
                        )
                    }
                }
            }

            if (guardadoState is GuardadoUiState.Error) {
                Text(
                    text = (guardadoState as GuardadoUiState.Error)
                        .mensaje,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (guardando) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Guardando recordatorio...")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.reiniciarGuardado()
                        onCancelar()
                    },
                    enabled = !guardando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        viewModel.guardarRecordatorio(
                            titulo = titulo,
                            descripcion = descripcion,
                            categoria = categoria,
                            prioridad = prioridad
                        )
                    },
                    enabled = tituloValido && !guardando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}