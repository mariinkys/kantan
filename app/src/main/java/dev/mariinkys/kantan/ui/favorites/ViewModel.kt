package dev.mariinkys.kantan.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.model.DictionaryEntry
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
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FavoritesState>(FavoritesState.Loading)
    val state: StateFlow<FavoritesState> = _state

    private val _events = MutableSharedFlow<FavoritesEvent>()
    val events: SharedFlow<FavoritesEvent> = _events

    init {
        viewModelScope.launch {
            favoritesRepository.getAll()
                .collect { entries ->
                    _state.value = FavoritesState.Ready(entries)
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = FavoritesState.Loading
            val entries = favoritesRepository.getAll().first()
            _state.value = FavoritesState.Ready(entries)
            _events.emit(FavoritesEvent.ShowSnackbar("Favorites Refreshed"))
        }
    }

    fun remove(sequence: Int) {
        viewModelScope.launch {
            favoritesRepository.remove(sequence)
        }
    }
}

sealed interface FavoritesEvent {
    data class ShowSnackbar(val message: String) : FavoritesEvent
}