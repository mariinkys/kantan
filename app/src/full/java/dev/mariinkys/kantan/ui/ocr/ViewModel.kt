package dev.mariinkys.kantan.ui.ocr

import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.text.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import dev.mariinkys.kantan.util.Deinflector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecognizedWord(
    val text: String,
    val entry: DictionaryEntry? = null,
    val isConjugated: Boolean = false
)

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val repository: DictionaryRepository
) : ViewModel() {

    private val _recognizedWords = MutableStateFlow<List<RecognizedWord>>(emptyList())
    val recognizedWords: StateFlow<List<RecognizedWord>> = _recognizedWords

    /** Prevents redundant repository queries when OCR output hasn't changed. */
    private var lastRawKey = ""

    /**
     * Called from the camera analyzer on each frame. Filters ML Kit's [Text] result, then looks up each segment in the dictionary.
     */
    fun processText(mlKitText: Text, scanRect: Rect) {
        if (scanRect == Rect.Zero) return

        val segments = mutableListOf<String>()

        for (block in mlKitText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val bb = element.boundingBox ?: continue
                    val elementBounds = Rect(
                        left = bb.left.toFloat(),
                        top = bb.top.toFloat(),
                        right = bb.right.toFloat(),
                        bottom = bb.bottom.toFloat()
                    )
                    if (!scanRect.overlaps(elementBounds)) continue
                    val t = element.text.trim()
                    if (t.isNotBlank()) segments.add(t)
                }
            }
        }

        val rawKey = segments.joinToString("|")
        if (rawKey == lastRawKey) return
        lastRawKey = rawKey

        viewModelScope.launch {
            val result = segments.distinct().map { segment ->
                val direct = repository.search(segment).firstOrNull() ?: emptyList()
                if (direct.isNotEmpty()) {
                    return@map RecognizedWord(text = segment, entry = direct.first())
                }
                val entry = Deinflector.deinflect(segment)
                    .firstNotNullOfOrNull { candidate ->
                        repository.search(candidate.baseForm).firstOrNull()?.firstOrNull()
                    }
                RecognizedWord(text = segment, entry = entry, isConjugated = entry != null)
            }
            _recognizedWords.value = result
        }
    }

    fun clearWords() {
        lastRawKey = ""
        _recognizedWords.value = emptyList()
    }
}