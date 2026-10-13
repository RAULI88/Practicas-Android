package com.example.practica06

data class Contacto(val id: Int, val nombre: String, val telefono: String)

fun obtenerContactosDummy(): List<Contacto> {
    return listOf(
        Contacto(1, "Ana García", "555-1001"),
        Contacto(2, "Luis Pérez", "555-1002"),
        Contacto(3, "María López", "555-1003"),
        Contacto(4, "Carlos Ramírez", "555-1004"),
        Contacto(5, "Sofía Torres", "555-1005")
    )
}