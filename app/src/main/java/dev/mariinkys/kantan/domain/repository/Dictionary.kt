package dev.mariinkys.kantan.domain.repository

import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.KanjiEntry
import kotlinx.coroutines.flow.Flow

interface DictionaryRepository {
    fun search(query: String): Flow<List<DictionaryEntry>>
    suspend fun getEntryById(id: Long): DictionaryEntry?
    suspend fun getKanji(character: String): KanjiEntry?
    suspend fun getKanjiForWord(expression: String): List<KanjiEntry>
}