package com.example.practica11

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// Ubicaciones de destino donde se pueden guardar los archivos
enum class Ubicacion(val etiqueta: String) {
    INTERNA("Memoria interna"),
    EXTERNA("Almacenamiento externo"),
    SD_FISICA("SDCard física")
}

object ArchivoUtils {

    /** Directorio que corresponde a cada ubicación (null si no está disponible). */
    fun directorio(context: Context, ubicacion: Ubicacion): File? = when (ubicacion) {
        // /data/data/<paquete>/files -> privado, no requiere permisos
        Ubicacion.INTERNA -> context.filesDir
        // /storage/emulated/0/Android/data/<paquete>/files -> almacenamiento externo de la app
        Ubicacion.EXTERNA -> if (externaMontada()) context.getExternalFilesDir(null) else null
        // /storage/XXXX-XXXX/Android/data/<paquete>/files -> tarjeta SD extraíble (si existe)
        Ubicacion.SD_FISICA -> sdFisica(context)
    }

    /** Verifica que el almacenamiento externo esté montado (lectura/escritura). */
    fun externaMontada(): Boolean =
        Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    /** Devuelve la carpeta de la app en la SD extraíble, si el dispositivo/emulador tiene una. */
    fun sdFisica(context: Context): File? {
        val dirs = context.getExternalFilesDirs(null)
        if (dirs.size < 2) return null
        val sd = dirs[1] ?: return null
        return if (Environment.getExternalStorageState(sd) == Environment.MEDIA_MOUNTED) sd else null
    }

    /** Escribe texto con FileOutputStream. agregar = true añade al final sin borrar lo anterior. */
    fun escribir(dir: File, nombre: String, contenido: String, agregar: Boolean): File {
        val archivo = File(dir, nombre)
        FileOutputStream(archivo, agregar).bufferedWriter(Charsets.UTF_8).use { writer ->
            if (agregar && archivo.length() > 0) writer.newLine()
            writer.write(contenido)
        }
        return archivo
    }

    /** Lee el texto completo de un archivo con FileInputStream + BufferedReader. */
    fun leer(dir: File, nombre: String): String {
        val archivo = File(dir, nombre)
        return FileInputStream(archivo).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    fun eliminar(dir: File, nombre: String): Boolean = File(dir, nombre).delete()

    fun listar(dir: File?): List<File> =
        dir?.listFiles()?.filter { it.isFile }?.sortedBy { it.name.lowercase() } ?: emptyList()

    // ---------------- Permisos ----------------

    /**
     * Permisos de almacenamiento que se piden en tiempo de ejecución según la versión de Android:
     *  - Android 9 o menor (API <= 28): WRITE_EXTERNAL_STORAGE + READ_EXTERNAL_STORAGE
     *  - Android 10 a 12 (API 29-32): READ_EXTERNAL_STORAGE (Scoped Storage limita la escritura)
     *  - Android 13+ (API 33+): ya no existen; las carpetas propias de la app no los necesitan
     */
    fun permisosRequeridos(): Array<String> = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        else -> emptyArray()
    }

    fun tienePermisos(context: Context): Boolean = permisosRequeridos().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
