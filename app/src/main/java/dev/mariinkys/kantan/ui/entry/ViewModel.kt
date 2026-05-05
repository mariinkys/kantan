package dev.mariinkys.kantan.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.KanjiEntry
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import dev.mariinkys.kantan.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    private val expression: String = checkNotNull(savedStateHandle["expression"])
    private val reading: String = checkNotNull(savedStateHandle["reading"])

    private val _state = MutableStateFlow<EntryDetailState>(EntryDetailState.Loading)
    val state: StateFlow<EntryDetailState> = _state

    val isFavorite: StateFlow<Boolean> = favoritesRepository
        .isFavorite(expression, reading)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            val entry = repository.getEntry(expression, reading)
            if (entry == null) {
                _state.value = EntryDetailState.Error("Entry not found")
                return@launch
            }
            val kanji = repository.getKanjiForWord(entry.expression)
            _state.value = EntryDetailState.Success(entry = entry, kanji = kanji)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            favoritesRepository.toggle(expression, reading)
        }
    }
}