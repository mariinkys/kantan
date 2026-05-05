package dev.mariinkys.kantan.data.repository

import dev.mariinkys.kantan.data.local.dao.FavoriteDao
import dev.mariinkys.kantan.data.local.entity.FavoriteEntity
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao
) : FavoritesRepository {

    /**
     * Favorites only store (expression, reading) — we reconstruct a minimal
     * DictionaryEntry so the favorites screen can display and navigate to them
     * without a second DB join. Full definitions load when the user taps through
     * to the detail screen as normal.
     */
    override fun getAll(): Flow<List<DictionaryEntry>> =
        dao.getAll().map { favorites ->
            favorites.map { fav ->
                DictionaryEntry(
                    expression = fav.expression,
                    reading = fav.reading,
                    definitions = emptyList(),  // loaded on demand in detail screen
                    rules = "",
                    definitionTags = "",
                    tags = ""
                )
            }
        }

    override fun isFavorite(expression: String, reading: String): Flow<Boolean> =
        dao.isFavorite(expression, reading)

    override suspend fun add(expression: String, reading: String) {
        dao.insert(FavoriteEntity(expression = expression, reading = reading))
    }

    override suspend fun remove(expression: String, reading: String) {
        dao.delete(expression, reading)
    }

    override suspend fun toggle(expression: String, reading: String) {
        if (dao.isFavorite(expression, reading).first()) {
            remove(expression, reading)
        } else {
            add(expression, reading)
        }
    }
}