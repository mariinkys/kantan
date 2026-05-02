package dev.mariinkys.kantan.ui.kanji

import android.app.Application
import android.graphics.Path
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.data.svg.KanjiVGParser
import dev.mariinkys.kantan.domain.model.KanjiEntry
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface KanjiDetailState {
    data object Loading : KanjiDetailState
    data class Error(val message: String) : KanjiDetailState
    data class Success(
        val kanji: KanjiEntry,
        val strokes: List<Path>
    ) : KanjiDetailState
}

@HiltViewModel
class KanjiDetailViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val repository: DictionaryRepository
) : AndroidViewModel(application) {

    // Character is URL-encoded in the nav arg because slash/special chars break routes
    private val character: String = checkNotNull(savedStateHandle["character"])

    private val _state = MutableStateFlow<KanjiDetailState>(KanjiDetailState.Loading)
    val state: StateFlow<KanjiDetailState> = _state

    init {
        viewModelScope.launch {
            val kanji = repository.getKanji(character)
            if (kanji == null) {
                _state.value = KanjiDetailState.Error("No data found for '$character'")
                return@launch
            }
            val strokes = withContext(Dispatchers.IO) {
                KanjiVGParser.parseStrokes(application, character)
            }
            _state.value = KanjiDetailState.Success(kanji = kanji, strokes = strokes)
        }
    }
}