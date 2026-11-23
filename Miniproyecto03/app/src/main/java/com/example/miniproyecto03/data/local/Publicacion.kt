package com.example.miniproyecto03.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidad (tabla) de Room: cada fila es una publicación de la red social
@Entity(tableName = "publicaciones")
data class Publicacion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val descripcion: String,
    // URI (file://...) de la foto copiada en la memoria interna de la app
    @ColumnInfo(name = "foto_uri")
    val fotoUri: String,
    // Fecha en milisegundos (epoch)
    val fecha: Long
)
