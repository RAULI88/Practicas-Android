package com.example.practica14

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.practica14.ui.theme.Practica14Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ctx = applicationContext
        // Configuración de OSMdroid (caché de mosaicos y preferencias)
        org.osmdroid.config.Configuration.getInstance().load(
            ctx,
            ctx.getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        // Firma única de la app para los servidores de mosaicos
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "Practica14_SeanMuro_UAS/1.0"
        setContent {
            Practica14Theme {
                MapaOSMScreen()
            }
        }
    }
}