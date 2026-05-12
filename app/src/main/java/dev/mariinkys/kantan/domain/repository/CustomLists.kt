package dev.mariinkys.kantan.domain.repository

import dev.mariinkys.kantan.domain.model.CustomList
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.StudyState
import kotlinx.coroutines.flow.Flow

interface CustomListsRepository {
    fun getAllLists(): Flow<List<CustomList>>
    fun getList(listId: Int): Flow<CustomList?>
    fun getListEntries(listId: Int): Flow<List<DictionaryEntry>?>

    fun getListIdsForEntry(sequenceId: Int): Flow<List<Int>>
    suspend fun createList(name: String)
    suspend fun deleteList(listId: Int)
    suspend fun renameList(listId: Int, name: String)
    suspend fun addEntry(listId: Int, sequenceId: Int)
    suspend fun addMultipleEntry(listId: Int, sequenceIds: List<Int>)
    suspend fun removeEntry(listId: Int, sequenceId: Int)


    suspend fun updateStudyState(listId: Int, sequenceId: Int, newState: StudyState)

    suspend fun isEntryInList(listId: Int, sequenceId: Int): Boolean
}