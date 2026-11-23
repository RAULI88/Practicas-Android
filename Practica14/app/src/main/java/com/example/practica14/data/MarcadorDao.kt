package com.example.practica14.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarcadorDao {

    // Flujo reactivo: el mapa se actualiza solo al agregar o borrar un punto
    @Query("SELECT * FROM marcadores ORDER BY fecha DESC")
    fun obtenerTodos(): Flow<List<MarcadorEntity>>

    @Insert
    suspend fun insertar(marcador: MarcadorEntity): Long

    @Delete
    suspend fun eliminar(marcador: MarcadorEntity)

    @Query("DELETE FROM marcadores")
    suspend fun eliminarTodos()
}
