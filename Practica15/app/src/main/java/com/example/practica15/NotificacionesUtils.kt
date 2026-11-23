package com.example.practica15

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow

// Identificadores de los canales (cada uno con su nivel de importancia)
const val CANAL_URGENTE = "canal_practica_15"     // IMPORTANCE_HIGH  -> sonido + aviso flotante
const val CANAL_GENERAL = "canal_recordatorios"   // IMPORTANCE_DEFAULT -> sonido
const val CANAL_SILENCIOSO = "canal_silencioso"   // IMPORTANCE_LOW -> sin sonido

// Esquema del Deep Link declarado en el AndroidManifest
const val ESQUEMA_DEEP_LINK = "practica15"
const val HOST_DETALLE = "detalle"

/** Crea los canales. Desde Android 8.0 (API 26) toda notificación debe pertenecer a uno. */
fun crearCanalesNotificacion(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val canales = listOf(
            NotificationChannel(CANAL_URGENTE, "Alertas urgentes", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Canal de notificaciones locales y remotas (Firebase) de alta prioridad"
            },
            NotificationChannel(CANAL_GENERAL, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos normales de la aplicación"
            },
            NotificationChannel(CANAL_SILENCIOSO, "Silenciosas", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Información sin sonido ni vibración"
            }
        )
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(canales)
    }
}

/** Construye el Deep Link practica15://detalle?titulo=...&mensaje=...&origen=... */
fun construirDeepLink(titulo: String, mensaje: String, origen: String): Uri =
    Uri.Builder()
        .scheme(ESQUEMA_DEEP_LINK)
        .authority(HOST_DETALLE)
        .appendQueryParameter("titulo", titulo)
        .appendQueryParameter("mensaje", mensaje)
        .appendQueryParameter("origen", origen)
        .build()

/**
 * Muestra una notificación. El PendingIntent le da permiso al sistema para abrir
 * nuestra MainActivity (con el Deep Link) cuando el usuario toque la alerta.
 */
@SuppressLint("MissingPermission")
fun mostrarNotificacion(
    context: Context,
    idCanal: String,
    titulo: String,
    mensaje: String,
    deepLink: Uri? = null
) {
    // Sin permiso (Android 13+) o con notificaciones desactivadas, no se puede mostrar
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

    val intent = Intent(context, MainActivity::class.java).apply {
        if (deepLink != null) {
            action = Intent.ACTION_VIEW
            data = deepLink
        }
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val idNotificacion = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    // FLAG_IMMUTABLE es obligatorio desde Android 12
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
        context,
        idNotificacion,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val prioridad = when (idCanal) {
        CANAL_URGENTE -> NotificationCompat.PRIORITY_HIGH
        CANAL_SILENCIOSO -> NotificationCompat.PRIORITY_LOW
        else -> NotificationCompat.PRIORITY_DEFAULT
    }

    val builder = NotificationCompat.Builder(context, idCanal)
        .setSmallIcon(R.drawable.ic_notificacion)
        .setContentTitle(titulo)
        .setContentText(mensaje)
        .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
        .setPriority(prioridad)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    NotificationManagerCompat.from(context).notify(idNotificacion, builder.build())
}

/** Puente sencillo entre el servicio de Firebase y la interfaz de Compose. */
object EstadoFcm {
    val token = MutableStateFlow<String?>(null)
    val ultimoMensaje = MutableStateFlow<String?>(null)
}
