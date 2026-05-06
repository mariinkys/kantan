package dev.mariinkys.kantan.data.repository

import dev.mariinkys.kantan.data.local.StoredSense
import dev.mariinkys.kantan.data.local.dao.KanjiDao
import dev.mariinkys.kantan.data.local.dao.TermDao
import dev.mariinkys.kantan.data.local.entity.KanjiEntity
import dev.mariinkys.kantan.data.local.entity.TermEntity
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.Example
import dev.mariinkys.kantan.domain.model.KanjiEntry
import dev.mariinkys.kantan.domain.model.Sense
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import dev.mariinkys.kantan.util.RomajiConverter
import dev.mariinkys.kantan.util.resolveTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DictionaryRepositoryImpl @Inject constructor(
    private val termDao: TermDao,
    private val kanjiDao: KanjiDao
) : DictionaryRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun search(query: String): Flow<List<DictionaryEntry>> {
        val q = query.trim()
        if (q.isBlank()) return flowOf(emptyList())
        return flow {
            val rows = if (RomajiConverter.isJapanese(q)) {
                termDao.searchByPrefix(q)
            } else {
                val kana = RomajiConverter.convert(q)
                val prefixRows = if (kana != q && kana.isNotBlank())
                    termDao.searchByPrefix(kana) else emptyList()
                val ftsQuery = buildFtsQuery(q)
                val ftsRows = if (ftsQuery.isNotBlank())
                    runCatching { termDao.searchByFts(ftsQuery) }.getOrElse { emptyList() }
                else emptyList()
                val seen = mutableSetOf<Long>()
                (prefixRows + ftsRows).filter { seen.add(it.id) }
            }
            emit(rows.groupAndMap())
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getEntry(expression: String, reading: String): DictionaryEntry? =
        termDao.getByExpressionAndReading(expression, reading)
            .takeIf { it.isNotEmpty() }
            ?.groupAndMap()
            ?.firstOrNull()

    override suspend fun getRandomEntry(): DictionaryEntry? {
        val randomSense = termDao.getRandomCommonTerm() ?: return null
        return getEntry(randomSense.expression, randomSense.reading)
    }

    override suspend fun getKanji(character: String): KanjiEntry? =
        kanjiDao.getByCharacter(character)?.toDomain()

    override suspend fun getKanjiForWord(expression: String): List<KanjiEntry> {
        val chars = expression.filter { it.isKanji() }.map { it.toString() }.distinct()
        if (chars.isEmpty()) return emptyList()
        return kanjiDao.getByCharacters(chars).map { it.toDomain() }
    }

    /**
     * Groups flat TermEntity rows into one DictionaryEntry per (expression, reading).
     * Each row becomes one Sense — preserving POS, glosses, and examples separately.
     * "forms" rows (definitionTags == "forms") are skipped — they're metadata tables,
     * not definitions.
     */
    private fun List<TermEntity>.groupAndMap(): List<DictionaryEntry> =
        groupBy { it.expression to it.reading }
            .map { (key, rows) ->
                val (expression, reading) = key
                val senses = rows
                    .filter { it.definitionTags != "forms" }
                    .flatMap { entity ->
                        runCatching {
                            json.decodeFromString<List<StoredSense>>(entity.sensesJson)
                                .map { it.toDomain() }
                        }.getOrDefault(emptyList())
                    }
                    .filter { it.glosses.isNotEmpty() }
                val rules =
                    rows.firstNotNullOfOrNull { it.rules.takeIf { r -> r.isNotBlank() } } ?: ""
                val tags =
                    rows.firstNotNullOfOrNull { it.termTags.takeIf { t -> t.isNotBlank() } } ?: ""
                DictionaryEntry(
                    expression = expression,
                    reading = reading,
                    senses = senses,
                    rules = rules,
                    tags = tags
                )
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

    private fun StoredSense.toDomain(): Sense {
        val truePosTag = posTags.firstOrNull { it != "1" && it != "2" } ?: ""
        val posLabel = when {
            posTags.contains("forms") -> "Forms"
            truePosTag.isNotBlank() -> resolveTag(truePosTag)
            else -> ""
        }
        return Sense(
            partOfSpeech = posLabel,
            posTags = posTags.filter { it != "1" && it != "2" },
            glosses = glosses,
            examples = examples.map { Example(it.japanese, it.english) },
            info = info
        )
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

private fun Char.isKanji(): Boolean {
    val cp = code
    return cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0xF900..0xFAFF
}