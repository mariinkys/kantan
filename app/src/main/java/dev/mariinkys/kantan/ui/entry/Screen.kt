package dev.mariinkys.kantan.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.KanjiEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    onBack: () -> Unit,
    onKanjiClick: (character: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (state is EntryDetailState.Success) {
                        val s = state as EntryDetailState.Success
                        Column {
                            Text(s.entry.expression, style = MaterialTheme.typography.titleLarge)
                            if (s.entry.reading != s.entry.expression) {
                                Text(
                                    s.entry.reading,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
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
            is EntryDetailState.Loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is EntryDetailState.Error -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text(s.message) }

            is EntryDetailState.Success -> EntryDetailContent(
                entry = s.entry,
                kanji = s.kanji,
                onKanjiClick = onKanjiClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun EntryDetailContent(
    entry: DictionaryEntry,
    kanji: List<KanjiEntry>,
    onKanjiClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = buildList {
        add("Definitions")
        if (kanji.isNotEmpty()) add("Kanji")
    }
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                    text = { Text(title) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (tabs[page]) {
                "Definitions" -> DefinitionsTab(entry)
                "Kanji" -> KanjiTab(kanji, onKanjiClick)
            }
        }
    }
}

@Composable
private fun DefinitionsTab(entry: DictionaryEntry) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // TODO: FIX Tags row (rules + term tags)
        val tagChips = buildList {
            if (entry.rules.isNotBlank()) addAll(entry.rules.split(" "))
            if (entry.tags.isNotBlank()) addAll(entry.tags.split(" "))
        }.filter { it.isNotBlank() }.distinct()

        if (tagChips.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tagChips.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
        }

        // Numbered definitions
        itemsIndexed(entry.definitions) { index, def ->
            val cleaned = def.trimStart('\n').trim()
            if (cleaned.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        text = cleaned,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (index < entry.definitions.lastIndex) HorizontalDivider(
                    modifier = Modifier.padding(
                        top = 8.dp
                    )
                )
            }
        }

        // TODO: Example sentences
    }
}

@Composable
private fun KanjiTab(kanji: List<KanjiEntry>, onKanjiClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(kanji.size) { i ->
            KanjiCard(kanji = kanji[i], onClick = { onKanjiClick(kanji[i].character) })
        }
    }
}

@Composable
private fun KanjiCard(kanji: KanjiEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large kanji character
            Text(
                text = kanji.character,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Meanings
                Text(
                    text = kanji.meanings.take(3).joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // On/kun readings
                if (kanji.onyomi.isNotEmpty()) {
                    Text(
                        text = "音: " + kanji.onyomi.take(4).joinToString("、"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (kanji.kunyomi.isNotEmpty()) {
                    Text(
                        text = "訓: " + kanji.kunyomi.take(4).joinToString("、"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Quick stats
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    kanji.strokeCount?.let { StatChip("$it strokes") }
                    kanji.jlptLevel?.let { StatChip("JLPT N$it") }
                    kanji.grade?.let { StatChip("Grade $it") }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}