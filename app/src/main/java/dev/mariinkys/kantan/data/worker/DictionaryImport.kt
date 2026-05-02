package dev.mariinkys.kantan.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.mariinkys.kantan.data.local.dao.KanjiDao
import dev.mariinkys.kantan.data.local.dao.TermDao
import dev.mariinkys.kantan.data.local.entity.KanjiEntity
import dev.mariinkys.kantan.data.local.entity.TermEntity
import dev.mariinkys.kantan.data.local.entity.TermFtsEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@HiltWorker
class DictionaryImportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val termDao: TermDao,
    private val kanjiDao: KanjiDao
) : CoroutineWorker(appContext, params) {

    private val json = Json { ignoreUnknownKeys = true }
    private val tag = "DictImport"

    override suspend fun doWork(): Result {
        return try {
            if (termDao.count() > 0 && kanjiDao.count() > 0) {
                Log.i(tag, "DB already populated, skipping")
                return Result.success()
            }
            importTermBanks()
            importKanjiBanks()
            Log.i(tag, "Done — terms: ${termDao.count()}, kanji: ${kanjiDao.count()}")
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Import failed", e)
            Result.retry()
        }
    }

    private suspend fun importTermBanks() {
        val assets = applicationContext.assets
        val files = assets.list("dict/jmdict")
            ?.filter { it.startsWith("term_bank_") && it.endsWith(".json") }
            ?.sorted() ?: return
        Log.i(tag, "Found ${files.size} term bank files")

        for (file in files) {
            val rows = json.parseToJsonElement(
                assets.open("dict/jmdict/$file").bufferedReader().readText()
            ).jsonArray

            val batch = rows.mapNotNull { it.jsonArray.toTermEntity() }

            val insertedIds = termDao.insertAll(batch)

            val ftsBatch = batch.mapIndexedNotNull { index, entity ->
                val generatedId = insertedIds[index]
                if (generatedId != -1L) {
                    TermFtsEntity(
                        termId = generatedId,
                        expression = entity.expression,
                        definitionsText = entity.definitionsText
                    )
                } else null
            }
            termDao.insertFts(ftsBatch)

            Log.d(tag, "Processed ${batch.size} terms from $file")
        }
    }

    private fun JsonArray.toTermEntity(): TermEntity? = runCatching {
        val tags = this[2].jsonPrimitive.content

        if (tags.contains("forms") || tags.contains("kana") || tags.contains("kanji")) {
            return null
        }

        val definitions = this[5].jsonArray.flatMap { element ->
            when (element) {
                is JsonPrimitive -> {
                    val content = element.content.trim()
                    if (content.isNotBlank()) listOf(content) else emptyList()
                }

                is JsonObject -> extractGlossaryOnly(element)
                else -> emptyList()
            }
        }

        if (definitions.isEmpty()) return null

        TermEntity(
            expression = this[0].jsonPrimitive.content,
            reading = this[1].jsonPrimitive.content,
            definitionTags = tags,
            rules = this[3].jsonPrimitive.content,
            score = this[4].jsonPrimitive.int,
            definitions = definitions,
            sequence = this[6].jsonPrimitive.int,
            termTags = this[7].jsonPrimitive.content,
            definitionsText = definitions.joinToString(" ")
        )
    }.getOrNull()

    private fun extractGlossaryOnly(element: JsonElement): List<String> {
        val results = mutableListOf<String>()

        if (element is JsonObject && element["type"]?.jsonPrimitive?.content == "structured-content") {
            val contentArray = element["content"]?.jsonArray ?: return emptyList()

            for (item in contentArray) {
                val itemObj = item as? JsonObject ?: continue
                val dataContent =
                    itemObj["data"]?.jsonObject?.get("content")?.jsonPrimitive?.content

                if (dataContent == "glossary") {
                    val text = extractTextFromNode(itemObj["content"] ?: continue)
                    if (text.isNotBlank()) results.add(text)
                }
            }
        } else if (element is JsonPrimitive) {
            results.add(element.content)
        }

        return results
    }

    private fun extractTextFromNode(node: JsonElement): String = when (node) {
        is JsonPrimitive -> node.content
        is JsonArray -> node.joinToString(" ") { extractTextFromNode(it) }
        is JsonObject -> {
            val tag = node["tag"]?.jsonPrimitive?.content
            val content = node["content"]
            if (node["data"] != null && content == null) return ""
            val inner = if (content != null) extractTextFromNode(content) else ""
            when (tag) {
                "tr" -> (content as? JsonArray)
                    ?.joinToString(" | ") { extractTextFromNode(it).trim() } ?: inner

                "table" -> if (content != null) extractTextFromNode(content).trim() else inner
                "div", "p", "li" -> "\n$inner"
                else -> inner
            }
        }
    }

    private suspend fun importKanjiBanks() {
        val assets = applicationContext.assets
        val files = assets.list("dict/kanjidic")
            ?.filter { it.startsWith("kanji_bank_") && it.endsWith(".json") }
            ?.sorted() ?: return
        Log.i(tag, "Found ${files.size} kanji bank files")

        for (file in files) {
            val rows = json.parseToJsonElement(
                assets.open("dict/kanjidic/$file").bufferedReader().readText()
            ).jsonArray
            val batch = rows.mapNotNull { it.jsonArray.toKanjiEntity() }
            kanjiDao.insertAll(batch)
            Log.d(tag, "Processed ${batch.size} kanji from $file")
        }
    }

    private fun JsonArray.toKanjiEntity(): KanjiEntity? = runCatching {
        val stats = this[5].jsonObject
        fun stat(key: String) = stats[key]?.jsonPrimitive?.content?.toIntOrNull()
        KanjiEntity(
            character = this[0].jsonPrimitive.content,
            onyomi = this[1].jsonPrimitive.content,
            kunyomi = this[2].jsonPrimitive.content,
            tags = this[3].jsonPrimitive.content,
            meanings = this[4].jsonArray.map { it.jsonPrimitive.content },
            grade = stat("grade"),
            strokeCount = stat("strokes"),
            jlptLevel = stat("jlpt"),
            frequency = stat("freq")
        )
    }.getOrNull()
}