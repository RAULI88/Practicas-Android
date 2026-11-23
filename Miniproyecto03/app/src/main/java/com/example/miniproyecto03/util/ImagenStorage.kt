package com.example.miniproyecto03.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Utilidades para guardar las fotos dentro de la memoria interna de la app
object ImagenStorage {

    private fun carpeta(context: Context): File =
        File(context.filesDir, "imagenes").apply { if (!exists()) mkdirs() }

    /** Crea un archivo vacío para que la cámara escriba la foto. */
    fun crearArchivoFoto(context: Context): File =
        File(carpeta(context), "IMG_${System.currentTimeMillis()}.jpg")

    /** URI segura (content://) del FileProvider para pasársela a la app de cámara. */
    fun uriParaCamara(context: Context, archivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

    /**
     * Copia la imagen elegida de la galería a la carpeta interna.
     * Así la publicación conserva su foto aunque se borre la original o se pierda el permiso.
     */
    fun copiarDesdeGaleria(context: Context, origen: Uri): File? = try {
        val destino = crearArchivoFoto(context)
        context.contentResolver.openInputStream(origen)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        }
        if (destino.length() > 0) destino else null
    } catch (e: Exception) {
        null
    }

    fun aUriString(archivo: File): String = archivo.toUri().toString()

    /** Borra el archivo físico de una foto guardada (file://...). */
    fun eliminar(uriString: String) {
        runCatching { uriString.toUri().path?.let { File(it).delete() } }
    }

    fun formatearFecha(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.forLanguageTag("es-MX")).format(Date(millis))
}
