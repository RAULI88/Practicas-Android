package com.example.practica15

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.practica15.ui.theme.Practica15Theme

class MainActivity : ComponentActivity() {

    // Deep Link pendiente de abrir (viene de una notificación local, un push o un enlace)
    private val deepLinkPendiente = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        crearCanalesNotificacion(this)

        // Solo la primera vez (al rotar la pantalla la navegación ya se restaura sola)
        if (savedInstanceState == null) {
            deepLinkPendiente.value = resolverDeepLink(intent)
        }

        setContent {
            Practica15Theme {
                AppNotificaciones(
                    deepLink = deepLinkPendiente.value,
                    onDeepLinkConsumido = { deepLinkPendiente.value = null }
                )
            }
        }
    }

    // La Activity es singleTop: si ya está abierta, el nuevo Intent llega aquí
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resolverDeepLink(intent)?.let { deepLinkPendiente.value = it }
    }
}

/**
 * Extrae el Deep Link del Intent:
 *  1) Notificación local o push con la app abierta -> viene en intent.data
 *  2) Push recibido con la app en segundo plano -> Android pone el Data Payload en los extras
 */
fun resolverDeepLink(intent: Intent?): Uri? {
    if (intent == null) return null
    intent.data?.let { uri ->
        if (uri.scheme == ESQUEMA_DEEP_LINK) return uri
    }
    val extras = intent.extras ?: return null
    extras.getString("deep_link")?.let { return it.toUri() }
    val titulo = extras.getString("titulo_oculto")
    val mensaje = extras.getString("mensaje_oculto")
    if (titulo != null || mensaje != null || extras.getString("pantalla") == HOST_DETALLE) {
        return construirDeepLink(titulo ?: "Mensaje push", mensaje ?: "", "push")
    }
    return null
}

@Composable
fun AppNotificaciones(deepLink: Uri?, onDeepLinkConsumido: () -> Unit) {
    val navController = rememberNavController()

    // Cuando llega un Deep Link, navegamos automáticamente al destino específico
    LaunchedEffect(deepLink) {
        if (deepLink != null && deepLink.host == HOST_DETALLE) {
            val titulo = Uri.encode(deepLink.getQueryParameter("titulo") ?: "Sin título")
            val mensaje = Uri.encode(deepLink.getQueryParameter("mensaje") ?: "")
            val origen = Uri.encode(deepLink.getQueryParameter("origen") ?: "enlace")
            navController.navigate("detalle?titulo=$titulo&mensaje=$mensaje&origen=$origen") {
                launchSingleTop = true
            }
            onDeepLinkConsumido()
        }
    }

    NavHost(navController = navController, startDestination = "inicio") {
        composable("inicio") {
            NotificacionScreen()
        }
        composable(
            route = "detalle?titulo={titulo}&mensaje={mensaje}&origen={origen}",
            arguments = listOf(
                navArgument("titulo") { type = NavType.StringType; defaultValue = "Sin título" },
                navArgument("mensaje") { type = NavType.StringType; defaultValue = "" },
                navArgument("origen") { type = NavType.StringType; defaultValue = "enlace" }
            )
        ) { entrada ->
            val args = entrada.arguments
            DetalleScreen(
                titulo = args?.getString("titulo") ?: "Sin título",
                mensaje = args?.getString("mensaje") ?: "",
                origen = args?.getString("origen") ?: "enlace",
                onVolver = {
                    if (!navController.popBackStack()) navController.navigate("inicio")
                }
            )
        }
    }
}
