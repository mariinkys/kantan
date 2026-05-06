package dev.mariinkys.kantan.domain.repository

import dev.mariinkys.kantan.domain.model.DictionaryEntry
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    /**
     * Retrieves all favorite entries, fully populated with definitions and variants.
     */
    fun getAll(): Flow<List<DictionaryEntry>>

    /**
     * Checks if a word is favorite using its unique sequence ID.
     */
    fun isFavorite(id: Int): Flow<Boolean>

    /**
     * Adds an entry to favorites by its sequence ID.
     */
    suspend fun add(id: Int)

    /**
     * Removes an entry from favorites by its sequence ID.
     */
    suspend fun remove(id: Int)

    /**
     * Toggles the favorite status for a given sequence ID.
     */
    suspend fun toggle(id: Int)
}