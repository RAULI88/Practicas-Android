package com.example.miniproyecto03.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.miniproyecto03.data.local.AppDatabase
import com.example.miniproyecto03.data.local.Publicacion
import com.example.miniproyecto03.data.repository.PublicacionRepository
import com.example.miniproyecto03.util.ImagenStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel (MVVM): expone el estado a la UI y ejecuta las operaciones en corrutinas
class PublicacionViewModel(private val repository: PublicacionRepository) : ViewModel() {

    // Lista reactiva de publicaciones (se actualiza sola al insertar o borrar)
    val publicaciones: StateFlow<List<Publicacion>> = repository.publicaciones
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun publicar(titulo: String, descripcion: String, fotoUri: String, fecha: Long) {
        viewModelScope.launch {
            repository.publicar(
                Publicacion(
                    titulo = titulo.trim(),
                    descripcion = descripcion.trim(),
                    fotoUri = fotoUri,
                    fecha = fecha
                )
            )
        }
    }

    fun eliminar(publicacion: Publicacion) {
        viewModelScope.launch {
            repository.eliminar(publicacion)
            ImagenStorage.eliminar(publicacion.fotoUri)
        }
    }

    companion object {
        // Factory para crear el ViewModel con su repositorio (sin librerías de inyección)
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val dao = AppDatabase.getDatabase(app).publicacionDao()
                PublicacionViewModel(PublicacionRepository(dao))
            }
        }
    }
}
