package dev.mariinkys.kantan.ui.debug

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.mariinkys.kantan.BuildConfig
import dev.mariinkys.kantan.data.local.dao.KanjiDao
import dev.mariinkys.kantan.data.local.dao.TermDao
import dev.mariinkys.kantan.data.worker.DictionaryImportWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

@Composable
fun DebugScreen(
    onMenuClick: () -> Unit,
    termDao: TermDao,
    kanjiDao: KanjiDao,
    modifier: Modifier
) {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }

    // True only while the import job is actively running or enqueued
    val isImporting by remember(workManager) {
        workManager.getWorkInfosForUniqueWorkFlow("DICT_IMPORT")
            .map { infos ->
                infos.any {
                    it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                }
            }
    }.collectAsStateWithLifecycle(initialValue = false)

    AnimatedContent(
        targetState = isImporting,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "debug_import_transition"
    ) { importing ->
        if (importing) {
            ImportLoadingScreen(
                getTermCount = { termDao.count() },
                getKanjiCount = { kanjiDao.count() }
            )
        } else {
            DebugContent(
                onMenuClick = onMenuClick,
                modifier = modifier,
                onRebuildClick = {
                    val request = OneTimeWorkRequestBuilder<DictionaryImportWorker>()
                        .setInputData(
                            androidx.work.workDataOf("force" to true)
                        )
                        .build()
                    workManager.enqueueUniqueWork(
                        "DICT_IMPORT",
                        ExistingWorkPolicy.REPLACE,
                        request
                    )
                }
            )
        }
    }
}

@Composable
private fun DebugContent(
    onMenuClick: () -> Unit,
    onRebuildClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SelectionContainer {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                }
                Text("Debug", style = MaterialTheme.typography.titleLarge)
            }

            HorizontalDivider()

            if (BuildConfig.DEBUG) {
                TextButton(onClick = onRebuildClick) {
                    Text("Rebuild database")
                }
            }
        }
    }
}

@Composable
private fun ImportLoadingScreen(
    getTermCount: suspend () -> Int,
    getKanjiCount: suspend () -> Int
) {
    var termCount by remember { mutableIntStateOf(0) }
    var kanjiCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            termCount = getTermCount()
            kanjiCount = getKanjiCount()
            delay(1_000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(
                text = "簡単",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Building Dictionary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "This will happen only once",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Please keep the app open",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatRow(label = "Terms", count = termCount)

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    StatRow(label = "Kanji", count = kanjiCount)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically { it } + fadeIn() togetherWith
                        slideOutVertically { -it } + fadeOut())
                    .using(SizeTransform(clip = false))
            },
            label = "countAnimation",
            contentAlignment = Alignment.CenterEnd
        ) { targetCount ->
            Text(
                text = if (targetCount > 0) "%,d".format(targetCount) else "Calculating...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
        }
    }
}