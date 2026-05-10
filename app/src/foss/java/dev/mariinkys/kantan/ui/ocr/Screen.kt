package dev.mariinkys.kantan.ui.ocr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun OcrScreen(
    onBack: () -> Unit,
    onEntryClick: (sequence: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OcrViewModel = hiltViewModel()
) {
    onBack()
}