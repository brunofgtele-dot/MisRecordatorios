
package com.example.misrecordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                PantallaRecordatorios()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRecordatorios() {

    var filtroEstado by remember {
        mutableStateOf("Todos")
    }

    var filtroPrioridad by remember {
        mutableStateOf("Todas")
    }

    val recordatoriosFiltrados =
        DatosSimulados.recordatorios.filter {
            (filtroEstado == "Todos" ||
                    it.estado.nombre == filtroEstado) &&
                    (filtroPrioridad == "Todas" ||
                            it.prioridad.nombre == filtroPrioridad)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mis Recordatorios")
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Text(
                text = "Lista de tareas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Filtrar por estado")

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todos", "Pendiente", "Completado")
                    .forEach { estado ->

                        FilterChip(
                            selected = filtroEstado == estado,
                            onClick = {
                                filtroEstado = estado
                            },
                            label = {
                                Text(estado)
                            }
                        )
                    }
            }

            Text("Filtrar por prioridad")

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todas", "Alta", "Media", "Baja")
                    .forEach { prioridad ->

                        FilterChip(
                            selected = filtroPrioridad == prioridad,
                            onClick = {
                                filtroPrioridad = prioridad
                            },
                            label = {
                                Text(prioridad)
                            }
                        )
                    }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Recordatorios: ${recordatoriosFiltrados.size}",
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordatoriosFiltrados) { recordatorio ->
                    TarjetaRecordatorio(recordatorio)
                }
            }
        }
    }
}

@Composable
fun TarjetaRecordatorio(recordatorio: Recordatorio) {

    val colorCategoria = Color(
        DatosSimulados.coloresCategorias[
            recordatorio.categoria
        ] ?: 0xFF757575
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = recordatorio.titulo,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = recordatorio.descripcion ?: "",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Categoría: ${recordatorio.categoria.nombre}",
                color = colorCategoria,
                fontWeight = FontWeight.Medium
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
 