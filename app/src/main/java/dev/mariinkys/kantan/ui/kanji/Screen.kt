package dev.mariinkys.kantan.ui.kanji

import android.graphics.PathMeasure
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mariinkys.kantan.domain.model.KanjiEntry
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanjiDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier,
    viewModel: KanjiDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val title = (state as? KanjiDetailState.Success)?.kanji?.character ?: "Kanji"
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val s = state) {
            is KanjiDetailState.Loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding), Alignment.Center
            ) { CircularProgressIndicator() }

            is KanjiDetailState.Error -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding), Alignment.Center
            ) { Text(s.message, color = MaterialTheme.colorScheme.error) }

            is KanjiDetailState.Success -> KanjiDetailContent(
                kanji = s.kanji,
                strokes = s.strokes,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun KanjiDetailContent(
    kanji: KanjiEntry,
    strokes: List<android.graphics.Path>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { StrokeAnimationCard(strokes = strokes, character = kanji.character) }
        item {
            InfoCard("Readings") {
                if (kanji.onyomi.isNotEmpty()) ReadingRow("音読み (On)", kanji.onyomi)
                if (kanji.kunyomi.isNotEmpty()) ReadingRow("訓読み (Kun)", kanji.kunyomi)
            }
        }
        item {
            InfoCard("Meanings") {
                kanji.meanings.forEachIndexed { i, m ->
                    SelectionContainer {
                        Text(
                            "${i + 1}. $m", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
        item {
            InfoCard("Details") {
                StatRow("Stroke count", kanji.strokeCount?.toString() ?: "—")
                StatRow("JLPT Level", kanji.jlptLevel?.let { "N$it" } ?: "—")
                StatRow("School Grade", kanji.grade?.toString() ?: "—")
                StatRow("Frequency", kanji.frequency?.let { "#$it" } ?: "—")
            }
        }
    }
}

private const val KANJIVG_VIEWBOX = 109f   // KanjiVG paths are in 0..109 coordinate space

@Composable
private fun StrokeAnimationCard(
    strokes: List<android.graphics.Path>,
    character: String
) {
    var isPlaying by remember { mutableStateOf(false) }
    var completedStrokes by remember { mutableIntStateOf(0) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var hasPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(strokes) {
        if (strokes.isNotEmpty()) {
            delay(400); isPlaying = true
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        hasPlayed = true
        for (i in completedStrokes until strokes.size) {
            val steps = 50
            for (step in 0..steps) {
                currentProgress = step / steps.toFloat()
                delay(8L)
            }
            completedStrokes = i + 1
            currentProgress = 0f
            delay(60L)
        }
        isPlaying = false
    }

    val strokeColor = MaterialTheme.colorScheme.onSurface
    val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val progressColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (strokes.isEmpty()) "Stroke order"
                else "Stroke order (${strokes.size} strokes)",
                style = MaterialTheme.typography.titleMedium
            )

            if (strokes.isEmpty()) {
                // if noo SVG, plain text fallback
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(bgColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        character, style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .size(220.dp)
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    // scale 109×109 kanji to canvas size
                    val s = size.minDimension / KANJIVG_VIEWBOX
                    scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                        drawStrokes(
                            strokes = strokes,
                            completedStrokes = completedStrokes,
                            currentProgress = currentProgress,
                            strokeColor = strokeColor,
                            guideColor = guideColor,
                            progressColor = progressColor,
                            strokeWidth = 3.5f / s
                        )
                    }
                }
            }

            if (isPlaying) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        "Stroke ${completedStrokes + 1} of ${strokes.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                FilledTonalButton(onClick = {
                    completedStrokes = 0; currentProgress = 0f; isPlaying = true
                }) {
                    Icon(
                        if (hasPlayed) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        null, Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (hasPlayed) "Replay" else "Play")
                }
            }
        }
    }
}

private fun DrawScope.drawStrokes(
    strokes: List<android.graphics.Path>,
    completedStrokes: Int,
    currentProgress: Float,
    strokeColor: Color,
    guideColor: Color,
    progressColor: Color,
    strokeWidth: Float
) {
    val style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    strokes.forEachIndexed { i, path ->
        when {
            i < completedStrokes -> {
                // Fully drawn stroke
                drawPath(path.asComposePath(), strokeColor, style = style)
            }

            i == completedStrokes -> {
                // Guide for current stroke
                drawPath(
                    path.asComposePath(), guideColor,
                    style = Stroke(width = strokeWidth * 0.5f, cap = StrokeCap.Round)
                )

                // Animate partial segment
                val measure = PathMeasure(path, false)
                val len = measure.length
                if (len > 0f) {
                    val dst = android.graphics.Path()
                    measure.getSegment(0f, len * currentProgress, dst, true)
                    drawPath(dst.asComposePath(), progressColor, style = style)

                    // Dot at the stroke tip
                    val pos = FloatArray(2)
                    measure.getPosTan(len * currentProgress, pos, null)
                    drawCircle(
                        color = progressColor,
                        radius = strokeWidth * 1.2f,
                        center = androidx.compose.ui.geometry.Offset(pos[0], pos[1])
                    )
                }
            }

            else -> {
                // Upcoming strokes as faint guides
                drawPath(
                    path.asComposePath(), guideColor,
                    style = Stroke(width = strokeWidth * 0.5f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            content()
        }
    }
}

@Composable
private fun ReadingRow(label: String, readings: List<String>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        SelectionContainer {
            Text(readings.joinToString("、"), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}