package dev.mariinkys.kantan.data.repository

import dev.mariinkys.kantan.data.local.dao.KanjiDao
import dev.mariinkys.kantan.data.local.dao.TermDao
import dev.mariinkys.kantan.data.local.entity.KanjiEntity
import dev.mariinkys.kantan.data.mapper.groupAndMap
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.KanjiEntry
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import dev.mariinkys.kantan.util.RomajiConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
            val kana = if (!RomajiConverter.isJapanese(q)) RomajiConverter.convert(q) else q

            val rows = if (RomajiConverter.isJapanese(q)) {
                termDao.searchByPrefix(q)
            } else {
                val prefixRows = if (kana != q && kana.isNotBlank())
                    termDao.searchByPrefix(kana) else emptyList()
                val ftsQuery = buildFtsQuery(q)
                val ftsRows = if (ftsQuery.isNotBlank())
                    runCatching { termDao.searchByFts(ftsQuery) }.getOrElse { emptyList() }
                else emptyList()
                val seen = mutableSetOf<Long>()
                (prefixRows + ftsRows).filter { seen.add(it.id) }
            }

            emit(rows.groupAndMap().sortedByDescending { rankScore(it, q, kana) })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getEntry(sequence: Int): DictionaryEntry? =
        termDao.getBySequences(listOf(sequence))
            .takeIf { it.isNotEmpty() }
            ?.groupAndMap()
            ?.firstOrNull()

    override suspend fun getEntryByTerm(term: String): DictionaryEntry? =
        termDao.searchByPrefix(term)
            .groupAndMap()
            .firstOrNull { it.expression == term || it.variants.contains(term) }

    /**
     * Gets a string with different terms divided by either ',' or '、' and returns all the EXACTLY matching dictionary entries
     * Ej: 女、学校、学生 or 女,学校,学生
     */
    override suspend fun getBulkEntriesByTerms(terms: String): List<DictionaryEntry> =
        coroutineScope {
            if (terms.isBlank()) return@coroutineScope emptyList()

            terms.split(',', '、')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct() // Avoid querying the same term twice if the user inputs "女, 女"
                .map { term ->
                    async { getEntryByTerm(term) }
                }
                .awaitAll()
                .filterNotNull()
        }

    override suspend fun getRandomEntry(): DictionaryEntry? {
        val randomSense = termDao.getRandomCommonTerm() ?: return null
        return getEntry(randomSense.sequence)
    }

    override suspend fun getKanji(character: String): KanjiEntry? =
        kanjiDao.getByCharacter(character)?.toDomain()

    override suspend fun getKanjiForWord(expression: String): List<KanjiEntry> {
        val chars = expression.filter { it.isKanji() }.map { it.toString() }.distinct()
        if (chars.isEmpty()) return emptyList()
        return kanjiDao.getByCharacters(chars).map { it.toDomain() }
    }

    private fun buildFtsQuery(raw: String): String {
        val words = raw
            .replace(Regex("""["()\-*:]+"""), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
        if (words.isEmpty()) return ""
        return words.joinToString(" ") { "$it*" }
    }

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

internal fun Char.isKanji(): Boolean {
    val cp = code
    return cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0xF900..0xFAFF
}

private fun rankScore(entry: DictionaryEntry, query: String, kanaQuery: String = ""): Int {
    val q = query.lowercase()
    var boost = 0

    // Reading match boost important for romaji/kana searches.
    // Exact reading match (おんな == おんな) must beat prefix match (おんな < おんながた).
    if (kanaQuery.isNotBlank()) {
        // entry.reading may be "おんな、 おにょ" so check each reading individually
        val readings = entry.reading.split("、").map { it.trim() }
        when {
            readings.any { it == kanaQuery } -> boost += 200_000
            readings.any { it.startsWith(kanaQuery) } -> boost += 30_000
        }
    }

    // Gloss match boost (for English searches)
    for ((senseIndex, sense) in entry.senses.withIndex()) {
        for ((glossIndex, gloss) in sense.glosses.withIndex()) {
            val g = gloss.lowercase()
            val isExact = g == q
            val isPrefix = g.startsWith(q)
            val contains = g.contains(q)
            if (!isExact && !isPrefix && !contains) continue

            val positionWeight = 1.0 / ((senseIndex + 1) * (glossIndex + 1))
            boost += when {
                isExact -> (100_000 * positionWeight).toInt()
                isPrefix -> (30_000 * positionWeight).toInt()
                else -> (5_000 * positionWeight).toInt()
            }
        }
    }

    // JMDict frequency as a baseline, normalized so it doesn't drown out
    val frequencyWeight = entry.score.coerceAtLeast(0) / 2

    // Shorter expressions are more fundamental for a given meaning.
    val lengthPenalty = entry.expression.length * 15_000

    return boost + frequencyWeight - lengthPenalty
}