package com.example.practica05

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.practica05.data.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(onNavigateToDetail: (String, String, String, String, Boolean) -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    var matricula by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }

    val carreras = listOf("Ing. de Software", "Ing. Civil", "Ing. Industrial", "Arquitectura")
    var carreraSeleccionada by remember { mutableStateOf(carreras[0]) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val turnos = listOf("Matutino", "Vespertino")
    var turnoSeleccionado by remember { mutableStateOf(turnos[0]) }

    var estatusActivo by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val matriculaGuardada = preferencesManager.getMatricula()
        val nombreGuardado = preferencesManager.getNombre()
        if (matriculaGuardada.isNotEmpty()) {
            matricula = matriculaGuardada
        }
        if (nombreGuardado.isNotEmpty()) {
            nombre = nombreGuardado
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Registro de Estudiantes", style = MaterialTheme.typography.headlineMedium)
            HorizontalDivider()

            OutlinedTextField(
                value = matricula,
                onValueChange = { matricula = it },
                label = { Text("Matrícula") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = carreraSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Carrera") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    carreras.forEach { seleccion ->
                        DropdownMenuItem(
                            text = { Text(seleccion) },
                            onClick = {
                                carreraSeleccionada = seleccion
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            Text("Turno:")
            Row(Modifier.selectableGroup()) {
                turnos.forEach { turno ->
                    Row(
                        Modifier
                            .selectable(
                                selected = (turno == turnoSeleccionado),
                                onClick = { turnoSeleccionado = turno },
                                role = Role.RadioButton
                            )
                            .padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (turno == turnoSeleccionado), onClick = null)
                        Text(text = turno, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Estatus: ${if (estatusActivo) "Activo" else "Inactivo"}")
                Switch(checked = estatusActivo, onCheckedChange = { estatusActivo = it })
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (matricula.isNotBlank() && nombre.isNotBlank()) {
                        preferencesManager.saveDatos(matricula, nombre)
                        onNavigateToDetail(matricula, nombre, carreraSeleccionada, turnoSeleccionado, estatusActivo)
                    } else {
                        Toast.makeText(context, "Llena los campos de texto", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar y Continuar")
            }
        }
    }
}