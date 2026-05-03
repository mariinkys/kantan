package dev.mariinkys.kantan.domain.model

/**
 * A single dictionary entry as shown in search results and the detail screen.
 * Decoupled from the Room entity so the UI never imports data-layer classes.
 */
data class DictionaryEntry(
    val expression: String,   // kanji/word form  e.g. "食べる"
    val reading: String,      // kana reading     e.g. "たべる"
    val definitions: List<String>,
    val rules: String,        // inflection codes e.g. "v1" (ichidan verb)
    val definitionTags: String, // "1 adj-na n"
    val tags: String,         // term tags        e.g. "news ichi"
    val examples: List<ExampleSentence> = emptyList()
) {
    val longDefinition: String
        get() {
            val cleanDefinitions = definitions
                .flatMap { def ->
                    def.split(",")
                }
                .map { it.trim() }
                .filter { it.isNotBlank() }

            return when {
                cleanDefinitions.isEmpty() -> ""
                cleanDefinitions.size <= 3 -> cleanDefinitions.joinToString(", ")
                else -> cleanDefinitions.take(3).joinToString(", ") + "..."
            }
        }
}