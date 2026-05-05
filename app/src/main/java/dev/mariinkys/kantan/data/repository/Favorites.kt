package dev.mariinkys.kantan.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : FavoritesRepository {

    companion object {
        private val KEY = stringSetPreferencesKey("saved_words")

        // Encode as "expression|reading", pipe is safe since neither field
        // can contain it (I think) (JMdict uses CJK/kana/Latin, never ASCII pipe)
        private fun encode(expression: String, reading: String) = "$expression|$reading"
        private fun decode(raw: String): Pair<String, String> {
            val i = raw.indexOf('|')
            return raw.substring(0, i) to raw.substring(i + 1)
        }
    }

    override fun getAll(): Flow<List<DictionaryEntry>> =
        dataStore.data.map { prefs ->
            (prefs[KEY] ?: emptySet())
                .map { raw ->
                    val (expression, reading) = decode(raw)
                    DictionaryEntry(
                        expression = expression,
                        reading = reading,
                        definitions = emptyList(), // loaded on demand in detail screen
                        rules = "",
                        definitionTags = "",
                        tags = ""
                    )
                }
        }

    override fun isFavorite(expression: String, reading: String): Flow<Boolean> =
        dataStore.data.map { prefs ->
            encode(expression, reading) in (prefs[KEY] ?: emptySet())
        }

    override suspend fun add(expression: String, reading: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY] ?: emptySet()
            prefs[KEY] = current + encode(expression, reading)
        }
    }

    override suspend fun remove(expression: String, reading: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY] ?: emptySet()
            prefs[KEY] = current - encode(expression, reading)
        }
    }

    override suspend fun toggle(expression: String, reading: String) {
        val key = encode(expression, reading)
        dataStore.edit { prefs ->
            val current = prefs[KEY] ?: emptySet()
            prefs[KEY] = if (key in current) current - key else current + key
        }
    }
}