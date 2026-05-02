package dev.mariinkys.kantan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import dev.mariinkys.kantan.data.local.dao.KanjiDao
import dev.mariinkys.kantan.data.local.dao.TermDao
import dev.mariinkys.kantan.ui.search.SearchScreen
import dev.mariinkys.kantan.ui.theme.KantanTheme
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var termDao: TermDao

    @Inject
    lateinit var kanjiDao: KanjiDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KantanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SearchScreen(
                        onEntryClick = { /* TODO: navigate to detail */ },
                        modifier = Modifier.padding(innerPadding)
                    )
                    //ImportDebugScreen(
                    //    modifier = Modifier.padding(innerPadding),
                    //    getTermCount = { termDao.count() },
                    //    getKanjiCount = { kanjiDao.count() }
                    //)
                }
            }
        }
    }
}

@Composable
fun ImportDebugScreen(
    modifier: Modifier = Modifier,
    getTermCount: suspend () -> Int,
    getKanjiCount: suspend () -> Int
) {
    var termCount by remember { mutableIntStateOf(-1) }
    var kanjiCount by remember { mutableIntStateOf(-1) }

    // Poll every 2 seconds so we can watch the import progress live
    LaunchedEffect(Unit) {
        while (true) {
            termCount = getTermCount()
            kanjiCount = getKanjiCount()
            delay(2_000)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Kantan — Import Status", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        if (termCount == -1) {
            CircularProgressIndicator()
        } else {
            StatusRow(label = "Terms (JMdict)", count = termCount)
            Spacer(Modifier.height(12.dp))
            StatusRow(label = "Kanji (KANJIDIC)", count = kanjiCount)
            Spacer(Modifier.height(24.dp))
            if (termCount > 0 && kanjiCount > 0) {
                Text(
                    "✅ Import complete!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    "⏳ Import in progress…",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, count: Int) {
    Card(modifier = Modifier.fillMaxWidth(0.7f)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (count >= 0) "%,d".format(count) else "…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}