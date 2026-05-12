package dev.mariinkys.kantan.ui.customLists.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mariinkys.kantan.domain.model.StudyState

@Composable
fun StudySessionScreen(
    listName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudySessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Text(listName, style = MaterialTheme.typography.titleLarge)
            }
            IconButton(
                onClick = viewModel::resetSession,
                enabled = state !is StudySessionViewModel.State.Loading
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Restart session",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider()

        when (val s = state) {
            is StudySessionViewModel.State.Loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

            is StudySessionViewModel.State.Finished ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🎉", style = MaterialTheme.typography.displaySmall)
                        Text("Session complete!", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = viewModel::resetSession) { Text("Study again") }
                        OutlinedButton(onClick = onBack) { Text("Back to list") }
                    }
                }

            is StudySessionViewModel.State.Studying -> {
                // Progress
                LinearProgressIndicator(
                    progress = { if (s.total == 0) 0f else 1f - (s.remaining.toFloat() / s.total.toFloat()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    "${s.total - s.remaining} / ${s.total}",
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 16.dp),
                    style = MaterialTheme.typography.labelSmall
                )

                // Flashcard
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .clickable { viewModel.swapReveal() }
                ) {
                    Box(Modifier.fillMaxSize()) {
                        StudyStateChip(
                            studyState = s.studyState,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )

                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(s.card.expression, style = MaterialTheme.typography.displayMedium)
                            if (s.revealed) {
                                Text(
                                    s.card.reading, style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(
                                    Modifier.padding(
                                        horizontal = 32.dp,
                                        vertical = 8.dp
                                    )
                                )
                                Text(
                                    s.card.shortDefinition,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            } else {
                                Text(
                                    "Tap to reveal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Rating buttons
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.rate(StudyState.BAD) }) {
                        Text("Again", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.rate(StudyState.GOOD) }) { Text("Good") }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.rate(StudyState.MASTERED) }) { Text("Mastered") }
                }

            }
        }
    }
}

@Composable
private fun StudyStateChip(studyState: StudyState, modifier: Modifier) {
    val (label, containerColor, contentColor) = when (studyState) {
        StudyState.NEW -> Triple(
            "New",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        StudyState.BAD -> Triple(
            "Again",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )

        StudyState.GOOD -> Triple(
            "Good",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )

        StudyState.MASTERED -> Triple(
            "Mastered",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}