package com.example.miniproyecto03.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.miniproyecto03.util.ImagenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaPublicacionScreen(
    onPublicar: (titulo: String, descripcion: String, fotoUri: String, fecha: Long) -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado del formulario (rememberSaveable sobrevive a la rotación y a la app de cámara)
    var titulo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var fotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var fecha by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var fechaManual by rememberSaveable { mutableStateOf(false) }
    var archivoCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var intentoPublicar by rememberSaveable { mutableStateOf(false) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var copiando by remember { mutableStateOf(false) }

    // Si se reemplaza la foto, borramos la anterior para no dejar archivos huérfanos
    fun asignarFoto(nueva: String) {
        fotoUri?.let { ImagenStorage.eliminar(it) }
        fotoUri = nueva
    }

    fun salirSinPublicar() {
        fotoUri?.let { ImagenStorage.eliminar(it) }
        onVolver()
    }
    BackHandler { salirSinPublicar() }

    // ---- Cámara: TakePicture guarda la foto en la URI del FileProvider ----
    val camaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        val ruta = archivoCamara
        if (exito && ruta != null) {
            asignarFoto(ImagenStorage.aUriString(File(ruta)))
        } else {
            ruta?.let { File(it).delete() }
        }
        archivoCamara = null
    }

    fun abrirCamara() {
        val archivo = ImagenStorage.crearArchivoFoto(context)
        archivoCamara = archivo.absolutePath
        camaraLauncher.launch(ImagenStorage.uriParaCamara(context, archivo))
    }

    // ---- Permiso de cámara en tiempo de ejecución ----
    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) abrirCamara()
        else Toast.makeText(context, "Se necesita el permiso de cámara para tomar fotos", Toast.LENGTH_LONG).show()
    }

    fun tomarFoto() {
        val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (tienePermiso) abrirCamara() else permisoLauncher.launch(Manifest.permission.CAMERA)
    }

    // ---- Galería: Photo Picker (no requiere permisos de almacenamiento) ----
    val galeriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            copiando = true
            scope.launch {
                val copia = withContext(Dispatchers.IO) { ImagenStorage.copiarDesdeGaleria(context, uri) }
                copiando = false
                if (copia != null) asignarFoto(ImagenStorage.aUriString(copia))
                else Toast.makeText(context, "No se pudo copiar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val tituloValido = titulo.isNotBlank()
    val descripcionValida = descripcion.isNotBlank()
    val fotoValida = fotoUri != null

    fun publicar() {
        intentoPublicar = true
        val foto = fotoUri
        if (tituloValido && descripcionValida && foto != null) {
            // Si el usuario no eligió fecha, se genera automáticamente en este momento
            val fechaFinal = if (fechaManual) fecha else System.currentTimeMillis()
            onPublicar(titulo, descripcion, foto, fechaFinal)
            fotoUri = null // ya pertenece a la publicación: no se debe borrar al salir
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva publicación") },
                navigationIcon = {
                    IconButton(onClick = { salirSinPublicar() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    TextButton(onClick = { publicar() }) { Text("Publicar", fontWeight = FontWeight.Bold) }
                },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- Fotografía ----------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        copiando -> CircularProgressIndicator()
                        fotoUri != null -> AsyncImage(
                            model = fotoUri,
                            contentDescription = "Foto seleccionada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text("Agrega una foto", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
            if (intentoPublicar && !fotoValida) {
                Text("La fotografía es obligatoria", color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { tomarFoto() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Cámara")
                }
                OutlinedButton(
                    onClick = {
                        galeriaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Galería")
                }
            }

            // ---------- Título y descripción ----------
            OutlinedTextField(
                value = titulo,
                onValueChange = { if (it.length <= 60) titulo = it },
                label = { Text("Título") },
                singleLine = true,
                isError = intentoPublicar && !tituloValido,
                supportingText = {
                    Text(if (intentoPublicar && !tituloValido) "El título es obligatorio" else "${titulo.length}/60")
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                minLines = 3,
                isError = intentoPublicar && !descripcionValida,
                supportingText = {
                    if (intentoPublicar && !descripcionValida) Text("La descripción es obligatoria")
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Fecha ----------
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (fechaManual) Icons.Default.DateRange else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Fecha", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (fechaManual) ImagenStorage.formatearFecha(fecha)
                            else "Automática (al publicar)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (fechaManual) {
                        TextButton(onClick = { fechaManual = false }) { Text("Auto") }
                    }
                    TextButton(onClick = { mostrarCalendario = true }) { Text("Elegir") }
                }
            }

            Button(
                onClick = { publicar() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Text("Publicar")
            }
        }
    }

    // ---------- Selector de fecha (DatePicker de Material 3) ----------
    if (mostrarCalendario) {
        val estadoCalendario = rememberDatePickerState(
            initialSelectedDateMillis = aMediaNocheUtc(fecha)
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoCalendario.selectedDateMillis?.let {
                        fecha = combinarConHoraActual(it)
                        fechaManual = true
                    }
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoCalendario)
        }
    }
}

// El DatePicker trabaja con medianoche UTC: convertimos la fecha local a ese formato
private fun aMediaNocheUtc(millisLocal: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millisLocal }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

// Toma el día elegido (UTC) y le pone la hora actual local
private fun combinarConHoraActual(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utc.get(Calendar.YEAR))
        set(Calendar.MONTH, utc.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
