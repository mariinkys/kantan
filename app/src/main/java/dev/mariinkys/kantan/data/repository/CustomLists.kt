package dev.mariinkys.kantan.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.mariinkys.kantan.data.local.dao.TermDao
import dev.mariinkys.kantan.data.mapper.groupAndMap
import dev.mariinkys.kantan.domain.model.CustomList
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.ListEntry
import dev.mariinkys.kantan.domain.model.StudyState
import dev.mariinkys.kantan.domain.repository.CustomListsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CustomListsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val termDao: TermDao
) : CustomListsRepository {

    companion object {
        private val KEY = stringPreferencesKey("custom_lists")
    }

    private fun encode(lists: List<CustomList>): String =
        Json.encodeToString(lists)

    private fun decode(raw: String?): List<CustomList> =
        if (raw.isNullOrBlank()) emptyList()
        else runCatching { Json.decodeFromString<List<CustomList>>(raw) }.getOrDefault(emptyList())

    private val listsFlow: Flow<List<CustomList>> = dataStore.data
        .map { prefs -> decode(prefs[KEY]) }

    override fun getAllLists(): Flow<List<CustomList>> = listsFlow

    override fun getList(listId: Int): Flow<CustomList?> =
        listsFlow.map { lists -> lists.firstOrNull { it.id == listId } }

    override fun getListEntries(listId: Int): Flow<List<DictionaryEntry>> =
        listsFlow.map { lists ->
            lists.firstOrNull { it.id == listId }?.let { list ->
                termDao.getBySequences(list.entries.map { it.sequenceId }).groupAndMap()
            } ?: emptyList()
        }

    override fun getListIdsForEntry(sequenceId: Int): Flow<List<Int>> =
        listsFlow.map { lists ->
            lists.filter { it -> sequenceId in it.entries.map { it.sequenceId } }.map { it.id }
        }

    override suspend fun createList(name: String) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY])
            val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
            prefs[KEY] =
                encode(current + CustomList(id = newId, name = name, entries = emptyList()))
        }
    }

    override suspend fun deleteList(listId: Int) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY])
            prefs[KEY] = encode(current.filter { it.id != listId })
        }
    }

    override suspend fun renameList(listId: Int, name: String) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY])
            prefs[KEY] = encode(current.map { if (it.id == listId) it.copy(name = name) else it })
        }
    }

    override suspend fun addEntry(listId: Int, sequenceId: Int) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY] ?: "")
            prefs[KEY] = encode(current.map { list ->
                if (list.id == listId && list.entries.none { it.sequenceId == sequenceId }) {
                    list.copy(entries = list.entries + ListEntry(sequenceId))
                } else {
                    list
                }
            })
        }
    }

    override suspend fun addMultipleEntry(listId: Int, sequenceIds: List<Int>) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY] ?: "")
            prefs[KEY] = encode(current.map { list ->
                if (list.id == listId) {
                    val existingIds = list.entries.map { it.sequenceId }.toSet()
                    val newEntries = sequenceIds
                        .filter { it !in existingIds }
                        .map { ListEntry(sequenceId = it) }

                    list.copy(entries = list.entries + newEntries)
                } else {
                    list
                }
            })
        }
    }

    override suspend fun removeEntry(listId: Int, sequenceId: Int) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY] ?: "")
            prefs[KEY] = encode(current.map { list ->
                if (list.id == listId) {
                    list.copy(entries = list.entries.filterNot { it.sequenceId == sequenceId })
                } else {
                    list
                }
            })
        }
    }

    override suspend fun updateStudyState(listId: Int, sequenceId: Int, newState: StudyState) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY])
            prefs[KEY] = encode(current.map { list ->
                if (list.id == listId) {
                    list.copy(entries = list.entries.map { entry ->
                        if (entry.sequenceId == sequenceId) {
                            entry.copy(studyState = newState)
                        } else entry
                    })
                } else list
            })
        }
    }

    override suspend fun isEntryInList(listId: Int, sequenceId: Int): Boolean =
        listsFlow.map { lists ->
            lists.firstOrNull { it.id == listId }
                ?.entries
                ?.any { it.sequenceId == sequenceId } ?: false
        }.first()
}