package com.example.practica12.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.practica12.data.model.Personaje

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonajesScreen(viewModel: PersonajesViewModel = viewModel()) {
    // La UI observa el StateFlow del ViewModel
    val estado by viewModel.uiState.collectAsState()
    var textoBusqueda by rememberSaveable { mutableStateOf("") }
    var seleccionado by remember { mutableStateOf<Personaje?>(null) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Scroll infinito: cuando se ve uno de los últimos 3 elementos, pide la siguiente página
    val llegoAlFinal by remember {
        derivedStateOf {
            val ultimoVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            ultimoVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(llegoAlFinal) {
        if (llegoAlFinal && estado.personajes.isNotEmpty()) viewModel.cargarSiguientePagina()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rick and Morty API")
                        if (estado.totalPersonajes > 0) {
                            Text(
                                "${estado.personajes.size} de ${estado.totalPersonajes} personajes",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
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
        ) {
            // Buscador por nombre (usa el parámetro ?name= de la API)
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { textoBusqueda = it },
                placeholder = { Text("Buscar personaje (ej. Rick, Morty)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (textoBusqueda.isNotEmpty()) {
                        IconButton(onClick = {
                            textoBusqueda = ""
                            viewModel.buscar("")
                            focusManager.clearFocus()
                        }) { Icon(Icons.Default.Clear, contentDescription = "Limpiar") }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.buscar(textoBusqueda)
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(estado.personajes, key = { it.id }) { personaje ->
                    PersonajeCard(personaje = personaje, onClick = { seleccionado = personaje })
                }

                // Pie de la lista: cargando / error / fin
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            estado.cargando -> CircularProgressIndicator()
                            estado.error != null -> EstadoError(estado.error!!, onReintentar = viewModel::reintentar)
                            !estado.hayMas && estado.personajes.isNotEmpty() ->
                                Text("No hay más personajes", color = MaterialTheme.colorScheme.outline)
                            estado.hayMas && estado.personajes.isNotEmpty() ->
                                TextButton(onClick = viewModel::cargarSiguientePagina) { Text("Cargar más") }
                        }
                    }
                }
            }
        }
    }

    seleccionado?.let { personaje ->
        DetallePersonajeDialog(personaje = personaje, onDismiss = { seleccionado = null })
    }
}

@Composable
fun PersonajeCard(personaje: Personaje, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = personaje.image,
                contentDescription = personaje.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    personaje.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IndicadorEstado(personaje)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ubicación: ${personaje.location.name}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun IndicadorEstado(personaje: Personaje) {
    val (color, texto) = when (personaje.status) {
        "Alive" -> Color(0xFF43A047) to "Vivo"
        "Dead" -> Color(0xFFE53935) to "Muerto"
        else -> Color(0xFF9E9E9E) to "Desconocido"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text("$texto - ${personaje.species}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EstadoError(mensaje: String, onReintentar: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(mensaje, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onReintentar) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Reintentar")
        }
    }
}

@Composable
fun DetallePersonajeDialog(personaje: Personaje, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text(personaje.name) },
        text = {
            Column {
                AsyncImage(
                    model = personaje.image,
                    contentDescription = personaje.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(12.dp))
                IndicadorEstado(personaje)
                Text("Género: ${personaje.gender}")
                if (personaje.type.isNotBlank()) Text("Tipo: ${personaje.type}")
                Text("Origen: ${personaje.origin.name}")
                Text("Última ubicación: ${personaje.location.name}")
                Text("Aparece en ${personaje.episode.size} episodio(s)")
            }
        }
    )
}
