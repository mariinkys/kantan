package dev.mariinkys.kantan.data.repository

import dev.mariinkys.kantan.data.local.dao.KanjiDao
import dev.mariinkys.kantan.data.local.dao.TermDao
import dev.mariinkys.kantan.data.local.entity.KanjiEntity
import dev.mariinkys.kantan.data.local.entity.TermEntity
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.KanjiEntry
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import dev.mariinkys.kantan.util.RomajiConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DictionaryRepositoryImpl @Inject constructor(
    private val termDao: TermDao,
    private val kanjiDao: KanjiDao
) : DictionaryRepository {

    override fun search(query: String): Flow<List<DictionaryEntry>> {
        val q = query.trim()
        if (q.isBlank()) return flowOf(emptyList())

        return flow {
            val isJapanese = RomajiConverter.isJapanese(q)

            val rawResults = if (isJapanese) {
                termDao.searchByPrefix(q)
            } else {
                val kana = RomajiConverter.convert(q)
                val convertedSuccessfully = kana != q && kana.isNotBlank()

                val prefixResults =
                    if (convertedSuccessfully) termDao.searchByPrefix(kana) else emptyList()
                val ftsQuery = buildFtsQuery(q)
                val ftsResults = if (ftsQuery.isNotBlank()) {
                    runCatching {
                        termDao.searchByFts(ftsQuery, q, limit = 200)
                    }.getOrElse { emptyList() }
                } else emptyList()

                val seen = mutableSetOf<Long>()
                (prefixResults + ftsResults).filter { seen.add(it.id) }
            }

            val grouped = rawResults
                .groupBy { it.expression }
                .values
                .map { variations ->
                    val bestMatch = variations.maxBy { it.score }
                    DictionaryEntry(
                        id = bestMatch.id,
                        expression = bestMatch.expression,
                        reading = variations.map { it.reading }.distinct().joinToString(", "),
                        definitions = variations.flatMap { it.definitions }.distinct(),
                        rules = bestMatch.rules,
                        tags = bestMatch.termTags
                    )
                }

            emit(grouped)
        }.flowOn(Dispatchers.IO)
    }

    private fun buildFtsQuery(raw: String): String {
        val words = raw.split(Regex("\\s+")).filter { it.length >= 2 }
        if (words.isEmpty()) return ""

        return words.joinToString(" ") { "\"$it\" OR $it*" }
    }

    override suspend fun getKanji(character: String): KanjiEntry? =
        kanjiDao.getByCharacter(character)?.toDomain()

    override suspend fun getKanjiForWord(expression: String): List<KanjiEntry> {
        val chars = expression.filter { it.isKanji() }.map { it.toString() }.distinct()
        if (chars.isEmpty()) return emptyList()
        return kanjiDao.getByCharacters(chars).map { it.toDomain() }
    }


    private fun TermEntity.toDomain() = DictionaryEntry(
        id = id,
        expression = expression,
        reading = reading,
        definitions = definitions,
        rules = rules,
        tags = termTags
    )

    private fun KanjiEntity.toDomain() = KanjiEntry(
        character = character,
        onyomi = onyomi.split(" ").filter { it.isNotBlank() },
        kunyomi = kunyomi.split(" ").filter { it.isNotBlank() },
        meanings = meanings,
        strokeCount = strokeCount,
        grade = grade,
        jlptLevel = jlptLevel,
        frequency = frequency
    )
}

private fun Char.isKanji(): Boolean {
    val cp = code
    return cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0xF900..0xFAFF
}