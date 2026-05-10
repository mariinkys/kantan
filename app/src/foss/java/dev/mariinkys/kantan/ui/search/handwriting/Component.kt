package dev.mariinkys.kantan.ui.search.handwriting

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingBottomSheet(
    onDismiss: () -> Unit,
    onCharacterSelected: (String) -> Unit,
    viewModel: HandwritingViewModel = viewModel()
) {
    onDismiss()
}

class HandwritingViewModel : ViewModel() {
    
}