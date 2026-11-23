package com.example.practica11

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.practica11.ui.theme.Practica11Theme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Practica11Theme {
                var pestana by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(if (pestana == 0) "Archivos y SDCard" else "Mini Reproductor MP3") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = pestana == 0,
                                onClick = { pestana = 0 },
                                icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                label = { Text("Archivos") }
                            )
                            NavigationBarItem(
                                selected = pestana == 1,
                                onClick = { pestana = 1 },
                                icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                                label = { Text("Reproductor") }
                            )
                        }
                    }
                ) { paddingValues ->
                    when (pestana) {
                        0 -> ArchivosScreen(Modifier.padding(paddingValues))
                        else -> ReproductorScreen(Modifier.padding(paddingValues))
                    }
                }
            }
        }
    }
}
