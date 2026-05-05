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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.ExampleSentence
import dev.mariinkys.kantan.domain.model.KanjiEntry
import dev.mariinkys.kantan.util.resolveTag
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    onBack: () -> Unit,
    onKanjiClick: (character: String) -> Unit,
    modifier: Modifier,
    viewModel: EntryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                },
                actions = {
                    // We only show the favorite button once the entry has loaded
                    if (state is EntryDetailState.Success) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite)
                                    Icons.Filled.Favorite
                                else
                                    Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite)
                                    "Remove from favorites"
                                else
                                    "Add to favorites",
                                tint = if (isFavorite)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

        val tagCodes = buildList {
            if (entry.definitionTags.isNotBlank()) addAll(entry.definitionTags.split(" "))
            if (entry.tags.isNotBlank()) addAll(entry.tags.split(" "))
            if (entry.rules.isNotBlank()) addAll(entry.rules.split(" "))
        }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { it.toIntOrNull() == null }

        if (tagCodes.isNotEmpty()) {
            item {
                TagRow(tagCodes)
            }
        }

        // Numbered definitions
        val cleanDefinitions = entry.definitions
            .map { it.trim() }
            .filter { it.isNotBlank() }
        itemsIndexed(cleanDefinitions) { index, def ->
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
                    text = def,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (index < cleanDefinitions.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (entry.examples.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Examples",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            itemsIndexed(entry.examples) { _, example ->
                ExampleCard(example)
            }
        }
    }
}

@Composable
private fun TagRow(tagCodes: List<String>) {
    val posCategories = setOf(
        "adj-i", "adj-ix", "adj-na", "adj-no", "adj-pn", "adj-f", "adj-t",
        "adv", "adv-to", "aux", "aux-adj", "aux-v", "conj", "cop", "ctr",
        "exp", "int", "n", "n-adv", "n-pr", "n-pref", "n-suf", "n-t",
        "num", "pn", "pref", "prt", "suf", "unc",
        "v1", "v1-s", "v5aru", "v5b", "v5g", "v5k", "v5k-s", "v5m",
        "v5n", "v5r", "v5r-i", "v5s", "v5t", "v5u", "v5u-s", "v5uru",
        "vi", "vk", "vn", "vr", "vs", "vs-c", "vs-i", "vs-s", "vt", "vz"
    )

    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tagCodes.forEach { code ->
            val label = resolveTag(code)
            val isPos = code in posCategories
            SuggestionChip(
                onClick = {},
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = if (isPos) SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) else SuggestionChipDefaults.suggestionChipColors()
            )
        }
    }
}

@Composable
private fun ExampleCard(example: ExampleSentence) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = example.japanese,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (example.english.isNotBlank()) {
                Text(
                    text = example.english,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }
        }
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