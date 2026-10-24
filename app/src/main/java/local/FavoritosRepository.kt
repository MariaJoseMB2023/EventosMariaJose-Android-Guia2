
package com.example.eventosmariajose.local

class FavoritosRepository(
    private val dao: EventoFavoritoDao
) {
    val favoritos = dao.listar()

    suspend fun guardar(item: EventoFavorito) {
        dao.guardar(item)
    }

    suspend fun eliminar(item: EventoFavorito) {
        dao.eliminar(item)
    }

    suspend fun existe(id: Long): Boolean {
        return dao.existe(id)
    }
}
