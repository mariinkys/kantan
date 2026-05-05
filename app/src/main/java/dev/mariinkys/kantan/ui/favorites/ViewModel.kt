package dev.mariinkys.kantan.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import dev.mariinkys.kantan.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FavoritesState {
    data object Loading : FavoritesState
    data class Ready(val entries: List<DictionaryEntry>) : FavoritesState
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FavoritesState>(FavoritesState.Loading)
    val state: StateFlow<FavoritesState> = _state

    private val _events = MutableSharedFlow<FavoritesEvent>()
    val events: SharedFlow<FavoritesEvent> = _events

    init {
        // whenever the favorites set changes (add/remove), reload with definitions
        viewModelScope.launch {
            favoritesRepository.getAll()
                .collect { stubs -> loadWithDefinitions(stubs) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = FavoritesState.Loading
            val stubs = favoritesRepository.getAll().first()
            loadWithDefinitions(stubs)
            _events.emit(FavoritesEvent.ShowSnackbar("Favorites Refreshed"))
        }
    }

    fun remove(expression: String, reading: String) {
        viewModelScope.launch {
            favoritesRepository.remove(expression, reading)
        }
    }

    /**
     * For each saved (expression, reading) pair, fetch the full entry from the
     * dictionary so definitions are always shown. Falls back to the stub (no
     * definitions) if the word isn't found — handles the edge case where a
     * dictionary update removed an entry the user had saved.
     */
    private suspend fun loadWithDefinitions(stubs: List<DictionaryEntry>) {
        val full = stubs.map { stub ->
            dictionaryRepository.getEntry(stub.expression, stub.reading) ?: stub
        }
        _state.value = FavoritesState.Ready(full)
    }
}

sealed interface FavoritesEvent {
    data class ShowSnackbar(val message: String) : FavoritesEvent
}