package com.example.miniproyecto02.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.miniproyecto02.data.Tarea
import com.example.miniproyecto02.domain.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = db.tareaDao()
    val coroutineScope = rememberCoroutineScope()

    var menuExpandido by remember { mutableStateOf(false) }
    var mostrarSoloPendientes by remember { mutableStateOf(false) }
    var nuevaTareaTitulo by remember { mutableStateOf("") }

    val todasLasTareas by dao.obtenerTodas().collectAsState(initial = emptyList())
    val tareasPendientes by dao.obtenerPendientes().collectAsState(initial = emptyList())
    val tareasAMostrar = if (mostrarSoloPendientes) tareasPendientes else todasLasTareas

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestor de Tareas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { menuExpandido = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(
                        expanded = menuExpandido,
                        onDismissRequest = { menuExpandido = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (mostrarSoloPendientes) "Mostrar Todas" else "Mostrar solo Pendientes") },
                            onClick = {
                                mostrarSoloPendientes = !mostrarSoloPendientes
                                menuExpandido = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Borrar Completadas") },
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    dao.eliminarCompletadas()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Tareas limpiadas", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                menuExpandido = false
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nuevaTareaTitulo,
                    onValueChange = { nuevaTareaTitulo = it },
                    label = { Text("Nueva tarea...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (nuevaTareaTitulo.isNotBlank()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                dao.insertar(Tarea(titulo = nuevaTareaTitulo))
                                withContext(Dispatchers.Main) {
                                    nuevaTareaTitulo = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tareasAMostrar) { tarea ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tarea.completada,
                                onCheckedChange = { isChecked ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        dao.actualizar(tarea.copy(completada = isChecked))
                                    }
                                }
                            )
                            Text(
                                text = tarea.titulo,
                                modifier = Modifier.weight(1f),
                                textDecoration = if (tarea.completada) TextDecoration.LineThrough else TextDecoration.None
                            )
                            IconButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        dao.eliminar(tarea)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}