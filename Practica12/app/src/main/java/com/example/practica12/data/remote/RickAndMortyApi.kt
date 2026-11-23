package com.example.practica12.data.remote

import com.example.practica12.data.model.Personaje
import com.example.practica12.data.model.PersonajesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Interfaz del servicio web: cada función es un endpoint HTTP
interface RickAndMortyApi {

    // GET https://rickandmortyapi.com/api/character?page=1&name=rick
    @GET("character")
    suspend fun obtenerPersonajes(
        @Query("page") pagina: Int,
        @Query("name") nombre: String? = null
    ): PersonajesResponse

    // GET https://rickandmortyapi.com/api/character/1
    @GET("character/{id}")
    suspend fun obtenerPersonaje(@Path("id") id: Int): Personaje
}
