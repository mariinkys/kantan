package dev.mariinkys.kantan.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mariinkys.kantan.BuildConfig
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import dev.mariinkys.kantan.ui.search.handwriting.HandwritingBottomSheet

@Composable
fun SearchScreen(
    onMenuClick: () -> Unit,
    onEntryClick: (expression: String, reading: String) -> Unit,
    modifier: Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    var showHandwriting by remember { mutableStateOf(false) }
    val randomState by viewModel.randomEntryState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        SearchBar(
            query = viewModel.query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clearQuery,
            onHandwritingClick = { showHandwriting = true },
            onMenuClick = onMenuClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        HorizontalDivider()

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = viewModel.searchState) {
                is SearchState.Idle -> EmptyPrompt(
                    randomState = randomState,
                    onEntryClick = onEntryClick
                )

                is SearchState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                is SearchState.NoResults -> NoResultsMessage(state.query)
                is SearchState.Results -> ResultList(
                    entries = state.entries,
                    onEntryClick = onEntryClick
                )
            }
        }

        // Handwriting sheet
        if (showHandwriting) {
            HandwritingBottomSheet(
                onDismiss = { showHandwriting = false },
                onCharacterSelected = { character ->
                    val newQuery = viewModel.query + character
                    viewModel.onQueryChange(newQuery)
                }
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onHandwritingClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
    }

    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newFieldValue ->
            textFieldValue = newFieldValue
            onQueryChange(newFieldValue.text)
        },
        modifier = modifier.focusRequester(focusRequester),
        placeholder = {
            Text(
                "Search here…",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
            }
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, "Clear")
                    }
                }
                if (BuildConfig.OCR_ENABLED) {
                    IconButton(onClick = onHandwritingClick) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Draw to search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun ResultList(
    entries: List<DictionaryEntry>,
    onEntryClick: (String, String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(
            items = entries,
            key = { "${it.expression}|${it.reading}" }
        ) { entry ->
            EntryRow(
                entry = entry,
                onClick = { onEntryClick(entry.expression, entry.reading) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun EntryRow(entry: DictionaryEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.expression, style = MaterialTheme.typography.titleMedium)
            if (entry.reading.isNotBlank() && entry.reading != entry.expression) {
                Text(
                    text = entry.reading,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = entry.longDefinition,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun EmptyPrompt(
    randomState: RandomEntryDetailState,
    onEntryClick: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ready to learn?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Search using English, Rōmaji, or Kanji",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        when (randomState) {
            is RandomEntryDetailState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            is RandomEntryDetailState.Success -> {
                DiscoveryCard(
                    entry = randomState.entry,
                    onClick = {
                        onEntryClick(
                            randomState.entry.expression,
                            randomState.entry.reading
                        )
                    }
                )
            }

            is RandomEntryDetailState.Error -> {
                androidx.compose.material3.ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Did you know?",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "You can draw kanji directly by clicking the pencil icon if you don't know the reading!",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryCard(
    entry: DictionaryEntry,
    onClick: () -> Unit
) {
    androidx.compose.material3.ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Did you know?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You can draw kanji directly by clicking the pencil icon if you don't know the reading!",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(modifier = Modifier.alpha(0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Featured Word", style = MaterialTheme.typography.labelSmall)
            Text(
                text = entry.expression,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                entry.longDefinition,
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoResultsMessage(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("😶", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                "No results for \u201c$query\u201d",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}