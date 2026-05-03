package dev.mariinkys.kantan.ui.search.handwriting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingBottomSheet(
    onDismiss: () -> Unit,
    onCharacterSelected: (String) -> Unit,
    viewModel: HandwritingViewModel = viewModel()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        HandwritingSheetContent(
            viewModel = viewModel,
            onCharacterSelected = { char ->
                onCharacterSelected(char)
            }
        )
    }
}

@Composable
private fun HandwritingSheetContent(
    viewModel: HandwritingViewModel,
    onCharacterSelected: (String) -> Unit
) {
    var currentPoints by remember { mutableStateOf<List<PointWithTime>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val strokeColor = MaterialTheme.colorScheme.onSurface

        Text(
            "Draw a character",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // Status Header
        when (val s = viewModel.modelState) {
            is ModelState.Downloading -> LoadingIndicator()
            is ModelState.Failed -> Text(
                s.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )

            is ModelState.Ready -> Unit
        }

        // Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(0.1f),
                    RoundedCornerShape(16.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            currentPoints = listOf(PointWithTime(it, System.currentTimeMillis()))
                            viewModel.candidates = emptyList()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints = currentPoints + PointWithTime(
                                change.position,
                                System.currentTimeMillis()
                            )
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                viewModel.strokes += DrawnStroke(currentPoints)
                                currentPoints = emptyList()
                            }
                        }
                    )
                }
        ) {
            val style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)

            viewModel.strokes.forEach { stroke ->
                drawSmoothPath(stroke.points.map { it.offset }, strokeColor, style)
            }
            if (currentPoints.size > 1) {
                drawSmoothPath(currentPoints.map { it.offset }, strokeColor, style)
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = { viewModel.clear() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear")
            }

            Button(
                onClick = { viewModel.recognize() },
                modifier = Modifier.weight(1f),
                enabled = viewModel.strokes.isNotEmpty() && viewModel.modelState is ModelState.Ready && !viewModel.isRecognizing
            ) {
                if (viewModel.isRecognizing) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Recognize")
                }
            }
        }

        // Candidates Chips
        AnimatedVisibility(viewModel.candidates.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            CandidateRow(candidates = viewModel.candidates, onSelected = { char ->
                onCharacterSelected(char)
                viewModel.clear()
            })
        }

        viewModel.errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = "Downloading model…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CandidateRow(candidates: List<String>, onSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Suggestions:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            candidates.forEach { text ->
                InputChip(
                    selected = false,
                    onClick = { onSelected(text) },
                    label = {
                        Text(
                            text,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothPath(
    points: List<Offset>,
    color: Color,
    style: Stroke
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.lastIndex) {
            val mid =
                Offset((points[i].x + points[i + 1].x) / 2f, (points[i].y + points[i + 1].y) / 2f)
            quadraticTo(points[i].x, points[i].y, mid.x, mid.y)
        }
        lineTo(points.last().x, points.last().y)
    }
    drawPath(path, color, style = style)
}