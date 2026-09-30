package com.example.practica05

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "form_screen") {

        composable("form_screen") {
            FormScreen(onNavigateToDetail = { matricula, nombre, carrera, turno, estatus ->
                val safeNombre = nombre.replace("/", "-").ifEmpty { "SinNombre" }
                val safeCarrera = carrera.replace("/", "-")
                navController.navigate("detail_screen/$matricula/$safeNombre/$safeCarrera/$turno/$estatus")
            })
        }

        composable(
            route = "detail_screen/{matricula}/{nombre}/{carrera}/{turno}/{estatus}",
            arguments = listOf(
                navArgument("matricula") { type = NavType.StringType },
                navArgument("nombre") { type = NavType.StringType },
                navArgument("carrera") { type = NavType.StringType },
                navArgument("turno") { type = NavType.StringType },
                navArgument("estatus") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            DetailScreen(
                matricula = args?.getString("matricula") ?: "",
                nombre = args?.getString("nombre") ?: "",
                carrera = args?.getString("carrera") ?: "",
                turno = args?.getString("turno") ?: "",
                estatusActivo = args?.getBoolean("estatus") ?: true,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}