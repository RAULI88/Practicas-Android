package com.example.practica03

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.practica03.ui.theme.Practica03Theme // Ajusta esto si tu tema tiene otro nombre

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Traemos los valores del Intent
        val nombre = intent.getStringExtra("EXTRA_NOMBRE") ?: "Sin Nombre"
        val correo = intent.getStringExtra("EXTRA_CORREO") ?: "Sin Correo"

        setContent {
            Practica03Theme {
                ProfileScreen(
                    nombre = nombre,
                    correo = correo,
                    onBackClick = { finish() } // Cierra la Activity y regresa a la anterior
                )
            }
        }
    }
}