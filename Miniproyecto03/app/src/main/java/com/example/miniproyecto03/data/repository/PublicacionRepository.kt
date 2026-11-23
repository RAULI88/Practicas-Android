package com.example.miniproyecto03.data.repository

import com.example.miniproyecto03.data.local.Publicacion
import com.example.miniproyecto03.data.local.PublicacionDao
import kotlinx.coroutines.flow.Flow

// Repositorio: única fuente de datos para el ViewModel (capa intermedia de MVVM)
class PublicacionRepository(private val dao: PublicacionDao) {

    val publicaciones: Flow<List<Publicacion>> = dao.obtenerTodas()

    suspend fun publicar(publicacion: Publicacion) = dao.insertar(publicacion)

    suspend fun eliminar(publicacion: Publicacion) = dao.eliminar(publicacion)
}
