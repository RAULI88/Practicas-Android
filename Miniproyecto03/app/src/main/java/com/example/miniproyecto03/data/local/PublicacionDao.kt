package com.example.miniproyecto03.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicacionDao {

    // Flow: Room emite una nueva lista cada vez que cambia la tabla
    @Query("SELECT * FROM publicaciones ORDER BY fecha DESC")
    fun obtenerTodas(): Flow<List<Publicacion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(publicacion: Publicacion): Long

    @Delete
    suspend fun eliminar(publicacion: Publicacion)
}
