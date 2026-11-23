package com.example.practica15

import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Se ejecuta automáticamente si el Token del dispositivo cambia
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Nuevo token generado: $token")
        EstadoFcm.token.value = token
    }

    // Se ejecuta cuando recibimos un mensaje mientras la app está abierta
    // (o siempre, si el mensaje solo trae "data")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val datos = remoteMessage.data
        Log.d("FCM_MENSAJE", "Mensaje recibido. Data payload: $datos")

        // Deep Link: puede venir completo en "deep_link" o se arma con los datos del payload
        fun deepLinkPara(titulo: String, mensaje: String) =
            datos["deep_link"]?.toUri() ?: construirDeepLink(titulo, mensaje, "push")

        // 1. Si el mensaje trae un "Notification Message" tradicional
        val notificacion = remoteMessage.notification
        if (notificacion != null) {
            val titulo = notificacion.title ?: "Alerta"
            val cuerpo = notificacion.body ?: ""
            Log.d("FCM_MENSAJE", "Cuerpo de la notificación: $cuerpo")
            EstadoFcm.ultimoMensaje.value = "$titulo: $cuerpo"
            mostrarNotificacion(this, CANAL_URGENTE, titulo, cuerpo, deepLinkPara(titulo, cuerpo))
            return
        }

        // 2. Si el mensaje solo trae información oculta tipo JSON (Data Payload)
        if (datos.isNotEmpty()) {
            val tituloEspecial = datos["titulo_oculto"] ?: "Mensaje Oculto"
            val mensajeEspecial = datos["mensaje_oculto"] ?: "Tienes datos nuevos"
            EstadoFcm.ultimoMensaje.value = "$tituloEspecial: $mensajeEspecial"
            mostrarNotificacion(
                this,
                CANAL_URGENTE,
                tituloEspecial,
                mensajeEspecial,
                deepLinkPara(tituloEspecial, mensajeEspecial)
            )
        }
    }
}
