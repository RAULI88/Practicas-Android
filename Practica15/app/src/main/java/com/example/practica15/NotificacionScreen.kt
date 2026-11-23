package com.example.practica15

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionScreen() {
    val context = LocalContext.current

    var permisoConcedido by remember { mutableStateOf(tienePermisoNotificaciones(context)) }
    var mensajeLocal by rememberSaveable { mutableStateOf("¡Hola! Esta es una notificación local de la Práctica 15") }
    val token by EstadoFcm.token.collectAsState()
    val ultimoPush by EstadoFcm.ultimoMensaje.collectAsState()
    var errorToken by remember { mutableStateOf<String?>(null) }

    // Solicitud del permiso POST_NOTIFICATIONS (obligatorio en Android 13+)
    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoConcedido = concedido
        if (!concedido) {
            Toast.makeText(context, "Sin permiso no se mostrarán notificaciones", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!permisoConcedido && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Obtenemos el FCM Token del dispositivo y lo imprimimos en Logcat
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
            if (tarea.isSuccessful) {
                val t = tarea.result
                Log.d("FCM_TOKEN", t)
                EstadoFcm.token.value = t
            } else {
                errorToken = tarea.exception?.message ?: "No se pudo obtener el token"
                Log.e("FCM_TOKEN", "Error al obtener el token", tarea.exception)
            }
        }
    }

    fun enviarLocal(canal: String, titulo: String) {
        if (!permisoConcedido) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permisoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }
        mostrarNotificacion(
            context = context,
            idCanal = canal,
            titulo = titulo,
            mensaje = mensajeLocal,
            // Al tocarla abre la pantalla de detalle mediante el Deep Link
            deepLink = construirDeepLink(titulo, mensajeLocal, "local")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones Locales y Push") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---------- Estado del permiso ----------
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (permisoConcedido) Color(0x1A2E7D32) else Color(0x1AC62828)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (permisoConcedido) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (permisoConcedido) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (permisoConcedido) "Permiso de notificaciones concedido"
                        else "Permiso de notificaciones denegado",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!permisoConcedido && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        TextButton(onClick = { permisoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Pedir")
                        }
                    }
                }
            }

            // ---------- PARTE A: Notificaciones locales ----------
            Text("Parte A · Notificaciones locales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = mensajeLocal,
                onValueChange = { mensajeLocal = it },
                label = { Text("Mensaje de la notificación") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { enviarLocal(CANAL_URGENTE, "Alerta urgente") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Urgente (importancia ALTA)")
            }
            FilledTonalButton(
                onClick = { enviarLocal(CANAL_GENERAL, "Recordatorio") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Recordatorio (importancia NORMAL)") }
            OutlinedButton(
                onClick = { enviarLocal(CANAL_SILENCIOSO, "Aviso silencioso") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Silenciosa (importancia BAJA)") }
            Text(
                "Al tocar la notificación, el PendingIntent abre la app directamente en la pantalla de detalle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            HorizontalDivider()

            // ---------- PARTE B: Firebase Cloud Messaging ----------
            Text("Parte B · Firebase Cloud Messaging", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("FCM Token del dispositivo", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.padding(2.dp))
                    when {
                        token != null -> SelectionContainer {
                            Text(token ?: "", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                        errorToken != null -> Text("Error: $errorToken", color = MaterialTheme.colorScheme.error)
                        else -> Text("Obteniendo token...")
                    }
                    Row {
                        TextButton(
                            enabled = token != null,
                            onClick = { token?.let { copiarAlPortapapeles(context, it) } }
                        ) { Text("Copiar token") }
                        TextButton(
                            enabled = token != null,
                            onClick = {
                                Log.d("FCM_TOKEN", token ?: "")
                                Toast.makeText(context, "Token impreso en Logcat (FCM_TOKEN)", Toast.LENGTH_SHORT).show()
                            }
                        ) { Text("Imprimir en Logcat") }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Último push recibido (app abierta)", fontWeight = FontWeight.Bold)
                        Text(ultimoPush ?: "Ninguno todavía")
                    }
                }
            }

            Text(
                "Prueba desde Firebase Console > Messaging > \"Enviar mensaje de prueba\" pegando el token. " +
                    "En \"Opciones adicionales > Datos personalizados\" agrega titulo_oculto y mensaje_oculto: " +
                    "al tocar la notificación se abrirá la pantalla de detalle con esos datos (Deep Link).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun tienePermisoNotificaciones(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun copiarAlPortapapeles(context: Context, texto: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("FCM Token", texto))
    Toast.makeText(context, "Token copiado", Toast.LENGTH_SHORT).show()
}
