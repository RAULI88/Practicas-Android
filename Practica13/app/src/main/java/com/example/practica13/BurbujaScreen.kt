package com.example.practica13

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Colores que se alternan cada vez que se detecta una sacudida
private val coloresSacudida = listOf(
    Color(0xFF6A1B9A), Color(0xFF1565C0), Color(0xFF2E7D32),
    Color(0xFFEF6C00), Color(0xFFC62828), Color(0xFF00838F)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurbujaScreen(sensorManager: SensorManager) {
    // Valores crudos del acelerómetro (m/s²)
    var ejeX by remember { mutableFloatStateOf(0f) }
    var ejeY by remember { mutableFloatStateOf(0f) }
    var ejeZ by remember { mutableFloatStateOf(0f) }
    var fuerzaG by remember { mutableFloatStateOf(1f) }

    // Detección de sacudidas
    var umbralG by remember { mutableFloatStateOf(2.5f) }
    var sacudidas by remember { mutableIntStateOf(0) }
    var ultimaSacudida by remember { mutableLongStateOf(0L) }
    var mostrarAviso by remember { mutableStateOf(false) }

    // Tamaños del nivel de burbuja: el radio máximo se calcula en píxeles según la pantalla
    val circuloExterior = 250.dp
    val burbuja = 40.dp
    val radioMaximoPx = with(LocalDensity.current) { ((circuloExterior - burbuja) / 2).toPx() }

    val acelerometro = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val umbralActual by rememberUpdatedState(umbralG)

    // Respuesta visual: el fondo cambia de color con animación en cada sacudida
    val colorFondo by animateColorAsState(
        targetValue = if (sacudidas == 0) Color.DarkGray else coloresSacudida[sacudidas % coloresSacudida.size],
        animationSpec = tween(durationMillis = 400),
        label = "colorFondo"
    )

    // Oculta el mensaje flotante 1.5 s después de cada sacudida
    LaunchedEffect(sacudidas) {
        if (sacudidas > 0) {
            mostrarAviso = true
            delay(1500)
            mostrarAviso = false
        }
    }

    // Ciclo de vida del sensor: se registra en ON_RESUME y se libera en ON_PAUSE
    // (así no gasta batería cuando la app está en segundo plano)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, acelerometro) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // 1. Lecturas de los tres ejes
                    ejeX = it.values[0]
                    ejeY = it.values[1]
                    ejeZ = it.values[2]

                    // 2. Magnitud del vector de aceleración: |a| = √(x² + y² + z²)
                    val magnitud = sqrt(ejeX * ejeX + ejeY * ejeY + ejeZ * ejeZ)
                    // 3. Se normaliza con la gravedad terrestre: en reposo ≈ 1 g
                    fuerzaG = magnitud / SensorManager.GRAVITY_EARTH

                    // 4. Si supera el umbral (y pasó 1 s desde la última), es una sacudida
                    val ahora = System.currentTimeMillis()
                    if (fuerzaG > umbralActual && ahora - ultimaSacudida > 1000) {
                        ultimaSacudida = ahora
                        sacudidas++
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> acelerometro?.let {
                    sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
                }
                Lifecycle.Event.ON_PAUSE -> sensorManager.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(listener)
        }
    }

    // Posición de la burbuja (misma lógica de la práctica: se limita al radio con Pitágoras)
    val rawX = -ejeX / SensorManager.GRAVITY_EARTH * radioMaximoPx
    val rawY = ejeY / SensorManager.GRAVITY_EARTH * radioMaximoPx
    val distancia = sqrt(rawX * rawX + rawY * rawY)
    val proporcion = if (distancia > radioMaximoPx) radioMaximoPx / distancia else 1f
    val offsetX = rawX * proporcion
    val offsetY = rawY * proporcion
    val nivelado = distancia < radioMaximoPx * 0.08f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensores: Acelerómetro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorFondo)
        ) {
            if (acelerometro == null) {
                Text(
                    "Este dispositivo no tiene acelerómetro",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ----- Lecturas en tiempo real de los 3 ejes -----
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilaEje("Eje X", ejeX, Color(0xFFEF5350))
                            FilaEje("Eje Y", ejeY, Color(0xFF66BB6A))
                            FilaEje("Eje Z", ejeZ, Color(0xFF42A5F5))
                            Text(
                                "Fuerza total: ${"%.2f".format(fuerzaG)} g",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ----- Nivel de burbuja -----
                    Box(
                        modifier = Modifier
                            .size(circuloExterior)
                            .border(4.dp, Color.White, CircleShape)
                            .background(Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Círculo de referencia del centro
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .border(2.dp, if (nivelado) Color(0xFF00FF00) else Color.Red, CircleShape)
                        )
                        // Burbuja que se mueve según la inclinación
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                                .size(burbuja)
                                .background(Color(0xFF00FF00), CircleShape)
                        )
                    }
                    Text(
                        if (nivelado) "¡Superficie nivelada!" else "Inclina el teléfono",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // ----- Detección de sacudidas -----
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Sacudidas detectadas: $sacudidas",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { sacudidas = 0 }) { Text("Reiniciar", color = Color.White) }
                            }
                            Text(
                                "Umbral de sensibilidad: ${"%.1f".format(umbralG)} g",
                                color = Color.White
                            )
                            Slider(
                                value = umbralG,
                                onValueChange = { umbralG = it },
                                valueRange = 1.5f..4f
                            )
                            Text(
                                "Menor umbral = más sensible",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Mensaje flotante al detectar la sacudida
                AnimatedVisibility(
                    visible = mostrarAviso,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(horizontal = 28.dp, vertical = 18.dp)
                    ) {
                        Text(
                            "¡Sacudida detectada!",
                            color = colorFondo,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaEje(nombre: String, valor: Float, color: Color) {
    Column {
        Row {
            Text(nombre, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
            Text(
                "${"%+6.2f".format(valor)} m/s²",
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(4.dp))
        // Barra centrada: -20 m/s² a +20 m/s²
        LinearProgressIndicator(
            progress = { ((valor + 20f) / 40f).coerceIn(0f, 1f) },
            color = color,
            trackColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
