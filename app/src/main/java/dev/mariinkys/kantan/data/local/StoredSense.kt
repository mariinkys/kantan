package dev.mariinkys.kantan.data.local

import kotlinx.serialization.Serializable

/**
 * Stored in Room as JSON (sensesJson column on TermEntity).
 * One StoredSense per original term_bank row.
 */
@Serializable
data class StoredSense(
    val posTags: List<String>,           // raw tags: ["n"], ["v1","vt"], ["forms"]…
    val glosses: List<String>,           // English meanings from glossary ul
    val examples: List<StoredExample>,   // from examples ul
    val info: List<String> = emptyList() // "See also: X", misc text nodes
)

@Serializable
data class StoredExample(
    val japanese: String,
    val english: String
)