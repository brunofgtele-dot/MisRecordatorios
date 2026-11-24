
package com.example.misrecordatorios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.misrecordatorios.Recordatorio
import com.example.misrecordatorios.Prioridad
import com.example.misrecordatorios.EstadoRecordatorio
import com.example.misrecordatorios.data.preferences.OrdenRecordatorios
import com.example.misrecordatorios.data.preferences.PreferenciasUsuario
import com.example.misrecordatorios.viewmodel.RecordatoriosViewModel
import com.example.misrecordatorios.viewmodel.RecordatoriosUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(

    viewModel: RecordatoriosViewModel,

    onCrearClick: () -> Unit,

    onDetalleClick: (Int) -> Unit,

    // API 3: preferencias del usuario
    preferencias: PreferenciasUsuario =
        PreferenciasUsuario(),

    // API 3: navegacion a configuracion
    onConfiguracionClick: () -> Unit = {}

) {

    val uiState by viewModel.uiState
        .collectAsStateWithLifecycle()

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("MisRecordatorios")
                },

                actions = {

                    TextButton(
                        onClick = onConfiguracionClick
                    ) {
                        Text("Config.")
                    }

                    TextButton(
                        onClick = onCrearClick
                    ) {
                        Text("+ Nuevo")
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)

        ) {

            // =====================================
            // API 2: CONTROLES DE PRUEBA
            // =====================================

            Text(
                text = "Pruebas de estados",
                style = MaterialTheme.typography.titleSmall
            )

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(4.dp)

            ) {

                OutlinedButton(

                    onClick = {
                        viewModel.simularLoading()
                    },

                    modifier = Modifier.weight(1f),

                    contentPadding = PaddingValues(4.dp)

                ) {
                    Text("Loading")
                }

                OutlinedButton(

                    onClick = {
                        viewModel.vaciarRecordatorios()
                    },

                    modifier = Modifier.weight(1f),

                    contentPadding = PaddingValues(4.dp)

                ) {
                    Text("Empty")
                }

                OutlinedButton(

                    onClick = {
                        viewModel.provocarError()
                    },

                    modifier = Modifier.weight(1f),

                    contentPadding = PaddingValues(4.dp)

                ) {
                    Text("Error")
                }
            }

            OutlinedButton(

                onClick = {
                    viewModel.restaurarRecordatorios()
                },

                modifier = Modifier.fillMaxWidth()

            ) {
                Text("Restaurar datos")
            }

            HorizontalDivider()

            // =====================================
            // API 3: PREFERENCIAS ACTIVAS
            // =====================================

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(

                    modifier = Modifier.padding(12.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)

                ) {

                    Text(

                        text = "Preferencias activas",

                        style =
                            MaterialTheme.typography.titleSmall,

                        color =
                            MaterialTheme.colorScheme.primary
                    )

                    Text(

                        text = "Orden: ${preferencias.orden.nombre}",

                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Text(

                        text = if (
                            preferencias.mostrarCompletados
                        ) {
                            "Mostrando todos los recordatorios"
                        } else {
                            "Ocultando recordatorios completados"
                        },

                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }

            // =====================================
            // API 2: ESTADOS DE LA INTERFAZ
            // =====================================

            when (val estado = uiState) {

                RecordatoriosUiState.Loading -> {

                    Box(

                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),

                        contentAlignment = Alignment.Center

                    ) {

                        Column(

                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)

                        ) {

                            CircularProgressIndicator()

                            Text("Cargando recordatorios...")
                        }
                    }
                }

                is RecordatoriosUiState.Success -> {

                    // API 3: aplicar filtro y ordenamiento
                    val recordatoriosVisibles = remember(
                        estado.recordatorios,
                        preferencias
                    ) {

                        aplicarPreferencias(
                            recordatorios = estado.recordatorios,
                            preferencias = preferencias
                        )
                    }

                    Text(

                        text = "Recordatorios: " +
                                "${recordatoriosVisibles.size}",

                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    if (recordatoriosVisibles.isEmpty()) {

                        Box(

                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),

                            contentAlignment = Alignment.Center

                        ) {

                            Column(

                                horizontalAlignment =
                                    Alignment.CenterHorizontally,

                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)

                            ) {

                                Text(
                                    "No hay recordatorios visibles."
                                )

                                Text(
                                    "Revisa los filtros de configuración."
                                )

                                OutlinedButton(

                                    onClick =
                                        onConfiguracionClick

                                ) {
                                    Text("Ir a configuración")
                                }
                            }
                        }

                    } else {

                        LazyColumn(

                            modifier = Modifier.weight(1f),

                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)

                        ) {

                            items(

                                items = recordatoriosVisibles,

                                key = { it.id }

                            ) { recordatorio ->

                                RecordatorioCard(

                                    recordatorio = recordatorio,

                                    onClick = {

                                        onDetalleClick(
                                            recordatorio.id
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                RecordatoriosUiState.Empty -> {

                    Box(

                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),

                        contentAlignment = Alignment.Center

                    ) {

                        Column(

                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)

                        ) {

                            Text(
                                "No hay recordatorios registrados."
                            )

                            Button(
                                onClick = onCrearClick
                            ) {
                                Text("Crear recordatorio")
                            }
                        }
                    }
                }

                is RecordatoriosUiState.Error -> {

                    Box(

                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),

                        contentAlignment = Alignment.Center

                    ) {

                        Column(

                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)

                        ) {

                            Text(

                                text = estado.mensaje,

                                color =
                                    MaterialTheme.colorScheme.error
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
                }
            }
        }
    }
}

// =============================================
// API 3: FILTRAR Y ORDENAR RECORDATORIOS
// =============================================

private fun aplicarPreferencias(

    recordatorios: List<Recordatorio>,

    preferencias: PreferenciasUsuario

): List<Recordatorio> {

    // Filtrar recordatorios completados
    val listaFiltrada = if (
        preferencias.mostrarCompletados
    ) {

        recordatorios

    } else {

        recordatorios.filter {

            it.estado != EstadoRecordatorio.Completado
        }
    }

    // Aplicar criterio de ordenamiento
    return when (preferencias.orden) {

        OrdenRecordatorios.CREACION -> {

            listaFiltrada.sortedBy {
                it.id
            }
        }

        OrdenRecordatorios.TITULO -> {

            listaFiltrada.sortedBy {
                it.titulo.lowercase()
            }
        }

        OrdenRecordatorios.PRIORIDAD -> {

            listaFiltrada.sortedWith(

                compareBy<Recordatorio> {

                    when (it.prioridad) {

                        Prioridad.Alta -> 0

                        Prioridad.Media -> 1

                        Prioridad.Baja -> 2
                    }

                }.thenBy {
                    it.id
                }
            )
        }
    }
}

// =============================================
// TARJETA DE RECORDATORIO
// =============================================

@Composable
private fun RecordatorioCard(

    recordatorio: Recordatorio,

    onClick: () -> Unit

) {

    Card(

        onClick = onClick,

        modifier = Modifier.fillMaxWidth()

    ) {

        Column(

            modifier = Modifier.padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(6.dp)

        ) {

            Text(

                text = recordatorio.titulo,

                style =
                    MaterialTheme.typography.titleMedium
            )

            if (!recordatorio.descripcion.isNullOrBlank()) {

                Text(

                    text = recordatorio.descripcion.orEmpty(),

                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Text(
                text = "Categoría: ${recordatorio.categoria.nombre}"
            )

            Text(

                text = "Prioridad: ${recordatorio.prioridad.nombre}",

                color = when (recordatorio.prioridad) {

                    Prioridad.Alta ->
                        MaterialTheme.colorScheme.error

                    Prioridad.Media ->
                        MaterialTheme.colorScheme.primary

                    Prioridad.Baja ->
                        MaterialTheme.colorScheme.secondary
                }
            )

            Text(
                text = "Estado: ${recordatorio.estado.nombre}"
            )
        }
    }
}
 