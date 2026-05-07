package dev.mariinkys.kantan.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.KanjiEntry
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import dev.mariinkys.kantan.domain.repository.FavoritesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EntryDetailState {
    data object Loading : EntryDetailState
    data class Error(val message: String) : EntryDetailState
    data class Success(val entry: DictionaryEntry, val kanji: List<KanjiEntry>) : EntryDetailState
}

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DictionaryRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {
    private val _state = MutableStateFlow<EntryDetailState>(EntryDetailState.Loading)
    val state: StateFlow<EntryDetailState> = _state

    private var resolvedSequence: Int? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val isFavorite: StateFlow<Boolean> = _state
        .map { (it as? EntryDetailState.Success)?.entry?.id }
        .filterNotNull()
        .flatMapLatest { favoritesRepository.isFavorite(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        val sequence = savedStateHandle.get<Int>("sequence")
        val term = savedStateHandle.get<String>("term")

        viewModelScope.launch {
            val entry = when {
                sequence != null -> repository.getEntry(sequence)
                term != null -> repository.getEntryByTerm(term)
                else -> null
            }

            if (entry == null) {
                _state.value = EntryDetailState.Error("Entry not found")
                return@launch
            }

            resolvedSequence = entry.id
            val kanji = repository.getKanjiForWord(entry.expression)
            _state.value = EntryDetailState.Success(entry = entry, kanji = kanji)
        }
    }

    fun toggleFavorite() {
        val seq = resolvedSequence ?: return
        viewModelScope.launch {
            favoritesRepository.toggle(seq)
        }
    }
}