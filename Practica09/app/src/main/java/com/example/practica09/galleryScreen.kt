package com.example.practica09

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

// Lista de URLs de imágenes de prueba (Lorem Picsum)
private val imagenesGaleria = listOf(
    "https://picsum.photos/id/1018/400/400",
    "https://picsum.photos/id/1015/400/400",
    "https://picsum.photos/id/1019/400/400",
    "https://picsum.photos/id/1020/400/400",
    "https://picsum.photos/id/1021/400/400",
    "https://picsum.photos/id/1022/400/400",
    "https://picsum.photos/id/1023/400/400",
    "https://picsum.photos/id/1024/400/400",
    "https://picsum.photos/id/1025/400/400",
    "https://picsum.photos/id/1035/400/400",
    "https://picsum.photos/id/1036/400/400",
    "https://picsum.photos/id/1039/400/400",
    "https://picsum.photos/id/1043/400/400",
    "https://picsum.photos/id/1044/400/400",
    "https://picsum.photos/id/1050/400/400",
    "https://picsum.photos/id/1067/400/400",
    "https://picsum.photos/id/1069/400/400",
    "https://picsum.photos/id/1074/400/400",
    "https://picsum.photos/id/1080/400/400",
    "https://picsum.photos/id/1084/400/400",
    // URL inválida a propósito para demostrar el estado de error de Coil
    "https://picsum.photos/id/no-existe/400/400"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaleriaScreen() {
    // Estado: imagen seleccionada para la vista de detalle (null = sin diálogo)
    var imagenSeleccionada by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Galería") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        // LazyVerticalGrid: solo compone las celdas visibles (scroll fluido y poca memoria)
        LazyVerticalGrid(
            // Cuadrícula adaptativa: 2 columnas en teléfono, más en tablet u horizontal
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(imagenesGaleria, key = { it }) { url ->
                ImagenItem(
                    url = url,
                    onClick = { imagenSeleccionada = url }
                )
            }
        }
    }

    // RETO: vista de detalle al seleccionar una imagen
    imagenSeleccionada?.let { url ->
        ImagenDetalleDialog(
            url = url,
            onDismiss = { imagenSeleccionada = null }
        )
    }
}

@Composable
fun ImagenItem(url: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        // SubcomposeAsyncImage de Coil: descarga, cachea y muestra la imagen sin bloquear la UI
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = "Imagen de la galería",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            // Placeholder mientras carga
            loading = { EstadoCargando() },
            // Placeholder de error si la descarga falla
            error = { EstadoError() }
        )
    }
}

@Composable
fun EstadoCargando() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
    }
}

@Composable
fun EstadoError() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "No se pudo cargar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun ImagenDetalleDialog(url: String, onDismiss: () -> Unit) {
    // Pedimos una versión más grande de la misma imagen para la vista ampliada
    val urlGrande = remember(url) { url.replace("/400/400", "/1080/1080") }
    val idImagen = remember(url) { url.substringAfter("/id/").substringBefore("/") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(urlGrande)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Imagen ampliada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    loading = { EstadoCargando() },
                    error = { EstadoError() }
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
            Text(
                text = "Imagen #$idImagen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp)
            )
            Text(
                text = "Toca fuera de la imagen o la X para cerrar",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
            )
        }
    }
}
