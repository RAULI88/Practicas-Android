package com.example.miniproyecto03

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.miniproyecto03.ui.screens.FeedScreen
import com.example.miniproyecto03.ui.screens.NuevaPublicacionScreen
import com.example.miniproyecto03.ui.theme.Miniproyecto03Theme
import com.example.miniproyecto03.ui.viewmodel.PublicacionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Miniproyecto03Theme {
                RedSocialApp()
            }
        }
    }
}

@Composable
fun RedSocialApp() {
    val navController = rememberNavController()
    // Un solo ViewModel compartido por las dos pantallas (vive mientras viva la Activity)
    val viewModel: PublicacionViewModel = viewModel(factory = PublicacionViewModel.Factory)

    NavHost(navController = navController, startDestination = "feed") {
        // Pantalla de listado
        composable("feed") {
            FeedScreen(
                viewModel = viewModel,
                onNuevaPublicacion = { navController.navigate("nueva") }
            )
        }
        // Formulario de alta
        composable("nueva") {
            NuevaPublicacionScreen(
                onPublicar = { titulo, descripcion, fotoUri, fecha ->
                    viewModel.publicar(titulo, descripcion, fotoUri, fecha)
                    navController.popBackStack()
                },
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
