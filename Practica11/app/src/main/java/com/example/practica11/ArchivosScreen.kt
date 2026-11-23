package com.example.practica11

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Operación que quedó pendiente mientras se pedían los permisos
private enum class Operacion { NINGUNA, GUARDAR, AGREGAR, LEER, ELIMINAR }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArchivosScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ubicacion by rememberSaveable { mutableStateOf(Ubicacion.INTERNA) }
    var nombre by rememberSaveable { mutableStateOf("notas.txt") }
    var contenido by rememberSaveable { mutableStateOf("") }
    var leido by rememberSaveable { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // texto, esError
    var permisosOk by remember { mutableStateOf(ArchivoUtils.tienePermisos(context)) }
    var pendiente by remember { mutableStateOf(Operacion.NINGUNA) }
    var refresco by remember { mutableIntStateOf(0) }
    var archivos by remember { mutableStateOf<List<File>>(emptyList()) }

    val sdDisponible = remember { ArchivoUtils.sdFisica(context) != null }
    val directorio = ArchivoUtils.directorio(context, ubicacion)

    // Recarga la lista de archivos cuando cambia la ubicación o después de cada operación
    LaunchedEffect(ubicacion, refresco) {
        archivos = withContext(Dispatchers.IO) { ArchivoUtils.listar(directorio) }
    }

    // Ejecuta la operación de E/S en un hilo de fondo (Dispatchers.IO)
    fun ejecutar(op: Operacion) {
        val dir = directorio
        if (dir == null) {
            mensaje = "La ubicación seleccionada no está disponible" to true
            return
        }
        if (nombre.isBlank()) {
            mensaje = "Escribe un nombre de archivo" to true
            return
        }
        scope.launch {
            val resultado = withContext(Dispatchers.IO) {
                runCatching {
                    when (op) {
                        Operacion.GUARDAR -> {
                            val f = ArchivoUtils.escribir(dir, nombre.trim(), contenido, agregar = false)
                            "Guardado en ${f.absolutePath}"
                        }
                        Operacion.AGREGAR -> {
                            ArchivoUtils.escribir(dir, nombre.trim(), contenido, agregar = true)
                            "Texto agregado al final de ${nombre.trim()}"
                        }
                        Operacion.LEER -> {
                            leido = ArchivoUtils.leer(dir, nombre.trim())
                            "Archivo leído correctamente"
                        }
                        Operacion.ELIMINAR -> {
                            if (ArchivoUtils.eliminar(dir, nombre.trim())) {
                                leido = null
                                "Archivo eliminado"
                            } else {
                                error("El archivo no existe")
                            }
                        }
                        Operacion.NINGUNA -> ""
                    }
                }
            }
            mensaje = resultado.fold(
                onSuccess = { it to false },
                onFailure = { "Error: ${it.message ?: it.javaClass.simpleName}" to true }
            )
            refresco++
        }
    }

    // Solicitud de permisos de almacenamiento en tiempo de ejecución
    val lanzadorPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        permisosOk = resultados.values.all { it }
        if (permisosOk) {
            mensaje = "Permisos de almacenamiento concedidos" to false
            if (pendiente != Operacion.NINGUNA) ejecutar(pendiente)
        } else {
            mensaje = "Permiso denegado: no se puede usar el almacenamiento externo" to true
        }
        pendiente = Operacion.NINGUNA
    }

    // La memoria interna nunca pide permisos; la externa sí (según la versión de Android)
    fun intentar(op: Operacion) {
        if (ubicacion != Ubicacion.INTERNA && !permisosOk) {
            pendiente = op
            lanzadorPermisos.launch(ArchivoUtils.permisosRequeridos())
        } else {
            ejecutar(op)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // -------- Tarjeta de permisos --------
        TarjetaPermisos(
            permisosOk = permisosOk,
            onSolicitar = { lanzadorPermisos.launch(ArchivoUtils.permisosRequeridos()) }
        )

        // -------- Selección de destino --------
        Text("Ubicación de destino", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ubicacion.entries.forEach { u ->
                val habilitada = u != Ubicacion.SD_FISICA || sdDisponible
                FilterChip(
                    selected = ubicacion == u,
                    onClick = {
                        ubicacion = u
                        leido = null
                        mensaje = null
                    },
                    enabled = habilitada,
                    label = {
                        Text(if (habilitada) u.etiqueta else "${u.etiqueta} (no detectada)")
                    },
                    leadingIcon = {
                        Icon(
                            if (u == Ubicacion.INTERNA) Icons.Default.Lock else Icons.Default.SdCard,
                            contentDescription = null
                        )
                    }
                )
            }
        }
        Text(
            text = "Ruta: ${directorio?.absolutePath ?: "no disponible"}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.outline
        )

        // -------- Formulario --------
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del archivo") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = contenido,
            onValueChange = { contenido = it },
            label = { Text("Contenido") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { intentar(Operacion.GUARDAR) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Guardar")
            }
            OutlinedButton(onClick = { intentar(Operacion.AGREGAR) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Agregar")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { intentar(Operacion.LEER) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Leer")
            }
            OutlinedButton(onClick = { intentar(Operacion.ELIMINAR) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Eliminar")
            }
        }

        // -------- Retroalimentación --------
        mensaje?.let { (texto, esError) ->
            Text(
                text = texto,
                color = if (esError) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        leido?.let { texto ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Contenido de \"$nombre\":", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(texto.ifEmpty { "(archivo vacío)" }, fontFamily = FontFamily.Monospace)
                    TextButton(onClick = { contenido = texto }) { Text("Copiar al editor para modificar") }
                }
            }
        }

        // -------- Archivos existentes --------
        HorizontalDivider()
        Text(
            "Archivos en ${ubicacion.etiqueta} (${archivos.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (archivos.isEmpty()) {
            Text("No hay archivos todavía", color = MaterialTheme.colorScheme.outline)
        }
        archivos.forEach { archivo ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        nombre = archivo.name
                        intentar(Operacion.LEER)
                    }
                    .padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(archivo.name, fontWeight = FontWeight.SemiBold)
                    Text("${archivo.length()} bytes", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TarjetaPermisos(permisosOk: Boolean, onSolicitar: () -> Unit) {
    val requeridos = ArchivoUtils.permisosRequeridos()
    val version = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (permisosOk) Color(0x1A2E7D32) else Color(0x1AC62828)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (permisosOk) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (permisosOk) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        requeridos.isEmpty() -> "Sin permisos extra (Scoped Storage)"
                        permisosOk -> "Permisos de almacenamiento concedidos"
                        else -> "Permisos de almacenamiento pendientes"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            Text(version, style = MaterialTheme.typography.bodySmall)
            if (requeridos.isNotEmpty()) {
                Text(
                    requeridos.joinToString { it.substringAfterLast('.') },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (!permisosOk && requeridos.isNotEmpty()) {
                TextButton(onClick = onSolicitar) { Text("Solicitar permisos") }
            }
        }
    }
}
