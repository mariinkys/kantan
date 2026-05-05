package dev.mariinkys.kantan.domain.repository

import dev.mariinkys.kantan.domain.model.DictionaryEntry
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAll(): Flow<List<DictionaryEntry>>
    fun isFavorite(expression: String, reading: String): Flow<Boolean>
    suspend fun add(expression: String, reading: String)
    suspend fun remove(expression: String, reading: String)
    suspend fun toggle(expression: String, reading: String)
}