package dev.mariinkys.kantan.ui.customLists.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.StudyState
import dev.mariinkys.kantan.domain.repository.CustomListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CustomListsRepository
) : ViewModel() {

    private val listId: Int = checkNotNull(savedStateHandle["listId"])

    sealed interface State {
        data object Loading : State
        data object Finished : State
        data class Studying(
            val card: DictionaryEntry,
            val studyState: StudyState,
            val revealed: Boolean,
            val remaining: Int,
            val total: Int
        ) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    // Queue of (entry, currentStudyState) pairs to review
    private var queue: ArrayDeque<Pair<DictionaryEntry, StudyState>> = ArrayDeque()
    private var totalCount = 0

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            combine(
                repo.getList(listId),
                repo.getListEntries(listId)
            ) { list, entries -> list to entries }
                .first { (list, entries) -> list != null && entries != null }
                .let { (list, entries) ->
                    val stateMap = list!!.entries.associate { it.sequenceId to it.studyState }
                    queue = entries!!
                        .sortedBy { entry ->
                            when (stateMap[entry.id] ?: StudyState.NEW) {
                                StudyState.NEW -> 0
                                StudyState.BAD -> 1
                                StudyState.GOOD -> 2
                                StudyState.MASTERED -> 3
                            }
                        }
                        .groupBy { entry -> stateMap[entry.id] ?: StudyState.NEW }
                        .flatMap { (_, group) -> group.shuffled() }  // shuffle within each priority group
                        .map { it to (stateMap[it.id] ?: StudyState.NEW) }
                        .let { ArrayDeque(it) }
                    totalCount = queue.size
                    advance()
                }
        }
    }

    fun resetSession() {
        _state.value = State.Loading
        loadSession()
    }

    fun swapReveal() {
        val current = _state.value as? State.Studying ?: return
        _state.value = current.copy(revealed = !current.revealed)
    }

    fun rate(newState: StudyState) {
        val current = _state.value as? State.Studying ?: return
        viewModelScope.launch {
            repo.updateStudyState(listId, current.card.id, newState)
            // Re-queue cards for another pass in this session if not mastered
            if (newState != StudyState.MASTERED) {
                queue.addLast(current.card to newState)
            }
            advance()
        }
    }

    private fun advance() {
        if (queue.isEmpty()) {
            _state.value = State.Finished
            return
        }
        val (entry, studyState) = queue.removeFirst()
        _state.value = State.Studying(
            card = entry,
            studyState = studyState,
            revealed = false,
            remaining = queue.size,
            total = totalCount
        )
    }
}