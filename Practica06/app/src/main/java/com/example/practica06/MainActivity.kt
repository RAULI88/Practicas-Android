package com.example.practica06

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val listaContactos = obtenerContactosDummy()

        setContent {
            // Incrustamos el RecyclerView dentro de tu Empty Activity
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    RecyclerView(context).apply {
                        // Cumple el Punto 4: Configura el LayoutManager adecuado
                        layoutManager = LinearLayoutManager(context)

                        // Cumple el Punto 3: Vincula el Adapter personalizado
                        adapter = ContactoAdapter(listaContactos) { contacto ->
                            // Cumple el Punto 5: Mostrar detalles al hacer clic
                            Toast.makeText(context, "Detalles: ${contacto.nombre}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}