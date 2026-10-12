package com.example.practica01

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondScreen(mensaje: String, onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = Color(0xFFF3E5F5),
        topBar = {
            // RETO: Se agrega la barra superior con el botón de retroceso
            TopAppBar(
                title = { Text("Segunda Pantalla") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE1BEE7) // Color que combina con el fondo
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Texto recibido:",
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Aquí se muestra el saludo dinámico que pide la instrucción
            Text(
                text = "$mensaje",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}