package dev.mariinkys.kantan.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.mariinkys.kantan.data.local.StoredExample
import dev.mariinkys.kantan.data.local.StoredSense
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
            termDao.insertAll(batch)
            termDao.insertFts(batch.map { TermFtsEntity(it.definitionsText) })
            Log.d(tag, "Processed ${batch.size} terms from $file")
        }
    }

    private fun JsonArray.toTermEntity(): TermEntity? = runCatching {
        val expression = this[0].jsonPrimitive.content
        val reading = this[1].jsonPrimitive.content
        val definitionTags = this[2].jsonPrimitive.content
        val rules = this[3].jsonPrimitive.content
        val score = this[4].jsonPrimitive.int
        val rawDefs = this[5].jsonArray
        val sequence = this[6].jsonPrimitive.int
        val termTags = this[7].jsonPrimitive.content

        val sense = parseSense(definitionTags, rawDefs)
        val sensesJson = json.encodeToString(listOf(sense))
        // Flat gloss text for FTS — skip if this is a "forms" row
        val definitionsText = if (definitionTags == "forms") ""
        else sense.glosses.joinToString(" ")

        TermEntity(
            expression = expression,
            reading = reading,
            definitionTags = definitionTags,
            rules = rules,
            score = score,
            sequence = sequence,
            termTags = termTags,
            sensesJson = sensesJson,
            definitionsText = definitionsText
        )
    }.getOrNull()

    /**
     * Parses one term_bank row's definition array into a [StoredSense].
     *
     * The definition array elements can be:
     *  - JsonPrimitive → plain gloss string
     *  - JsonObject { type:"structured-content", content: [...] }
     *    The content array contains typed ul/li structures:
     *      ul[data.content="glossary"]  → li items are glosses
     *      ul[data.content="examples"]  → li pairs are (jp sentence, en translation)
     *      table[data.content="formsTable"] → variant forms table, skip as gloss
     *  - JsonObject type:"text", text:"..."  → info/note string
     */
    private fun parseSense(definitionTags: String, rawDefs: JsonArray): StoredSense {
        val posTags = definitionTags.split(" ").filter { it.isNotBlank() }
        val glosses = mutableListOf<String>()
        val examples = mutableListOf<StoredExample>()
        val info = mutableListOf<String>()

        for (element in rawDefs) {
            when (element) {
                is JsonPrimitive -> {
                    val s = element.content.trim()
                    if (s.isNotBlank()) glosses.add(s)
                }

                is JsonObject -> {
                    when (element["type"]?.jsonPrimitive?.content) {
                        "structured-content" -> {
                            val content = element["content"] ?: continue
                            parseStructuredContent(content, glosses, examples, info)
                        }

                        "text" -> {
                            val t = element["text"]?.jsonPrimitive?.content?.trim()
                            if (!t.isNullOrBlank()) info.add(t)
                        }
                    }
                }

                else -> Unit
            }
        }

        return StoredSense(
            posTags = posTags,
            glosses = glosses,
            examples = examples,
            info = info
        )
    }

    /**
     * Walks a structured-content node tree, routing to the right extractor
     * based on the [data.content] marker on ul/table elements.
     */
    private fun parseStructuredContent(
        node: JsonElement,
        glosses: MutableList<String>,
        examples: MutableList<StoredExample>,
        info: MutableList<String>
    ) {
        when (node) {
            is JsonPrimitive -> {
                val s = node.content.trim()
                if (s.isNotBlank()) info.add(s)
            }

            is JsonArray -> node.forEach { parseStructuredContent(it, glosses, examples, info) }
            is JsonObject -> {
                val dataContent = node["data"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                val content = node["content"]

                when (dataContent) {
                    "glossary" -> extractGlossary(content, glosses)
                    "examples" -> extractExamples(content, examples)
                    "references" -> {
                        val refText = flatText(node).trim()
                        if (refText.isNotBlank()) info.add(refText)
                    }

                    "formsTable" -> { /* we skip them, forms rows handled separately */
                    }

                    else -> if (content != null) {
                        parseStructuredContent(content, glosses, examples, info)
                    }
                }
            }
        }
    }

    /** Extracts "li" text nodes from a glossary ul as individual glosses. */
    private fun extractGlossary(content: JsonElement?, glosses: MutableList<String>) {
        if (content == null) return
        val items = content as? JsonArray ?: return
        for (item in items) {
            val text = flatText(item).trim()
            if (text.isNotBlank()) {
                glosses.add(text)
            }
        }
    }

    /**
     * Extracts example pairs from an examples ul.
     * Structure: first li is Japanese, following li(s) with lang="en" are translations.
     * A single ul can contain multiple (jp, en) pairs interleaved.
     */
    private fun extractExamples(content: JsonElement?, examples: MutableList<StoredExample>) {
        if (content !is JsonArray) return
        var pendingJp: String? = null
        for (item in content) {
            if (item !is JsonObject) continue
            val lang = item["lang"]?.jsonPrimitive?.content
            val text = flatText(item).trim()
            if (text.isBlank()) continue
            when {
                lang == "en" && pendingJp != null -> {
                    examples.add(StoredExample(japanese = pendingJp, english = text))
                    pendingJp = null
                }

                lang != "en" -> pendingJp = text
                else -> pendingJp = text
            }
        }
    }

    /** Recursively collects all text content from a node as a flat string. */
    private fun flatText(node: JsonElement): String = when (node) {
        is JsonPrimitive -> node.content
        is JsonArray -> node.joinToString("") { flatText(it) }
        is JsonObject -> {
            val text = node["text"]?.jsonPrimitive?.content
            val content = node["content"]

            when {
                text != null -> text
                content != null -> flatText(content)
                else -> ""
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

