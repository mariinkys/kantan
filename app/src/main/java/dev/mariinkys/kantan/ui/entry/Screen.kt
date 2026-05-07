package dev.mariinkys.kantan.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mariinkys.kantan.data.repository.isKanji
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.domain.model.Example
import dev.mariinkys.kantan.domain.model.KanjiEntry
import dev.mariinkys.kantan.domain.model.Sense
import dev.mariinkys.kantan.util.ConjugationTable
import dev.mariinkys.kantan.util.resolveTag
import kotlinx.coroutines.launch


// Annotation tag used in AnnotatedString to mark tappable "see also" terms
private const val SEE_ALSO_TAG = "SEE_ALSO"

// Matches "see: <term> optional gloss", the Japanese term is group 1, trailing gloss is group 2
private val SEE_ALSO_REGEX = Regex("""^see:\s+(\S+)\s*(.*)$""", RegexOption.IGNORE_CASE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    onBack: () -> Unit,
    onKanjiClick: (character: String) -> Unit,
    // Called when the user taps a "See also" term or any cross-reference link.
    onTermClick: (term: String) -> Unit,
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    s.entry.expression,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                val altForms = s.entry.variants
                                    .filter { it != s.entry.expression }
                                    .filter { v -> v.any { it.isKanji() } }
                                if (altForms.isNotEmpty()) {
                                    Text(
                                        text = "· ${altForms.joinToString("、")}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (s.entry.reading.isNotBlank() && s.entry.reading != s.entry.expression) {
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
                    // only show the favorite button once the entry has loaded
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
                onTermClick = onTermClick,
                conjugationTable = s.conjugationTable,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun EntryDetailContent(
    entry: DictionaryEntry,
    kanji: List<KanjiEntry>,
    conjugationTable: ConjugationTable?,
    onKanjiClick: (String) -> Unit,
    onTermClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = buildList {
        add("Definitions")
        if (conjugationTable != null) add("Inflections")
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
                "Definitions" -> DefinitionsTab(entry, onTermClick)
                "Inflections" -> ConjugationsTab(conjugationTable!!)
                "Kanji" -> KanjiTab(kanji, onKanjiClick)
            }
        }
    }
}

@Composable
private fun DefinitionsTab(entry: DictionaryEntry, onTermClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        val wordTags = buildList {
            if (entry.rules.isNotBlank()) addAll(entry.rules.split(" "))
            if (entry.tags.isNotBlank()) addAll(entry.tags.split(" "))
        }
            .filter { it.isNotBlank() }
            .distinct()
            .mapNotNull { tag ->
                val label = resolveTag(tag)
                if (label.none { it.isLetterOrDigit() }) null else label
            }
            .distinct()

        if (wordTags.isNotEmpty()) {
            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    wordTags.forEach { label ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // Non-standard / irregular readings
        if (entry.nonStandardReadings.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Non-standard readings:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    entry.nonStandardReadings.forEach { reading ->
                        Text(
                            text = reading,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }

        // One section per sense
        itemsIndexed(entry.senses) { index, sense ->
            SenseSection(
                index = index + 1,
                sense = sense,
                isLast = index == entry.senses.lastIndex,
                onTermClick
            )
        }
    }
}

@Composable
private fun SenseSection(index: Int, sense: Sense, isLast: Boolean, onTermClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = if (index == 1) 4.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // POS header row  e.g. "① Noun"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sense number badge
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            // POS label
            if (sense.partOfSpeech.isNotBlank()) {
                Text(
                    text = sense.partOfSpeech,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // Additional POS tags as small chips (e.g. Transitive, Usually kana)
            sense.posTags.drop(1).forEach { tag ->
                val label = resolveTag(tag)
                if (label.any { it.isLetterOrDigit() }) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }

        // Glosses
        sense.glosses.forEachIndexed { i, gloss ->
            Text(
                text = if (sense.glosses.size == 1) gloss
                else "${i + 1}. $gloss",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Info notes (See also, Usually written as, etc.)
        sense.info.forEach { note ->
            InfoNote(note = note, onTermClick = onTermClick)
        }

        // Example sentences
        if (sense.examples.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            sense.examples.forEach { example ->
                ExampleCard(example)
            }
        }

        if (!isLast) {
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
        }
    }
}


/**
 * Renders an info/note string.
 *
 * If the note matches the "see: <term> <gloss>" pattern produced by the importer,
 * the Japanese term is rendered as a tappable underlined link. Everything else is
 * plain italic text.
 *
 */
@Composable
private fun InfoNote(note: String, onTermClick: (String) -> Unit) {
    val linkColor = MaterialTheme.colorScheme.primary
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    val baseStyle = MaterialTheme.typography.bodySmall

    val match = SEE_ALSO_REGEX.matchEntire(note.trim())

    if (match != null) {
        val term = match.groupValues[1]
        val gloss = match.groupValues[2].trim()

        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(color = baseColor, fontStyle = FontStyle.Italic)) {
                append("See also: ")
            }
            withLink(
                LinkAnnotation.Clickable(
                    tag = SEE_ALSO_TAG,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        )
                    ),
                    linkInteractionListener = {
                        // Strip furigana annotations like 女（じょ） → 女
                        val cleanTerm = term
                            .replace(Regex("""（[^）]*）"""), "")
                            .replace(Regex("""\([^)]*\)"""), "")
                            .trim()
                        onTermClick(cleanTerm)
                    }
                )
            ) {
                append(term)
            }
            if (gloss.isNotBlank()) {
                withStyle(SpanStyle(color = baseColor, fontStyle = FontStyle.Italic)) {
                    append("  $gloss")
                }
            }
        }

        Text(
            text = annotated,
            style = baseStyle,
            modifier = Modifier.padding(start = 4.dp)
        )
    } else {
        Text(
            text = note,
            style = baseStyle,
            color = baseColor,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun ExampleCard(example: Example) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "🇯🇵 ${example.japanese}",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp
        )
        Text(
            text = "🇬🇧 ${example.english}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConjugationsTab(table: ConjugationTable) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Form", Modifier.weight(2f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Affirmative", Modifier.weight(2f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Negative", Modifier.weight(2f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }

        itemsIndexed(table.rows) { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    row.label, Modifier.weight(2f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    row.affirmative, Modifier.weight(2f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    row.negative, Modifier.weight(2f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (index < table.rows.lastIndex) HorizontalDivider(thickness = 0.5.dp)
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