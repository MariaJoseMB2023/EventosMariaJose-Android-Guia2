
package com.example.eventosmariajose.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eventos_favoritos")
data class EventoFavorito(
    @PrimaryKey val idEvento: Long,
    val nombre: String,
    val lugar: String,
    val fechaHora: String,
    val categoria: String,
    val estado: String
)
