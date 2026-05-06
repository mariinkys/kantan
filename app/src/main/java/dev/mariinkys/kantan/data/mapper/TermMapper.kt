package dev.mariinkys.kantan.data.mapper

import dev.mariinkys.kantan.data.local.StoredSense
import dev.mariinkys.kantan.data.local.entity.TermEntity
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.Example
import dev.mariinkys.kantan.domain.model.Sense
import dev.mariinkys.kantan.util.resolveTag
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun List<TermEntity>.groupAndMap(): List<DictionaryEntry> {
    return this.groupBy { it.sequence }
        .map { (sequenceNumber, rows) ->
            val primaryRow = rows.maxByOrNull { it.score } ?: rows.first()

            val allForms = rows.map { it.expression }.distinct()

            // Only include readings from rows that aren't marked as non-standard
            val allReadings = rows
                .filter { !it.termTags.contains("⛬") }
                .map { it.reading }
                .distinct()

            val senses = rows.flatMap { row ->
                runCatching {
                    json.decodeFromString<List<StoredSense>>(row.sensesJson)
                }.getOrDefault(emptyList())
            }
                .distinctBy { sense ->
                    // Create a unique key using the text of the definitions
                    val glossKey = sense.glosses.joinToString("|").lowercase()
                    val posKey = sense.posTags.joinToString(",")
                    "$glossKey-$posKey"
                }
                .map { it.toDomain() }
                .filter { it.glosses.isNotEmpty() }

            val rules = rows.firstNotNullOfOrNull { it.rules.takeIf { r -> r.isNotBlank() } } ?: ""
            val tags =
                rows.firstNotNullOfOrNull { it.termTags.takeIf { t -> t.isNotBlank() } } ?: ""

            DictionaryEntry(
                id = sequenceNumber,
                expression = primaryRow.expression,
                reading = allReadings.joinToString("、 "),
                variants = allForms,
                senses = senses,
                rules = rules,
                tags = tags
            )
        }
        .filter { it.senses.isNotEmpty() }
}

private fun StoredSense.toDomain(): Sense {
    val filteredTags = posTags.filter { tag ->
        tag.toIntOrNull() == null && tag != "forms"
    }

    val truePosTag = filteredTags.firstOrNull() ?: ""
    val posLabel = if (truePosTag.isNotBlank()) resolveTag(truePosTag) else ""

    return Sense(
        partOfSpeech = posLabel,
        posTags = filteredTags,
        glosses = glosses,
        examples = examples.map { Example(it.japanese, it.english) },
        info = info
    )
}