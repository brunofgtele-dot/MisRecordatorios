
package com.example.misrecordatorios.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.example.misrecordatorios.ui.screens.ListadoScreen
import com.example.misrecordatorios.ui.screens.CrearRecordatorioScreen
import com.example.misrecordatorios.ui.screens.DetalleRecordatorioScreen
import com.example.misrecordatorios.viewmodel.RecordatoriosViewModel

@Composable
fun AppNavigation() {

    // Controlador de navegación
    val navController = rememberNavController()

    // Una instancia compartida para las tres pantallas
    val recordatoriosViewModel: RecordatoriosViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "listado"
    ) {

        // PANTALLA 1: LISTADO
        composable("listado") {

            ListadoScreen(
                viewModel = recordatoriosViewModel,

                onCrearClick = {
                    recordatoriosViewModel.reiniciarGuardado()
                    navController.navigate("crear")
                },

                onDetalleClick = { id ->
                    navController.navigate("detalle/$id")
                }
            )
        }

        // PANTALLA 2: CREAR
        composable("crear") {

            CrearRecordatorioScreen(
                viewModel = recordatoriosViewModel,

                onGuardarExitoso = {
                    navController.popBackStack()
                },

                onCancelar = {
                    navController.popBackStack()
                }
            )
        }

        // PANTALLA 3: DETALLE
        composable(
            route = "detalle/{recordatorioId}",
            arguments = listOf(
                navArgument("recordatorioId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val id = backStackEntry.arguments
                ?.getInt("recordatorioId") ?: -1

            DetalleRecordatorioScreen(
                recordatorioId = id,
                viewModel = recordatoriosViewModel,

                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }
}
 