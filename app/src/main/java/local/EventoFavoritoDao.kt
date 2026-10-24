
package com.example.eventosmariajose.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoFavoritoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(item: EventoFavorito)

    @Query("SELECT * FROM eventos_favoritos")
    fun listar(): Flow<List<EventoFavorito>>

    @Delete
    suspend fun eliminar(item: EventoFavorito)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM eventos_favoritos WHERE idEvento = :id)"
    )
    suspend fun existe(id: Long): Boolean
}
