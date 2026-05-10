package dev.mariinkys.kantan.ui.search.handwriting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface ModelState {
    data object Checking : ModelState
    data object Downloading : ModelState
    data object Ready : ModelState
    data class Failed(val message: String) : ModelState
}

data class DrawnStroke(val points: List<PointWithTime>)
data class PointWithTime(val offset: Offset, val timestamp: Long)

class HandwritingViewModel : ViewModel() {

    var modelState by mutableStateOf<ModelState>(ModelState.Checking)
        private set

    var strokes by mutableStateOf<List<DrawnStroke>>(emptyList())
    var isRecognizing by mutableStateOf(false)
    var candidates by mutableStateOf<List<String>>(emptyList())
    var errorMessage by mutableStateOf<String?>(null)

    private val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja")
    private val model = modelIdentifier?.let { DigitalInkRecognitionModel.builder(it).build() }
    private val recognizer = model?.let {
        DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(it).build())
    }

    init {
        downloadModel()
    }

    private fun downloadModel() {
        val m = model ?: run {
            modelState = ModelState.Failed("Japanese model identifier not found.")
            return
        }

        viewModelScope.launch {
            try {
                val manager = RemoteModelManager.getInstance()

                // if model is not cached we download (see: https://developers.google.com/ml-kit/vision/digital-ink-recognition/android) I think
                val isDownloaded = manager.isModelDownloaded(m).await()
                if (!isDownloaded) {
                    modelState = ModelState.Downloading
                    manager.download(m, DownloadConditions.Builder().build()).await()
                }
                
                modelState = ModelState.Ready
            } catch (e: Exception) {
                modelState = ModelState.Failed("Download failed: ${e.message}")
            }
        }
    }

    fun recognize() {
        val rec = recognizer ?: return
        if (strokes.isEmpty()) return

        isRecognizing = true
        errorMessage = null

        // DrawnStrokes into ML Kit Ink object
        val ink = Ink.builder().apply {
            strokes.forEach { stroke ->
                val sb = Ink.Stroke.builder()
                stroke.points.forEach { pt ->
                    sb.addPoint(Ink.Point.create(pt.offset.x, pt.offset.y, pt.timestamp))
                }
                addStroke(sb.build())
            }
        }.build()

        viewModelScope.launch {
            try {
                val result = rec.recognize(ink).await()
                candidates = result.candidates
                    .map { it.text.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(8)

                if (candidates.isEmpty()) {
                    errorMessage = "No match — try drawing more clearly."
                }
            } catch (e: Exception) {
                errorMessage = "Recognition failed: ${e.message}"
            } finally {
                isRecognizing = false
            }
        }
    }

    fun clear() {
        strokes = emptyList()
        candidates = emptyList()
        errorMessage = null
    }

    override fun onCleared() {
        super.onCleared()
        recognizer?.close()
    }
}