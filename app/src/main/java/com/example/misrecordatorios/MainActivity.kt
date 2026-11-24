
package com.example.misrecordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.misrecordatorios.navigation.AppNavigation
import com.example.misrecordatorios.viewmodel.PreferenciasViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            // API 3: ViewModel compartido de preferencias
            val preferenciasViewModel: PreferenciasViewModel =
                viewModel()

            // Observar las preferencias guardadas en DataStore
            val preferencias by preferenciasViewModel
                .preferencias
                .collectAsStateWithLifecycle()

            // Seleccionar el esquema de colores
            // segun las preferencias del usuario
            val esquemaColores = if (
                preferencias.temaOscuro
            ) {

                darkColorScheme()

            } else {

                lightColorScheme()
            }

            // Aplicar el tema a toda la aplicacion
            MaterialTheme(

                colorScheme = esquemaColores

            ) {

                Surface(

                    modifier = Modifier.fillMaxSize(),

                    color = MaterialTheme.colorScheme.background

                ) {

                    // Compartir el mismo ViewModel
                    // con el sistema de navegacion
                    AppNavigation(

                        preferenciasViewModel =
                            preferenciasViewModel
                    )
                }
            }
        }
    }
}
