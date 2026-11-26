package com.example.misrecordatorios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.misrecordatorios.EstadoRecordatorio
import com.example.misrecordatorios.Prioridad
import com.example.misrecordatorios.Recordatorio
import com.example.misrecordatorios.data.preferences.OrdenRecordatorios
import com.example.misrecordatorios.data.preferences.PreferenciasUsuario
import com.example.misrecordatorios.data.sync.ResultadoSincronizacion
import com.example.misrecordatorios.viewmodel.RecordatoriosUiState
import com.example.misrecordatorios.viewmodel.RecordatoriosViewModel

// ============================================
// 1. PANTALLA PRINCIPAL
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    viewModel: RecordatoriosViewModel,
    onCrearClick: () -> Unit,
    onDetalleClick: (Int) -> Unit,
    preferencias: PreferenciasUsuario = PreferenciasUsuario(),
    onConfiguracionClick: () -> Unit = {}
) {

    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    val cantidadPendientes by
    viewModel.cantidadPendientes.collectAsStateWithLifecycle()

    val sincronizando by
    viewModel.sincronizando.collectAsStateWithLifecycle()

    val ultimoResultado by
    viewModel.ultimoResultadoSincronizacion
        .collectAsStateWithLifecycle()

    val errorSincronizacion by
    viewModel.errorSincronizacion
        .collectAsStateWithLifecycle()

    val conectado by
    viewModel.conectado.collectAsStateWithLifecycle()

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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ====================================
            // 2. PANEL DE CONECTIVIDAD
            // ====================================

            item(key = "conectividad") {

                PanelConectividad(
                    conectado = conectado
                )
            }

            // ====================================
            // 3. PANEL DE SINCRONIZACION
            // ====================================

            item(key = "sincronizacion") {

                PanelSincronizacion(
                    conectado = conectado,
                    cantidadPendientes = cantidadPendientes,
                    sincronizando = sincronizando,
                    ultimoResultado = ultimoResultado,
                    errorSincronizacion = errorSincronizacion,
                    onSincronizar = {
                        viewModel.sincronizarRecordatorios()
                    },
                    onReintentar = {
                        viewModel.reintentarSincronizacion()
                    },
                    onCancelar = {
                        viewModel.cancelarSincronizacion()
                    },
                    onLimpiarResultado = {
                        viewModel.limpiarUltimoResultadoSincronizacion()
                    }
                )
            }

            // ====================================
            // 4. PREFERENCIAS ACTIVAS
            // ====================================

            item(key = "preferencias") {

                PanelPreferencias(
                    preferencias = preferencias,
                    onConfiguracionClick = onConfiguracionClick
                )
            }

            // ====================================
            // 5. CONTROLES DE PRUEBA
            // ====================================

            item(key = "simulaciones") {

                PanelSimulaciones(
                    viewModel = viewModel
                )
            }

            // ====================================
            // 6. ESTADOS DEL LISTADO
            // ====================================

            when (val estado = uiState) {

                RecordatoriosUiState.Loading -> {

                    item(key = "loading") {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                CircularProgressIndicator()

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                Text("Cargando recordatorios...")
                            }
                        }
                    }
                }

                RecordatoriosUiState.Empty -> {

                    item(key = "empty") {

                        PanelListadoVacio(
                            mensaje =
                                "No hay recordatorios registrados.",
                            textoBoton = "Crear recordatorio",
                            onClick = onCrearClick
                        )
                    }
                }

                is RecordatoriosUiState.Error -> {

                    item(key = "error_local") {

                        PanelErrorLocal(
                            mensaje = estado.mensaje,
                            onReintentar = {
                                viewModel.reintentar()
                            }
                        )
                    }
                }

                is RecordatoriosUiState.Success -> {

                    val recordatoriosVisibles =
                        aplicarPreferencias(
                            recordatorios = estado.recordatorios,
                            preferencias = preferencias
                        )

                    item(key = "contador") {

                        Text(
                            text =
                                "${recordatoriosVisibles.size} " +
                                        "recordatorios visibles",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (recordatoriosVisibles.isEmpty()) {

                        item(key = "filtrado_vacio") {

                            PanelListadoVacio(
                                mensaje =
                                    "No hay recordatorios visibles " +
                                            "con las preferencias actuales.",
                                textoBoton = "Revisar configuracion",
                                onClick = onConfiguracionClick
                            )
                        }

                    } else {

                        items(
                            items = recordatoriosVisibles,
                            key = { recordatorio ->
                                recordatorio.id
                            }
                        ) { recordatorio ->

                            RecordatorioCard(
                                recordatorio = recordatorio,
                                onClick = {
                                    onDetalleClick(recordatorio.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// 7. PANEL DE CONECTIVIDAD
// ============================================

@Composable
private fun PanelConectividad(
    conectado: Boolean
) {

    val colorIndicador =
        if (conectado) {
            Color(0xFF2E7D32)
        } else {
            Color(0xFFC62828)
        }

    val colorFondo =
        if (conectado) {
            Color(0xFFE8F5E9)
        } else {
            Color(0xFFFFEBEE)
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorFondo
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = colorIndicador,
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text =
                        if (conectado) {
                            "Con conexión"
                        } else {
                            "Sin conexión"
                        },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorIndicador
                )

                Text(
                    text =
                        if (conectado) {
                            "Red disponible para sincronizar."
                        } else {
                            "Modo offline. Los datos se guardan localmente."
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF424242)
                )
            }
        }
    }
}

// ============================================
// 8. PANEL DE SINCRONIZACION
// ============================================

@Composable
private fun PanelSincronizacion(
    conectado: Boolean,
    cantidadPendientes: Int,
    sincronizando: Boolean,
    ultimoResultado: ResultadoSincronizacion?,
    errorSincronizacion: String?,
    onSincronizar: () -> Unit,
    onReintentar: () -> Unit,
    onCancelar: () -> Unit,
    onLimpiarResultado: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = "Sincronizacion",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Almacenamiento local activo",
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider()

            // ====================================
            // 9. CONTADOR DE PENDIENTES
            // ====================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Pendientes o con error",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = cantidadPendientes.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (cantidadPendientes > 0) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                    )
                }

                Text(
                    text =
                        if (cantidadPendientes > 0) {
                            "Cambios locales\npor sincronizar"
                        } else {
                            "Sin pendientes\nlocales"
                        },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ====================================
            // 10. AVISO DE CONECTIVIDAD
            // ====================================

            if (!conectado) {

                Text(
                    text =
                        "Sin conexión de red. " +
                                "La sincronización no está disponible.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ====================================
            // 11. SINCRONIZACION EN EJECUCION
            // ====================================

            if (sincronizando) {

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Sincronizando con el servidor...",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar sincronizacion")
                }

            } else {

                Button(
                    onClick = onSincronizar,
                    enabled = conectado,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        if (conectado) {
                            "Sincronizar ahora"
                        } else {
                            "Sincronizacion no disponible"
                        }
                    )
                }
            }

            // ====================================
            // 12. RESULTADO DE SINCRONIZACION
            // ====================================

            if (!sincronizando) {

                if (errorSincronizacion != null) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            Text(
                                text = "Sincronizacion no completada",
                                fontWeight = FontWeight.Bold,
                                color =
                                    MaterialTheme.colorScheme
                                        .onErrorContainer
                            )

                            Text(
                                text = errorSincronizacion,
                                color =
                                    MaterialTheme.colorScheme
                                        .onErrorContainer
                            )

                            Text(
                                text = "Los datos locales se conservan.",
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onErrorContainer
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onReintentar,
                        enabled = conectado,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reintentar sincronizacion")
                    }

                } else {

                    when (ultimoResultado) {

                        null -> {

                            Text(
                                text = "Ultimo resultado: Sin ejecutar",
                                style =
                                    MaterialTheme.typography.bodySmall
                            )
                        }

                        is ResultadoSincronizacion.Exito -> {

                            Text(
                                text =
                                    "Sincronizacion completada correctamente.",
                                color =
                                    MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        is ResultadoSincronizacion.SinCambios -> {

                            Text(
                                text =
                                    "Sincronizacion completada. " +
                                            "No se encontraron cambios.",
                                color =
                                    MaterialTheme.colorScheme.primary
                            )
                        }

                        is ResultadoSincronizacion.Parcial -> {

                            Text(
                                text = ultimoResultado.mensaje,
                                color =
                                    MaterialTheme.colorScheme.tertiary
                            )

                            OutlinedButton(
                                onClick = onReintentar,
                                enabled = conectado,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reintentar sincronizacion")
                            }
                        }

                        is ResultadoSincronizacion.Error -> {

                            Text(
                                text = ultimoResultado.mensaje,
                                color =
                                    MaterialTheme.colorScheme.error
                            )

                            OutlinedButton(
                                onClick = onReintentar,
                                enabled = conectado,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reintentar sincronizacion")
                            }
                        }
                    }
                }

                // =================================
                // 13. LIMPIAR RESULTADO
                // =================================

                if (
                    ultimoResultado != null ||
                    errorSincronizacion != null
                ) {

                    TextButton(
                        onClick = onLimpiarResultado,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Limpiar resultado")
                    }
                }
            }
        }
    }
}

// ============================================
// 14. PANEL DE PREFERENCIAS
// ============================================

@Composable
private fun PanelPreferencias(
    preferencias: PreferenciasUsuario,
    onConfiguracionClick: () -> Unit
) {

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = "Preferencias activas",
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Orden: ${preferencias.orden.nombre}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text =
                    "Mostrar completados: " +
                            if (preferencias.mostrarCompletados) {
                                "Si"
                            } else {
                                "No"
                            },
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text =
                    "Tema: " +
                            if (preferencias.temaOscuro) {
                                "Oscuro"
                            } else {
                                "Claro"
                            },
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(
                onClick = onConfiguracionClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Modificar preferencias")
            }
        }
    }
}

// ============================================
// 15. CONTROLES DE SIMULACION
// ============================================

@Composable
private fun PanelSimulaciones(
    viewModel: RecordatoriosViewModel
) {

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = "Controles de prueba",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                TextButton(
                    onClick = {
                        viewModel.simularLoading()
                    }
                ) {
                    Text("Loading")
                }

                TextButton(
                    onClick = {
                        viewModel.vaciarRecordatorios()
                    }
                ) {
                    Text("Empty")
                }

                TextButton(
                    onClick = {
                        viewModel.provocarError()
                    }
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
        }
    }
}

// ============================================
// 16. PANEL LISTADO VACIO
// ============================================

@Composable
private fun PanelListadoVacio(
    mensaje: String,
    textoBoton: String,
    onClick: () -> Unit
) {

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = onClick
            ) {
                Text(textoBoton)
            }
        }
    }
}

// ============================================
// 17. PANEL ERROR LOCAL
// ============================================

@Composable
private fun PanelErrorLocal(
    mensaje: String,
    onReintentar: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.errorContainer
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Error al cargar datos locales",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(
                onClick = onReintentar
            ) {
                Text("Reintentar carga local")
            }
        }
    }
}

// ============================================
// 18. TARJETA DE RECORDATORIO
// ============================================

@Composable
private fun RecordatorioCard(
    recordatorio: Recordatorio,
    onClick: () -> Unit
) {

    val colorPrioridad =
        when (recordatorio.prioridad) {

            Prioridad.Alta ->
                MaterialTheme.colorScheme.error

            Prioridad.Media ->
                MaterialTheme.colorScheme.tertiary

            Prioridad.Baja ->
                MaterialTheme.colorScheme.primary
        }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = recordatorio.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (!recordatorio.descripcion.isNullOrBlank()) {

                Text(
                    text = recordatorio.descripcion.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        "Categoria: " +
                                recordatorio.categoria.nombre,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text =
                        "Prioridad: " +
                                recordatorio.prioridad.nombre,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = colorPrioridad
                )
            }

            Text(
                text =
                    "Estado: " +
                            recordatorio.estado.nombre,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ============================================
// 19. APLICAR PREFERENCIAS
// ============================================

private fun aplicarPreferencias(
    recordatorios: List<Recordatorio>,
    preferencias: PreferenciasUsuario
): List<Recordatorio> {

    val filtrados =
        if (preferencias.mostrarCompletados) {

            recordatorios

        } else {

            recordatorios.filter { recordatorio ->

                recordatorio.estado !=
                        EstadoRecordatorio.Completado
            }
        }

    return when (preferencias.orden) {

        OrdenRecordatorios.CREACION -> {

            filtrados
        }

        OrdenRecordatorios.TITULO -> {

            filtrados.sortedBy { recordatorio ->

                recordatorio.titulo.lowercase()
            }
        }

        OrdenRecordatorios.PRIORIDAD -> {

            filtrados.sortedBy { recordatorio ->

                when (recordatorio.prioridad) {

                    Prioridad.Alta -> 0

                    Prioridad.Media -> 1

                    Prioridad.Baja -> 2
                }
            }
        }
    }
}
