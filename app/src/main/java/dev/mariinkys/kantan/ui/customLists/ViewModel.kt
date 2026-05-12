package dev.mariinkys.kantan.ui.customLists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.model.CustomList
import dev.mariinkys.kantan.domain.repository.CustomListsRepository
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CustomListsState {
    data object Loading : CustomListsState
    data class Ready(val lists: List<CustomList>) : CustomListsState
}

@HiltViewModel
class CustomListsViewModel @Inject constructor(
    private val repository: CustomListsRepository,
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CustomListsState>(CustomListsState.Loading)
    val state: StateFlow<CustomListsState> = _state

    private val _events = MutableSharedFlow<CustomListsEvent>()
    val events: SharedFlow<CustomListsEvent> = _events

    init {
        viewModelScope.launch {
            repository.getAllLists().collect { lists ->
                _state.value = CustomListsState.Ready(lists)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = CustomListsState.Loading
            val lists = repository.getAllLists().first()
            _state.value = CustomListsState.Ready(lists)
            _events.emit(CustomListsEvent.ShowSnackbar("Lists Refreshed"))
        }
    }

    fun createList(name: String) = viewModelScope.launch { repository.createList(name) }

    fun renameList(listId: Int, name: String) = viewModelScope.launch {
        repository.renameList(listId, name)
    }

    fun addBulkEntries(listId: Int, terms: String) = viewModelScope.launch {
        try {
            val entries = dictionaryRepository.getBulkEntriesByTerms(terms)

            if (entries.isNotEmpty()) {
                repository.addMultipleEntry(listId, entries.map { it.id })
                _events.emit(CustomListsEvent.ShowSnackbar("Added ${entries.size} words to the list"))
            } else {
                _events.emit(CustomListsEvent.ShowSnackbar("No matching words found"))
            }
        } catch (e: Exception) {
            _events.emit(CustomListsEvent.ShowSnackbar("Error adding words: ${e.localizedMessage}"))
        }
    }

    fun deleteList(id: Int) = viewModelScope.launch { repository.deleteList(id) }
}

sealed interface CustomListsEvent {
    data class ShowSnackbar(val message: String) : CustomListsEvent
}