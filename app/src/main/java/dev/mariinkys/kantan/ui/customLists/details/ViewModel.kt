package dev.mariinkys.kantan.ui.customLists.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.repository.CustomListsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CustomListDetailsState {
    data object Loading : CustomListDetailsState
    data class Ready(val entries: List<DictionaryEntry>?) : CustomListDetailsState
}

@HiltViewModel
class ListEntriesViewModel @Inject constructor(
    private val repository: CustomListsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listId: Int = checkNotNull(savedStateHandle["listId"])

    private val _state = MutableStateFlow<CustomListDetailsState>(CustomListDetailsState.Loading)
    val state: StateFlow<CustomListDetailsState> = _state

    private val _events = MutableSharedFlow<CustomListEntriesEvent>()
    val events: SharedFlow<CustomListEntriesEvent> = _events

    init {
        viewModelScope.launch {
            repository.getListEntries(listId).collect { entries ->
                _state.value = CustomListDetailsState.Ready(entries)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = CustomListDetailsState.Loading
            val entries = repository.getListEntries(listId).first()
            _state.value = CustomListDetailsState.Ready(entries)
            _events.emit(CustomListEntriesEvent.ShowSnackbar("List Updated"))
        }
    }

    fun removeEntry(sequenceId: Int) = viewModelScope.launch {
        repository.removeEntry(listId, sequenceId)
    }
}

sealed interface CustomListEntriesEvent {
    data class ShowSnackbar(val message: String) : CustomListEntriesEvent
}