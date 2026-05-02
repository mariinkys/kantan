package dev.mariinkys.kantan.domain.model

/**
 * A single kanji entry from KANJIDIC.
 */
data class KanjiEntry(
    val character: String,
    val onyomi: List<String>,   // split from space-separated string
    val kunyomi: List<String>,
    val meanings: List<String>,
    val strokeCount: Int?,
    val grade: Int?,
    val jlptLevel: Int?,
    val frequency: Int?
)