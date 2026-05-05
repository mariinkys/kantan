package dev.mariinkys.kantan.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import dev.mariinkys.kantan.data.local.Converters

// JMdict term

@Entity(
    tableName = "terms",
    indices = [Index("expression"), Index("reading")]
)
@TypeConverters(Converters::class)
data class TermEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val reading: String,
    val definitionTags: String,
    val rules: String,
    val score: Int,
    val definitions: List<String>,
    val sequence: Int,
    val termTags: String,
    val examplesJson: String = "[]",
    // Flat text used by the FTS table — all definition strings joined with spaces.
    @ColumnInfo(name = "definitions_text") val definitionsText: String = ""
)

// FTS4 virtual table
//
// Intentionally NOT using contentEntity, that links via rowid which breaks when
// insertAll() silently ignores duplicate rows (rowids then diverge).
// Instead, we store `expression` and join back to `terms` on that column.

@Entity(tableName = "terms_fts")
@Fts4
data class TermFtsEntity(
    val termId: Long,
    val expression: String,
    @ColumnInfo(name = "definitions_text") val definitionsText: String
)

// KANJIDIC kanji

@Entity(
    tableName = "kanji",
    indices = [Index("character", unique = true)]
)
@TypeConverters(Converters::class)
data class KanjiEntity(
    @PrimaryKey val character: String,
    val onyomi: String,
    val kunyomi: String,
    val tags: String,
    val meanings: List<String>,
    val grade: Int?,
    val strokeCount: Int?,
    val jlptLevel: Int?,
    val frequency: Int?
)

// Entities in the favorite list

@Entity(
    tableName = "favorites",
    primaryKeys = ["expression", "reading"]
)
data class FavoriteEntity(
    val expression: String,
    val reading: String,
    val savedAt: Long = System.currentTimeMillis()
)
