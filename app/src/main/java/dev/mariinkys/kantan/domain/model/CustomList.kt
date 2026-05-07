package dev.mariinkys.kantan.domain.model

@kotlinx.serialization.Serializable
data class CustomList(
    val id: Int,
    val name: String,
    val sequences: List<Int>,
) {
    val entryCount: String
        get() = sequences
            .count().toString()
}