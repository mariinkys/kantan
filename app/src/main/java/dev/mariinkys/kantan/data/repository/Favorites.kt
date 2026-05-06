package dev.mariinkys.kantan.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.mariinkys.kantan.data.local.dao.TermDao
import dev.mariinkys.kantan.data.mapper.groupAndMap
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val termDao: TermDao
) : FavoritesRepository {

    companion object {
        private val KEY = stringSetPreferencesKey("favorite_sequence_ids")
    }

    override fun getAll(): Flow<List<DictionaryEntry>> =
        dataStore.data.map { prefs ->
            val idSet = prefs[KEY] ?: emptySet()
            if (idSet.isEmpty()) return@map emptyList()
            val sequenceIds = idSet.mapNotNull { it.toIntOrNull() }
            val entities = termDao.getBySequences(sequenceIds)

            entities.groupAndMap()
        }

    override fun isFavorite(id: Int): Flow<Boolean> =
        dataStore.data.map { prefs ->
            id.toString() in (prefs[KEY] ?: emptySet())
        }

    override suspend fun add(id: Int) {
        dataStore.edit { prefs ->
            val current = prefs[KEY] ?: emptySet()
            prefs[KEY] = current + id.toString()
        }
    }

    override suspend fun remove(id: Int) {
        dataStore.edit { prefs ->
            val current = prefs[KEY] ?: emptySet()
            prefs[KEY] = current - id.toString()
        }
    }

    override suspend fun toggle(id: Int) {
        dataStore.edit { prefs ->
            val current = prefs[KEY] ?: emptySet()
            val idStr = id.toString()
            prefs[KEY] = if (idStr in current) current - idStr else current + idStr
        }
    }
}