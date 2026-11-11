
package com.example.misrecordatorios.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.misrecordatorios.DatosSimulados
import com.example.misrecordatorios.Recordatorio
import com.example.misrecordatorios.viewmodel.RecordatoriosUiState
import com.example.misrecordatorios.viewmodel.RecordatoriosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    viewModel: RecordatoriosViewModel,
    onCrearClick: () -> Unit,
    onDetalleClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Recordatorios") },
                actions = {
                    TextButton(onClick = onCrearClick) {
                        Text("+ Nuevo")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // CONTROLES TEMPORALES PARA PRUEBAS
            Text(
                text = "Pruebas de estados UI",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.cargarRecordatorios()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("Loading")
                }

                Button(
                    onClick = {
                        viewModel.vaciarRecordatorios()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("Empty")
                }

                Button(
                    onClick = {
                        viewModel.provocarError()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("Error")
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    viewModel.restaurarRecordatorios()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restaurar datos")
            }

            Spacer(Modifier.height(16.dp))

            when (val estado = uiState) {

                is RecordatoriosUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is RecordatoriosUiState.Success -> {
                    Text(
                        text = "Mis tareas (${estado.recordatorios.size})",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = estado.recordatorios,
                            key = { it.id }
                        ) { recordatorio ->
                            TarjetaListado(
                                recordatorio = recordatorio,
                                onClick = {
                                    onDetalleClick(recordatorio.id)
                                }
                            )
                        }
                    }
                }

                is RecordatoriosUiState.Empty -> {
                    EstadoInformativo(
                        mensaje = "No hay recordatorios",
                        detalle = "Crea tu primer recordatorio",
                        textoBoton = "Crear recordatorio",
                        onClick = onCrearClick
                    )
                }

                is RecordatoriosUiState.Error -> {
                    EstadoInformativo(
                        mensaje = "Ocurrió un error",
                        detalle = estado.mensaje,
                        textoBoton = "Reintentar",
                        onClick = { viewModel.reintentar() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaListado(
    recordatorio: Recordatorio,
    onClick: () -> Unit
) {
    val color = Color(
        DatosSimulados.coloresCategorias[
            recordatorio.categoria
        ] ?: 0xFF757575
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = recordatorio.titulo,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(4.dp))

            recordatorio.descripcion?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Categoría: ${recordatorio.categoria.nombre}",
                color = color
            )

            Text(
                text = "Prioridad: ${recordatorio.prioridad.nombre}"
            )

            Text(
                text = "Estado: ${recordatorio.estado.nombre}"
            )
        }
    }
}

@Composable
private fun EstadoInformativo(
    mensaje: String,
    detalle: String,
    textoBoton: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = mensaje,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(8.dp))

            Text(text = detalle)

            Spacer(Modifier.height(16.dp))

            Button(onClick = onClick) {
                Text(textoBoton)
            }
        }
    }
}
 