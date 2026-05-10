package dev.mariinkys.kantan.ui.ocr

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.domain.repository.DictionaryRepository
import javax.inject.Inject

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val repository: DictionaryRepository
) : ViewModel() {

}