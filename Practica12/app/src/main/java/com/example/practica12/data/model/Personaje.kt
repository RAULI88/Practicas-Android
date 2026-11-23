package com.example.practica12.data.model

// Data classes que mapean EXACTAMENTE la estructura JSON de
// https://rickandmortyapi.com/api/character

data class PersonajesResponse(
    val info: Info,
    val results: List<Personaje>
)

data class Info(
    val count: Int,
    val pages: Int,
    val next: String?,
    val prev: String?
)

data class Personaje(
    val id: Int,
    val name: String,
    val status: String,   // "Alive", "Dead" o "unknown"
    val species: String,
    val type: String,
    val gender: String,
    val origin: Lugar,
    val location: Lugar,
    val image: String,
    val episode: List<String>,
    val url: String,
    val created: String
)

data class Lugar(
    val name: String,
    val url: String
)
