package com.example.practica12.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Cliente HTTP único (Singleton) para toda la app
object RetrofitClient {

    private const val BASE_URL = "https://rickandmortyapi.com/api/"

    val api: RickAndMortyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Gson convierte el JSON de la respuesta en nuestras data classes
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RickAndMortyApi::class.java)
    }
}
