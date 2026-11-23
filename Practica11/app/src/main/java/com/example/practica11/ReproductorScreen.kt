package com.example.practica11

import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ReproductorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Estados para la URI del archivo, el reproductor y si está sonando
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var nombreArchivo by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Lanzador para abrir el explorador de archivos (Storage Access Framework)
    val selectorAudio = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Liberar el reproductor anterior si existía para evitar fugas de memoria
            mediaPlayer?.release()

            // Crear un nuevo MediaPlayer con el archivo seleccionado
            val nuevo = MediaPlayer.create(context, uri)
            if (nuevo == null) {
                Toast.makeText(context, "No se pudo abrir el audio", Toast.LENGTH_SHORT).show()
                mediaPlayer = null
                isPlaying = false
                return@rememberLauncherForActivityResult
            }
            audioUri = uri
            nombreArchivo = obtenerNombre(context, uri)
            mediaPlayer = nuevo
            nuevo.start()
            isPlaying = true

            // Escuchar cuando la canción termine automáticamente
            nuevo.setOnCompletionListener {
                isPlaying = false
            }
        }
    }

    // DisposableEffect limpia los recursos del MediaPlayer si el usuario sale de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (audioUri == null) "Ningún archivo seleccionado" else "Audio cargado correctamente",
            style = MaterialTheme.typography.bodyLarge
        )
        nombreArchivo?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = when {
                audioUri == null -> ""
                isPlaying -> "Reproduciendo..."
                else -> "En pausa / detenido"
            },
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para abrir el explorador y filtrar archivos "solo audios"
        Button(onClick = { selectorAudio.launch("audio/*") }) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null)
            Text("  Buscar MP3 en SD/Memoria")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Controles de reproducción
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FloatingActionButton(
                onClick = {
                    if (mediaPlayer != null && !isPlaying) {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                }
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir")
            }

            FloatingActionButton(
                onClick = {
                    if (mediaPlayer != null && isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    }
                }
            ) {
                Icon(Icons.Default.Pause, contentDescription = "Pausar")
            }

            // RETO: botón Detener -> pausa y regresa el audio al segundo cero
            FloatingActionButton(
                onClick = {
                    if (mediaPlayer != null) {
                        mediaPlayer?.pause()
                        mediaPlayer?.seekTo(0)
                        isPlaying = false
                    }
                },
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Detener")
            }
        }
    }
}

// Obtiene el nombre visible del archivo a partir de su URI
private fun obtenerNombre(context: android.content.Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
