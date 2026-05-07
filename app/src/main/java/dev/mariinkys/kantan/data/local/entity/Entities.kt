package dev.mariinkys.kantan.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import dev.mariinkys.kantan.data.local.Converters

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
    val sequence: Int,
    val termTags: String,
    // Structured senses as JSON List<StoredSense>
    @ColumnInfo(name = "senses_json") val sensesJson: String = "[]",
    // Flat gloss text for FTS only we don't show this in the UI
    @ColumnInfo(name = "definitions_text") val definitionsText: String = ""
)

// FTS virtual table — indexes definitionsText for English keyword search
@Entity(tableName = "terms_fts")
@Fts4
data class TermFtsEntity(
    @ColumnInfo(name = "definitions_text") val definitionsText: String
)

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