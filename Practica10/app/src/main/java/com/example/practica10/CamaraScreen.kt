package com.example.practica10

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

// Estados posibles del permiso de cámara
enum class EstadoPermiso { NO_SOLICITADO, CONCEDIDO, DENEGADO, DENEGADO_PERMANENTE }

// Acción que el usuario quería hacer antes de pedir el permiso
private enum class AccionPendiente { NINGUNA, FOTO_HD, FOTO_RAPIDA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamaraScreen() {
    val context = LocalContext.current

    // ---------- Estado ----------
    var estadoPermiso by remember { mutableStateOf(estadoInicialPermiso(context)) }
    var accionPendiente by rememberSaveable { mutableStateOf(AccionPendiente.NINGUNA) }
    // URI donde la cámara escribirá la foto HD (se guarda por si Android recrea la Activity)
    var uriPendiente by rememberSaveable { mutableStateOf<Uri?>(null) }
    var archivoPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    // Última foto en modo rápido (Bitmap en memoria)
    var ultimoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Galería: todas las fotos guardadas en la carpeta privada de la app
    val fotos = remember { mutableStateListOf<File>().apply { addAll(listarFotos(context)) } }
    var fotoAmpliada by remember { mutableStateOf<File?>(null) }

    // ---------- Contratos de Activity Result ----------

    // 1) TakePicture: la app de cámara guarda la foto en alta calidad en la URI del FileProvider
    val tomarFotoHd = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        val ruta = archivoPendiente
        if (exito && ruta != null) {
            val archivo = File(ruta)
            fotos.add(0, archivo)
            ultimoBitmap = null
            Toast.makeText(context, "Foto guardada", Toast.LENGTH_SHORT).show()
        } else {
            // Si el usuario canceló, borramos el archivo vacío
            ruta?.let { File(it).delete() }
            Toast.makeText(context, "Captura cancelada", Toast.LENGTH_SHORT).show()
        }
        uriPendiente = null
        archivoPendiente = null
    }

    // 2) TakePicturePreview: devuelve directamente un Bitmap (miniatura)
    val tomarFotoRapida = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            ultimoBitmap = bitmap
            // También lo guardamos como archivo para que aparezca en la galería
            guardarBitmap(context, bitmap)?.let { fotos.add(0, it) }
        } else {
            Toast.makeText(context, "Captura cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    fun lanzarFotoHd() {
        val archivo = crearArchivoFoto(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        archivoPendiente = archivo.absolutePath
        uriPendiente = uri
        tomarFotoHd.launch(uri)
    }

    fun ejecutar(accion: AccionPendiente) {
        when (accion) {
            AccionPendiente.FOTO_HD -> lanzarFotoHd()
            AccionPendiente.FOTO_RAPIDA -> tomarFotoRapida.launch(null)
            AccionPendiente.NINGUNA -> Unit
        }
    }

    // 3) RequestPermission: solicitud del permiso CAMERA en tiempo de ejecución
    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            estadoPermiso = EstadoPermiso.CONCEDIDO
            ejecutar(accionPendiente)
        } else {
            val activity = context.findActivity()
            val mostrarExplicacion = activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            estadoPermiso = if (mostrarExplicacion) EstadoPermiso.DENEGADO else EstadoPermiso.DENEGADO_PERMANENTE
            Toast.makeText(context, "Sin permiso no se puede usar la cámara", Toast.LENGTH_SHORT).show()
        }
        accionPendiente = AccionPendiente.NINGUNA
    }

    // RETO: antes de abrir la cámara verificamos el permiso; si falta, lo pedimos
    fun verificarPermisoY(accion: AccionPendiente) {
        if (tienePermisoCamara(context)) {
            estadoPermiso = EstadoPermiso.CONCEDIDO
            ejecutar(accion)
        } else {
            accionPendiente = accion
            solicitarPermiso.launch(Manifest.permission.CAMERA)
        }
    }

    // ---------- UI ----------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cámara y Permisos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    if (fotos.isNotEmpty()) {
                        IconButton(onClick = {
                            fotos.forEach { it.delete() }
                            fotos.clear()
                            ultimoBitmap = null
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Borrar todas")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { verificarPermisoY(AccionPendiente.FOTO_HD) },
                icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                text = { Text("Tomar foto") }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tarjeta con el estado del permiso
            item(span = { GridItemSpan(maxLineSpan) }) {
                TarjetaPermiso(
                    estado = estadoPermiso,
                    onSolicitar = { verificarPermisoY(AccionPendiente.NINGUNA) },
                    onAbrirAjustes = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
            }

            // Botones de captura
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Button(
                        onClick = { verificarPermisoY(AccionPendiente.FOTO_HD) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Foto HD")
                    }
                    OutlinedButton(
                        onClick = { verificarPermisoY(AccionPendiente.FOTO_RAPIDA) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Foto rápida")
                    }
                }
            }

            // Vista previa de la última foto capturada
            item(span = { GridItemSpan(maxLineSpan) }) {
                VistaPrevia(ultimoBitmap = ultimoBitmap, ultimaFoto = fotos.firstOrNull())
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Galería (${fotos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            items(fotos, key = { it.absolutePath }) { archivo ->
                AsyncImage(
                    model = archivo,
                    contentDescription = "Foto capturada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { fotoAmpliada = archivo }
                )
            }

            // Espacio para que el FAB no tape la última fila
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(80.dp)) }
        }
    }

    fotoAmpliada?.let { archivo ->
        Dialog(onDismissRequest = { fotoAmpliada = null }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                AsyncImage(
                    model = archivo,
                    contentDescription = "Foto ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { fotoAmpliada = null },
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun TarjetaPermiso(
    estado: EstadoPermiso,
    onSolicitar: () -> Unit,
    onAbrirAjustes: () -> Unit
) {
    val (texto, color, icono) = when (estado) {
        EstadoPermiso.CONCEDIDO -> Triple("Permiso de cámara concedido", Color(0xFF2E7D32), Icons.Default.CheckCircle)
        EstadoPermiso.NO_SOLICITADO -> Triple("Permiso de cámara no solicitado", Color(0xFF616161), Icons.Default.Security)
        EstadoPermiso.DENEGADO -> Triple("Permiso denegado: la cámara no se puede usar", Color(0xFFC62828), Icons.Default.Error)
        EstadoPermiso.DENEGADO_PERMANENTE -> Triple("Permiso bloqueado. Actívalo desde Ajustes", Color(0xFFC62828), Icons.Default.Error)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = color)
            Spacer(Modifier.width(10.dp))
            Text(texto, modifier = Modifier.weight(1f), color = color, fontWeight = FontWeight.SemiBold)
        }
        when (estado) {
            EstadoPermiso.NO_SOLICITADO, EstadoPermiso.DENEGADO ->
                TextButton(onClick = onSolicitar, modifier = Modifier.padding(start = 4.dp)) {
                    Text("Solicitar permiso")
                }
            EstadoPermiso.DENEGADO_PERMANENTE ->
                TextButton(onClick = onAbrirAjustes, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Abrir ajustes")
                }
            EstadoPermiso.CONCEDIDO -> Unit
        }
    }
}

@Composable
private fun VistaPrevia(ultimoBitmap: Bitmap?, ultimaFoto: File?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                // Foto rápida: se renderiza el Bitmap directamente
                ultimoBitmap != null -> Image(
                    bitmap = ultimoBitmap.asImageBitmap(),
                    contentDescription = "Última foto (Bitmap)",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Foto HD: se carga el archivo guardado
                ultimaFoto != null -> AsyncImage(
                    model = ultimaFoto,
                    contentDescription = "Última foto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text("Aún no hay fotos", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// ---------- Utilidades ----------

private fun tienePermisoCamara(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private fun estadoInicialPermiso(context: Context): EstadoPermiso =
    if (tienePermisoCamara(context)) EstadoPermiso.CONCEDIDO else EstadoPermiso.NO_SOLICITADO

private fun carpetaFotos(context: Context): File =
    File(context.filesDir, "imagenes").apply { if (!exists()) mkdirs() }

private fun crearArchivoFoto(context: Context): File =
    File(carpetaFotos(context), "foto_${System.currentTimeMillis()}.jpg")

private fun listarFotos(context: Context): List<File> =
    carpetaFotos(context).listFiles { f -> f.extension == "jpg" && f.length() > 0 }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

private fun guardarBitmap(context: Context, bitmap: Bitmap): File? = try {
    val archivo = crearArchivoFoto(context)
    FileOutputStream(archivo).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    archivo
} catch (e: Exception) {
    null
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
