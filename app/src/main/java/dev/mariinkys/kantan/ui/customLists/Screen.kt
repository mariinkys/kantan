package dev.mariinkys.kantan.ui.customLists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mariinkys.kantan.domain.model.CustomList

@Composable
fun CustomListsScreen(
    onListClick: (listId: Int, name: String) -> Unit,
    onMenuClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: CustomListsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CustomListsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showCreateDialog) {
        CreateListDialog(
            onConfirm = { name ->
                viewModel.createList(name)
                @Suppress("AssignedValueIsNeverRead")
                showCreateDialog = false
            },
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showCreateDialog = false
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                    Text("My Lists", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(
                    onClick = viewModel::refresh,
                    enabled = state !is CustomListsState.Loading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh lists",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            when (val s = state) {
                is CustomListsState.Loading -> Box(
                    Modifier.fillMaxSize(), Alignment.Center
                ) { CircularProgressIndicator() }

                is CustomListsState.Ready -> {
                    if (s.lists.isEmpty()) {
                        EmptyLists()
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(items = s.lists, key = { it.id }) { list ->
                                ListFolderRow(
                                    list = list,
                                    onClick = { onListClick(list.id, list.name) },
                                    onDelete = { viewModel.deleteList(list.id) },
                                    onRename = { newName ->
                                        viewModel.renameList(
                                            list.id,
                                            newName
                                        )
                                    },
                                    onAddBulk = { terms ->
                                        viewModel.addBulkEntries(
                                            list.id,
                                            terms
                                        )
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                @Suppress("AssignedValueIsNeverRead")
                showCreateDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create new list")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ListFolderRow(
    list: CustomList,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onAddBulk: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showAddBulkDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showRenameDialog) {
        RenameListDialog(
            currentName = list.name,
            onConfirm = { newName ->
                @Suppress("AssignedValueIsNeverRead")
                showRenameDialog = false
                onRename(newName)
            },
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showRenameDialog = false
            }
        )
    }

    if (showAddBulkDialog) {
        AddBulkDialog(
            onConfirm = { terms ->
                @Suppress("AssignedValueIsNeverRead")
                showAddBulkDialog = false
                onAddBulk(terms)
            },
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showAddBulkDialog = false
            }
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                @Suppress("AssignedValueIsNeverRead")
                showSheet = false
            },
            sheetState = sheetState
        ) {
            Text(
                text = list.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            )
            HorizontalDivider()
            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                },
                label = { Text("Rename list") },
                selected = false,
                onClick = {
                    @Suppress("AssignedValueIsNeverRead")
                    showSheet = false
                    @Suppress("AssignedValueIsNeverRead")
                    showRenameDialog = true
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            NavigationDrawerItem(
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = { Text("Add Bulk", color = MaterialTheme.colorScheme.primary) },
                selected = false,
                onClick = {
                    @Suppress("AssignedValueIsNeverRead")
                    showSheet = false
                    @Suppress("AssignedValueIsNeverRead")
                    showAddBulkDialog = true
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            NavigationDrawerItem(
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                label = { Text("Delete list", color = MaterialTheme.colorScheme.error) },
                selected = false,
                onClick = {
                    @Suppress("AssignedValueIsNeverRead")
                    showSheet = false
                    onDelete()
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    @Suppress("AssignedValueIsNeverRead")
                    showSheet = true
                }
            )
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.widthIn(min = 80.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(list.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${list.entryCount} words",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete list",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateListDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New List") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenameListDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename List") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank() && name.trim() != currentName
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddBulkDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var terms by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bulk Words") },
        text = {
            OutlinedTextField(
                value = terms,
                onValueChange = { terms = it },
                label = { Text("Terms") },
                supportingText = { Text("Separate with commas (e.g., 女, 学校, 学生)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (terms.isNotBlank()) onConfirm(terms.trim()) },
                enabled = terms.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EmptyLists() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📋", style = MaterialTheme.typography.displaySmall)
            Text("No lists yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap + to create a new one",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}