package dev.mariinkys.kantan.domain.model

data class DictionaryEntry(
    val expression: String,
    val reading: String,
    val senses: List<Sense>,
    val rules: String,
    val tags: String
) {
    /** First gloss of first sense — for list/favorites display. */
    val shortDefinition: String
        get() = senses.firstOrNull()?.glosses?.firstOrNull()?.trim() ?: ""
}

data class Sense(
    /** Human-readable POS string, e.g. "Noun", "Ichidan verb", "Expression" */
    val partOfSpeech: String,
    /** Raw POS tag chips for display, e.g. ["n"], ["v1", "vt"] */
    val posTags: List<String>,
    val glosses: List<String>,
    val examples: List<Example>,
    val info: List<String>      // "See also: X", misc notes
)

data class Example(
    val japanese: String,
    val english: String
)