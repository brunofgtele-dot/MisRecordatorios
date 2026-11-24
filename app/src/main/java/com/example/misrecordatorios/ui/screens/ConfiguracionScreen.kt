
package com.example.misrecordatorios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.misrecordatorios.data.preferences.OrdenRecordatorios
import com.example.misrecordatorios.viewmodel.PreferenciasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    viewModel: PreferenciasViewModel,
    onVolver: () -> Unit
) {

    // Observar preferencias respetando el ciclo de vida
    val preferencias by viewModel.preferencias
        .collectAsStateWithLifecycle()

    // Observar errores de escritura
    val error by viewModel.error
        .collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    // Mostrar errores mediante Snackbar
    LaunchedEffect(error) {

        error?.let { mensaje ->

            snackbarHostState.showSnackbar(mensaje)

            viewModel.limpiarError()
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Configuración")
                },

                navigationIcon = {

                    TextButton(
                        onClick = onVolver
                    ) {
                        Text("Volver")
                    }
                }
            )
        },

        snackbarHost = {

            SnackbarHost(
                hostState = snackbarHostState
            )
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)

        ) {

            // =====================================
            // SECCION: APARIENCIA
            // =====================================

            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Tema oscuro",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Cambiar apariencia de la aplicación",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Switch(

                        checked = preferencias.temaOscuro,

                        onCheckedChange = {
                            viewModel.cambiarTemaOscuro(it)
                        }
                    )
                }
            }

            HorizontalDivider()

            // =====================================
            // SECCION: RECORDATORIOS
            // =====================================

            Text(
                text = "Recordatorios",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Mostrar completados",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Incluir recordatorios finalizados",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Switch(

                        checked =
                            preferencias.mostrarCompletados,

                        onCheckedChange = {

                            viewModel
                                .cambiarMostrarCompletados(it)
                        }
                    )
                }
            }

            // =====================================
            // SECCION: ORDENAMIENTO
            // =====================================

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(

                    modifier = Modifier.padding(16.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)

                ) {

                    Text(
                        text = "Ordenar recordatorios por",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OrdenRecordatorios.entries.forEach { orden ->

                        Row(

                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected =
                                        preferencias.orden == orden,
                                    onClick = {
                                        viewModel.cambiarOrden(orden)
                                    },
                                    role = Role.RadioButton
                                )
                                .heightIn(min = 48.dp),

                            verticalAlignment =
                                Alignment.CenterVertically

                        ) {

                            RadioButton(

                                selected =
                                    preferencias.orden == orden,

                                onClick = null
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = orden.nombre,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // =====================================
            // RESTAURAR PREFERENCIAS
            // =====================================

            OutlinedButton(

                onClick = {
                    viewModel.restaurarPreferencias()
                },

                modifier = Modifier.fillMaxWidth()

            ) {

                Text("Restaurar preferencias")
            }

            Text(

                text = "Los cambios se guardan automáticamente.",

                style = MaterialTheme.typography.bodySmall,

                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
 