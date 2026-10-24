
package com.example.eventosmariajose.local

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritosViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao =
        AppDatabase.getDatabase(application).favoritoDao()

    private val repository = FavoritosRepository(dao)

    val favoritos = repository.favoritos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun guardar(item: EventoFavorito) {
        viewModelScope.launch {
            repository.guardar(item)
        }
    }

    fun eliminar(item: EventoFavorito) {
        viewModelScope.launch {
            repository.eliminar(item)
        }
    }
}
