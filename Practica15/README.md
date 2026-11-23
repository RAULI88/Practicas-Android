# Practica15 — Práctica 15 Notificaciones

Desarrollo Móvil Nativo · Kotlin + Jetpack Compose

Notificaciones locales (NotificationChannel, NotificationManager, PendingIntent) y Firebase Cloud Messaging (token, data payload y deep links).

## Antes de compilar (obligatorio)
1. En https://console.firebase.google.com crea el proyecto **Practica15**.
2. Registra una app Android con el paquete `com.example.practica15`.
3. Descarga `google-services.json` y cópialo en la carpeta `app/` de este proyecto.
4. Sync Now y ejecuta.

## Probar
- **Parte A:** botones de notificación local (canales alta / normal / baja). Al tocarla abre la pantalla Detalle.
- **Parte B:** copia el token (o búscalo en Logcat con `FCM_TOKEN`) y en Firebase Console > Messaging > *Enviar mensaje de prueba* pégalo.
  En *Datos personalizados* agrega `titulo_oculto` y `mensaje_oculto`: al tocar la notificación se navega a Detalle con esos datos.
- **Deep Link por adb:** `adb shell am start -a android.intent.action.VIEW -d "practica15://detalle?titulo=Hola&mensaje=Prueba"`
