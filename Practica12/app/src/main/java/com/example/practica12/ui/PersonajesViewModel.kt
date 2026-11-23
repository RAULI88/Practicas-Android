package com.example.practica12.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practica12.data.model.Personaje
import com.example.practica12.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Estado completo de la pantalla
data class PersonajesUiState(
    val personajes: List<Personaje> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val pagina: Int = 0,
    val totalPaginas: Int = 1,
    val totalPersonajes: Int = 0,
    val busqueda: String = ""
) {
    val hayMas: Boolean get() = pagina < totalPaginas
}

class PersonajesViewModel : ViewModel() {

    private val api = RetrofitClient.api

    // StateFlow: la UI lo observa con collectAsState y se redibuja sola
    private val _uiState = MutableStateFlow(PersonajesUiState())
    val uiState: StateFlow<PersonajesUiState> = _uiState.asStateFlow()

    private var trabajoActual: Job? = null

    init {
        cargarSiguientePagina()
    }

    /** Pide la siguiente página a la API (paginación). */
    fun cargarSiguientePagina() {
        val estado = _uiState.value
        if (estado.cargando || !estado.hayMas) return
        pedirPagina(estado.pagina + 1, estado.busqueda, reiniciar = false)
    }

    /** Nueva búsqueda por nombre: reinicia la lista. */
    fun buscar(texto: String) {
        trabajoActual?.cancel()
        _uiState.value = PersonajesUiState(busqueda = texto.trim())
        pedirPagina(1, texto.trim(), reiniciar = true)
    }

    fun reintentar() {
        val estado = _uiState.value
        pedirPagina(if (estado.personajes.isEmpty()) 1 else estado.pagina + 1, estado.busqueda, reiniciar = estado.personajes.isEmpty())
    }

    private fun pedirPagina(pagina: Int, busqueda: String, reiniciar: Boolean) {
        // Corrutina en viewModelScope: la petición corre fuera del hilo principal
        // (Retrofit ejecuta las funciones suspend en su propio hilo de red)
        trabajoActual = viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val respuesta = api.obtenerPersonajes(
                    pagina = pagina,
                    nombre = busqueda.ifBlank { null }
                )
                _uiState.update {
                    it.copy(
                        personajes = if (reiniciar) respuesta.results else it.personajes + respuesta.results,
                        pagina = pagina,
                        totalPaginas = respuesta.info.pages,
                        totalPersonajes = respuesta.info.count,
                        cargando = false
                    )
                }
            } catch (e: HttpException) {
                // La API responde 404 cuando la búsqueda no tiene resultados
                val mensaje = if (e.code() == 404) "No se encontraron personajes" else "Error del servidor (${e.code()})"
                _uiState.update { it.copy(cargando = false, error = mensaje, totalPaginas = if (e.code() == 404) 0 else it.totalPaginas) }
            } catch (e: IOException) {
                _uiState.update { it.copy(cargando = false, error = "Sin conexión a internet") }
            } catch (e: CancellationException) {
                throw e // la búsqueda anterior se canceló: no es un error
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = "Error inesperado: ${e.message}") }
            }
        }
    }
}
