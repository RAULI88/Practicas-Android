package com.example.practica05

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DetailScreen(
    matricula: String,
    nombre: String,
    carrera: String,
    turno: String,
    estatusActivo: Boolean,
    onNavigateBack: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Detalles de Confirmación", style = MaterialTheme.typography.headlineMedium)
            HorizontalDivider()

            Text("Matrícula:", fontWeight = FontWeight.Bold)
            Text(matricula)

            Text("Nombre Completo:", fontWeight = FontWeight.Bold)
            Text(nombre)

            Text("Carrera:", fontWeight = FontWeight.Bold)
            Text(carrera)

            Text("Turno:", fontWeight = FontWeight.Bold)
            Text(turno)

            Text("Estatus:", fontWeight = FontWeight.Bold)
            Text(if (estatusActivo) "Activo" else "Inactivo")

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al Registro")
            }
        }
    }
}