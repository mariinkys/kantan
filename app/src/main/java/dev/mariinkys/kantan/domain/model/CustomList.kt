package dev.mariinkys.kantan.domain.model

@kotlinx.serialization.Serializable
data class CustomList(
    val id: Int,
    val name: String,
    val entries: List<ListEntry> = emptyList(),
) {
    val entryCount: String
        get() = entries.size.toString()
}

@kotlinx.serialization.Serializable
data class ListEntry(
    val sequenceId: Int,
    val studyState: StudyState = StudyState.NEW,
    val lastReviewed: Long? = null
)

@kotlinx.serialization.Serializable
enum class StudyState {
    NEW, GOOD, BAD, MASTERED
}