package dev.mariinkys.kantan.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Results(val entries: List<DictionaryEntry>) : SearchState
    data class NoResults(val query: String) : SearchState
}

sealed interface RandomEntryDetailState {
    data object Loading : RandomEntryDetailState
    data class Error(val message: String) : RandomEntryDetailState
    data class Success(val entry: DictionaryEntry) :
        RandomEntryDetailState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: DictionaryRepository,
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    var searchState: SearchState by mutableStateOf(SearchState.Idle)
        private set

    private var searchJob: Job? = null

    private val _randomEntryState =
        MutableStateFlow<RandomEntryDetailState>(RandomEntryDetailState.Loading)
    val randomEntryState: StateFlow<RandomEntryDetailState> = _randomEntryState

    init {
        viewModelScope.launch {
            val entry = repository.getRandomEntry()
            if (entry != null) {
                _randomEntryState.value = RandomEntryDetailState.Success(entry)
            } else {
                _randomEntryState.value = RandomEntryDetailState.Error("No featured word today")
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        query = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            searchState = SearchState.Idle
            return
        }

        searchState = SearchState.Loading
        searchJob = viewModelScope.launch {
            delay(300L)  // debounce
            repository.search(newQuery).collect { results ->
                searchState = if (results.isEmpty()) {
                    SearchState.NoResults(newQuery)
                } else {
                    SearchState.Results(results)
                }
            }
        }
    }

    fun clearQuery() = onQueryChange("")
}