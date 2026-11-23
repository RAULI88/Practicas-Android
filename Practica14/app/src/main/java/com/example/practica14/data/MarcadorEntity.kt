package com.example.practica14.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// RETO Fase 2: entidad (tabla) para guardar puntos de interés del mapa
@Entity(tableName = "marcadores")
data class MarcadorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitud: Double,
    val longitud: Double,
    val titulo: String,
    val fecha: Long = System.currentTimeMillis()
)
