package com.example.practica14

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.practica14.data.AppDatabase
import com.example.practica14.data.MarcadorEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Centro inicial: Los Mochis, Sinaloa (mientras llega la señal GPS)
private val CENTRO_INICIAL = GeoPoint(25.7905, -108.9875)

// Mapa de calles de Esri (usa el orden z/y/x en la URL, por eso se arma a mano)
private val MAPA_ESRI = object : OnlineTileSourceBase(
    "EsriWorldStreet", 0, 19, 256, "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"),
    "Tiles © Esri"
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
}

// Capas disponibles: si un servidor bloquea los mosaicos, se cambia a otro desde la app
private enum class CapaMapa(val etiqueta: String) {
    OPENTOPO("OpenTopoMap"),
    ESRI("Esri calles"),
    OSM("OpenStreetMap")
}

private fun fuenteDe(capa: CapaMapa): ITileSource = when (capa) {
    CapaMapa.OPENTOPO -> TileSourceFactory.OpenTopo
    CapaMapa.ESRI -> MAPA_ESRI
    CapaMapa.OSM -> TileSourceFactory.MAPNIK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaOSMScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // ---------- Room ----------
    val dao = remember { AppDatabase.getDatabase(context).marcadorDao() }
    val marcadores by remember { dao.obtenerTodos() }.collectAsState(initial = emptyList())

    // ---------- Estado ----------
    var permisoConcedido by remember { mutableStateOf(tienePermisoUbicacion(context)) }
    var permisoDenegado by remember { mutableStateOf(false) }
    var ubicacion by remember { mutableStateOf<GeoPoint?>(null) }
    var precision by remember { mutableStateOf<Float?>(null) }
    var centradoInicial by remember { mutableStateOf(false) }
    var puntoNuevo by remember { mutableStateOf<GeoPoint?>(null) }   // pulsación larga pendiente
    var mostrarLista by remember { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var capa by rememberSaveable { mutableStateOf(CapaMapa.OPENTOPO) }
    var menuCapas by remember { mutableStateOf(false) }

    // ---------- Permisos de ubicación en tiempo de ejecución ----------
    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        val fino = resultado[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val aproximado = resultado[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permisoConcedido = fino || aproximado
        permisoDenegado = !permisoConcedido
        when {
            fino -> Unit
            aproximado -> Toast.makeText(context, "Ubicación aproximada: la precisión será menor", Toast.LENGTH_LONG).show()
            else -> Toast.makeText(context, "Sin permiso de ubicación no se puede mostrar tu posición", Toast.LENGTH_LONG).show()
        }
    }

    fun pedirPermisos() {
        permisoLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // Verificar el permiso al iniciar la pantalla
    LaunchedEffect(Unit) {
        if (!permisoConcedido) pedirPermisos()
    }

    // ---------- MapView de OSMdroid (vista clásica dentro de Compose) ----------
    val mapView = remember {
        MapView(context).apply {
            setTileSource(fuenteDe(CapaMapa.OPENTOPO))
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(CENTRO_INICIAL)
            overlays.add(CopyrightOverlay(context))
            // RETO Fase 2 - Interacción: pulsación larga para capturar una coordenada
            overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    if (p != null) puntoNuevo = p
                    return true
                }
            }))
        }
    }

    // Cambia los mosaicos cuando el usuario elige otra capa
    LaunchedEffect(capa) {
        mapView.setTileSource(fuenteDe(capa))
        mapView.invalidate()
    }

    // Marcador personalizado fijado en la ubicación obtenida con FusedLocationProviderClient
    val marcadorActual = remember {
        Marker(mapView).apply {
            title = "Mi ubicación"
            icon = ContextCompat.getDrawable(context, R.drawable.ic_pin_ubicacion)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }

    // Ciclo de vida del MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // ---------- FusedLocationProviderClient: latitud y longitud ----------
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    DisposableEffect(permisoConcedido, lifecycleOwner) {
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                resultado.lastLocation?.let {
                    ubicacion = GeoPoint(it.latitude, it.longitude)
                    precision = it.accuracy
                }
            }
        }
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> if (permisoConcedido) {
                    iniciarActualizaciones(fusedClient, callback) { loc ->
                        ubicacion = GeoPoint(loc.latitude, loc.longitude)
                        precision = loc.accuracy
                    }
                }
                // Se detienen en segundo plano para ahorrar batería
                Lifecycle.Event.ON_PAUSE -> fusedClient.removeLocationUpdates(callback)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            fusedClient.removeLocationUpdates(callback)
        }
    }

    // ---------- Capa "mi ubicación" con ícono ROJO (RETO Fase 1) ----------
    LaunchedEffect(permisoConcedido) {
        if (permisoConcedido && mapView.overlays.none { it is MyLocationNewOverlay }) {
            val capaUbicacion = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
            val persona = vectorABitmap(context, R.drawable.ic_ubicacion_roja, 40)
            val flecha = vectorABitmap(context, R.drawable.ic_flecha_roja, 44)
            capaUbicacion.setPersonIcon(persona)
            capaUbicacion.setDirectionArrow(persona, flecha)
            capaUbicacion.setPersonAnchor(0.5f, 0.5f)
            capaUbicacion.setDirectionAnchor(0.5f, 0.5f)
            capaUbicacion.enableMyLocation()
            mapView.overlays.add(capaUbicacion)
            mapView.invalidate()
        }
    }

    // Mueve el marcador cuando cambia la ubicación y centra la cámara la primera vez
    LaunchedEffect(ubicacion) {
        val punto = ubicacion ?: return@LaunchedEffect
        marcadorActual.position = punto
        marcadorActual.snippet = "Lat: %.6f\nLon: %.6f".format(punto.latitude, punto.longitude)
        if (!mapView.overlays.contains(marcadorActual)) mapView.overlays.add(marcadorActual)
        if (!centradoInicial) {
            mapView.controller.animateTo(punto, 17.0, 1200L)
            centradoInicial = true
        }
        mapView.invalidate()
    }

    // Dibuja los marcadores guardados en Room (se repinta solo gracias al Flow)
    LaunchedEffect(marcadores) {
        val anteriores = mapView.overlays.filter { it is Marker && it.relatedObject is MarcadorEntity }
        anteriores.forEach { (it as Marker).closeInfoWindow() }
        mapView.overlays.removeAll(anteriores)
        marcadores.forEach { m ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(m.latitud, m.longitud)
                    title = m.titulo
                    snippet = "%.6f, %.6f\n%s".format(m.latitud, m.longitud, formatearFecha(m.fecha))
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    relatedObject = m
                }
            )
        }
        mapView.invalidate()
    }

    // ---------- UI ----------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS y Mapas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    if (marcadores.isNotEmpty()) {
                        IconButton(onClick = { confirmarBorrado = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Borrar marcadores")
                        }
                    }
                    // Selector de capas del mapa
                    Box {
                        IconButton(onClick = { menuCapas = true }) {
                            Icon(Icons.Default.Layers, contentDescription = "Cambiar mapa")
                        }
                        DropdownMenu(expanded = menuCapas, onDismissRequest = { menuCapas = false }) {
                            CapaMapa.entries.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion.etiqueta) },
                                    onClick = {
                                        capa = opcion
                                        menuCapas = false
                                    },
                                    leadingIcon = {
                                        if (opcion == capa) Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = { mostrarLista = true }) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Marcadores guardados")
                }
                FloatingActionButton(onClick = {
                    val punto = ubicacion
                    if (punto != null) {
                        mapView.controller.animateTo(punto, 17.0, 800L)
                    } else if (!permisoConcedido) {
                        pedirPermisos()
                    } else {
                        Toast.makeText(context, "Esperando señal GPS...", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Centrar en mi ubicación")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Tarjeta superior con las coordenadas o el estado del permiso
            TarjetaCoordenadas(
                permisoConcedido = permisoConcedido,
                permisoDenegado = permisoDenegado,
                ubicacion = ubicacion,
                precision = precision,
                totalMarcadores = marcadores.size,
                onPedirPermiso = { pedirPermisos() },
                onAbrirAjustes = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            )
        }
    }

    // ---------- Diálogo: guardar el punto de la pulsación larga ----------
    puntoNuevo?.let { punto ->
        var titulo by remember(punto) { mutableStateOf("Punto ${marcadores.size + 1}") }
        AlertDialog(
            onDismissRequest = { puntoNuevo = null },
            icon = { Icon(Icons.Default.Place, contentDescription = null) },
            title = { Text("Guardar marcador") },
            text = {
                Column {
                    Text(
                        "Lat: %.6f\nLon: %.6f".format(punto.latitude, punto.longitude),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.padding(4.dp))
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        label = { Text("Título descriptivo") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val nuevo = MarcadorEntity(
                        latitud = punto.latitude,
                        longitud = punto.longitude,
                        titulo = titulo.ifBlank { "Sin título" }
                    )
                    scope.launch { dao.insertar(nuevo) }
                    puntoNuevo = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { puntoNuevo = null }) { Text("Cancelar") } }
        )
    }

    // ---------- Diálogo: lista de marcadores guardados ----------
    if (mostrarLista) {
        AlertDialog(
            onDismissRequest = { mostrarLista = false },
            title = { Text("Marcadores guardados (${marcadores.size})") },
            text = {
                if (marcadores.isEmpty()) {
                    Text("Mantén presionado el mapa sobre una calle para guardar un punto.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(marcadores, key = { it.id }) { m ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mapView.controller.animateTo(GeoPoint(m.latitud, m.longitud), 17.0, 800L)
                                        mostrarLista = false
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(m.titulo, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "%.5f, %.5f".format(m.latitud, m.longitud),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { scope.launch { dao.eliminar(m) } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarLista = false }) { Text("Cerrar") } }
        )
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("Borrar marcadores") },
            text = { Text("Se eliminarán los ${marcadores.size} puntos guardados.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.eliminarTodos() }
                    confirmarBorrado = false
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaCoordenadas(
    permisoConcedido: Boolean,
    permisoDenegado: Boolean,
    ubicacion: GeoPoint?,
    precision: Float?,
    totalMarcadores: Int,
    onPedirPermiso: () -> Unit,
    onAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            when {
                // Escenario de denegación del permiso
                !permisoConcedido -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = Color(0xFFC62828))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (permisoDenegado) "Permiso de ubicación denegado" else "Se necesita permiso de ubicación",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "El mapa funciona, pero no puede mostrar tu posición.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row {
                        TextButton(onClick = onPedirPermiso) { Text("Conceder permiso") }
                        if (permisoDenegado) TextButton(onClick = onAbrirAjustes) { Text("Abrir ajustes") }
                    }
                }
                ubicacion == null -> {
                    Text("Obteniendo ubicación GPS...", fontWeight = FontWeight.Bold)
                    Text(
                        "En el emulador usa Extended Controls > Location",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = Color(0xFFE53935))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Lat: %.6f   Lon: %.6f".format(ubicacion.latitude, ubicacion.longitude),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Precisión: ±${precision?.toInt() ?: "-"} m · Marcadores: $totalMarcadores",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Utilidades ----------

private fun tienePermisoUbicacion(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission") // el permiso se verifica antes de llamar a esta función
private fun iniciarActualizaciones(
    cliente: FusedLocationProviderClient,
    callback: LocationCallback,
    onPrimeraUbicacion: (android.location.Location) -> Unit
) {
    // Lectura inmediata de la posición actual
    cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { loc -> if (loc != null) onPrimeraUbicacion(loc) }
    // Actualizaciones periódicas (cada 5 s, mínimo 2 s)
    val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
        .setMinUpdateIntervalMillis(2_000L)
        .build()
    cliente.requestLocationUpdates(solicitud, callback, android.os.Looper.getMainLooper())
}

/** RETO Fase 1: OSMdroid exige Bitmap, así que convertimos el Vector Asset a mapa de bits. */
private fun vectorABitmap(context: Context, @DrawableRes id: Int, tamDp: Int): Bitmap {
    val px = (tamDp * context.resources.displayMetrics.density).toInt()
    val drawable = requireNotNull(ContextCompat.getDrawable(context, id))
    return drawable.toBitmap(width = px, height = px)
}

private fun formatearFecha(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))