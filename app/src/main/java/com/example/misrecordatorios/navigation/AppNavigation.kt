
package com.example.misrecordatorios.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.misrecordatorios.ui.screens.ListadoScreen
import com.example.misrecordatorios.ui.screens.CrearRecordatorioScreen
import com.example.misrecordatorios.ui.screens.DetalleRecordatorioScreen
import com.example.misrecordatorios.ui.screens.ConfiguracionScreen
import com.example.misrecordatorios.viewmodel.RecordatoriosViewModel
import com.example.misrecordatorios.viewmodel.PreferenciasViewModel

@Composable
fun AppNavigation(

    preferenciasViewModel: PreferenciasViewModel =
        viewModel()

) {

    val navController = rememberNavController()

    // ViewModel compartido de recordatorios
    val recordatoriosViewModel: RecordatoriosViewModel =
        viewModel()

    // API 3: observar preferencias persistentes
    val preferencias by preferenciasViewModel
        .preferencias
        .collectAsStateWithLifecycle()

    NavHost(

        navController = navController,

        startDestination = "listado"

    ) {

        // =====================================
        // PANTALLA 1: LISTADO
        // =====================================

        composable("listado") {

            ListadoScreen(

                viewModel = recordatoriosViewModel,

                onCrearClick = {

                    navController.navigate("crear")
                },

                onDetalleClick = { recordatorioId ->

                    navController.navigate(
                        "detalle/$recordatorioId"
                    )
                },

                // API 3: preferencias activas
                preferencias = preferencias,

                // API 3: acceso a configuracion
                onConfiguracionClick = {

                    navController.navigate(
                        "configuracion"
                    )
                }
            )
        }

        // =====================================
        // PANTALLA 2: CREAR RECORDATORIO
        // =====================================

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

        // =====================================
        // PANTALLA 3: DETALLE
        // =====================================

        composable(

            route = "detalle/{recordatorioId}",

            arguments = listOf(

                navArgument("recordatorioId") {

                    type = NavType.IntType
                }
            )

        ) { backStackEntry ->

            val recordatorioId =
                backStackEntry.arguments
                    ?.getInt("recordatorioId")
                    ?: return@composable

            DetalleRecordatorioScreen(

                recordatorioId = recordatorioId,

                viewModel = recordatoriosViewModel,

                onVolver = {

                    navController.popBackStack()
                }
            )
        }

        // =====================================
        // PANTALLA 4: CONFIGURACION
        // =====================================

        composable("configuracion") {

            ConfiguracionScreen(

                viewModel = preferenciasViewModel,

                onVolver = {

                    navController.popBackStack()
                }
            )
        }
    }
}
 