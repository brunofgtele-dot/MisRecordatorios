
package com.example.misrecordatorios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.misrecordatorios.DatosSimulados
import com.example.misrecordatorios.EstadoRecordatorio
import com.example.misrecordatorios.viewmodel.RecordatoriosViewModel
import com.example.misrecordatorios.viewmodel.RecordatoriosUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleRecordatorioScreen(
    recordatorioId: Int,
    viewModel: RecordatoriosViewModel,
    onVolver: () -> Unit
) {

    // Observar los cambios publicados por el ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Obtener el recordatorio desde el estado observado
    val recordatorio = when (val estado = uiState) {
        is RecordatoriosUiState.Success ->
            estado.recordatorios.find {
                it.id == recordatorioId
            }

        else -> null
    }

    // Estado seleccionado en el formulario
    var estadoSeleccionado by remember(recordatorioId) {
        mutableStateOf<EstadoRecordatorio>(
            EstadoRecordatorio.Pendiente
        )
    }

    // Sincronizar el selector con el registro actual
    LaunchedEffect(recordatorio?.estado) {
        recordatorio?.let {
            estadoSeleccionado = it.estado
        }
    }

    var menuExpandido by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del recordatorio") },
                navigationIcon = {
                    TextButton(onClick = onVolver) {
                        Text("Volver")
                    }
                }
            )
        }
    ) { padding ->

        if (recordatorio == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Recordatorio no encontrado")

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = onVolver) {
                        Text("Volver al listado")
                    }
                }
            }
        } else {
            val colorCategoria = Color(
                DatosSimulados.coloresCategorias[
                    recordatorio.categoria
                ] ?: 0xFF757575
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = recordatorio.titulo,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = recordatorio.descripcion
                                ?: "Sin descripción",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        HorizontalDivider()

                        Text(
                            text = "ID: ${recordatorio.id}"
                        )

                        Text(
                            text = "Categoría: ${recordatorio.categoria.nombre}",
                            color = colorCategoria
                        )

                        Text(
                            text = "Prioridad: ${recordatorio.prioridad.nombre}"
                        )

                        Text(
                            text = "Estado actual: ${recordatorio.estado.nombre}"
                        )

                        HorizontalDivider()

                        Text(
                            text = "Cambiar estado",
                            style = MaterialTheme.typography.titleMedium
                        )

                        ExposedDropdownMenuBox(
                            expanded = menuExpandido,
                            onExpandedChange = {
                                menuExpandido = !menuExpandido
                            }
                        ) {
                            OutlinedTextField(
                                value = estadoSeleccionado.nombre,
                                onValueChange = {},
                                readOnly = true,
                                label = {
                                    Text("Seleccionar estado")
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = menuExpandido
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(
                                        ExposedDropdownMenuAnchorType
                                            .PrimaryNotEditable
                                    )
                            )

                            ExposedDropdownMenu(
                                expanded = menuExpandido,
                                onDismissRequest = {
                                    menuExpandido = false
                                }
                            ) {
                                listOf(
                                    EstadoRecordatorio.Pendiente,
                                    EstadoRecordatorio.EnProgreso,
                                    EstadoRecordatorio.Completado
                                ).forEach { opcion ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(opcion.nombre)
                                        },
                                        onClick = {
                                            estadoSeleccionado = opcion
                                            menuExpandido = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.actualizarEstado(
                                    recordatorioId,
                                    estadoSeleccionado
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled =
                                estadoSeleccionado != recordatorio.estado
                        ) {
                            Text("Actualizar estado")
                        }
                    }
                }
            }
        }
    }
}
 