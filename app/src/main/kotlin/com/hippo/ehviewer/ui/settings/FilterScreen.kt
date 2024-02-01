package com.hippo.ehviewer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.client.EhFilter.forget
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhFilter.trigger
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.coroutines.resume
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.groupByToObserved

@Destination
@Composable
fun FilterScreen(navigator: DestinationsNavigator) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val allFilterMap = remember { scope.async { EhFilter.filters.await().groupByToObserved { it.mode } } }
    val dialogState = LocalDialogState.current
    val textIsEmpty = stringResource(R.string.text_is_empty)
    val labelExist = stringResource(R.string.label_text_exist)

    fun addFilter() {
        scope.launch {
            dialogState.dialog { cont ->
                val types = stringArrayResource(id = R.array.filter_entries)
                var type by remember { mutableStateOf(types[0]) }
                var value by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }
                fun invalidateAndSave() {
                    if (value.isBlank()) {
                        error = textIsEmpty
                        return
                    }
                    error = null
                    val mode = FilterMode.entries[types.indexOf(type)]
                    val filter = Filter(mode, value)
                    filter.remember {
                        if (it) {
                            cont.resume(Unit)
                            requireNotNull(allFilterMap.getCompleted()[mode]).add(filter)
                        } else {
                            error = labelExist
                        }
                    }
                }
                AlertDialog(
                    onDismissRequest = { cont.cancel() },
                    confirmButton = {
                        TextButton(onClick = ::invalidateAndSave) {
                            Text(text = stringResource(id = R.string.add))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { cont.cancel() }) {
                            Text(text = stringResource(id = android.R.string.cancel))
                        }
                    },
                    title = {
                        Text(text = stringResource(id = R.string.add_filter))
                    },
                    text = {
                        var expanded by remember { mutableStateOf(false) }
                        Column {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier.menuAnchor(),
                                    readOnly = true,
                                    value = type,
                                    onValueChange = {},
                                    label = {
                                        Text(text = stringResource(id = R.string.filter_label))
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    types.forEach {
                                        DropdownMenuItem(
                                            text = { Text(text = it) },
                                            onClick = {
                                                expanded = false
                                                type = it
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.size(16.dp))
                            val isError = error != null
                            OutlinedTextField(
                                value = value,
                                onValueChange = { value = it },
                                label = { Text(text = stringResource(id = R.string.filter_text)) },
                                supportingText = { error?.let { Text(text = it) } },
                                trailingIcon = {
                                    if (isError) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                isError = isError,
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done,
                                ),
                            )
                        }
                    },
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.filter)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            dialogState.awaitPermissionOrCancel(
                                title = R.string.filter,
                                showCancelButton = false,
                            ) {
                                Text(text = stringResource(id = R.string.filter_tip))
                            }
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.Help, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = ::addFilter) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { paddingValues ->
        Deferred({ allFilterMap.await() }) { filters ->
            LazyColumn(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = paddingValues,
            ) {
                var showTip = true
                filters.forEach { (filterMode, filters) ->
                    val title = when (filterMode) {
                        FilterMode.TITLE -> R.string.filter_title
                        FilterMode.UPLOADER -> R.string.filter_uploader
                        FilterMode.TAG -> R.string.filter_tag
                        FilterMode.TAG_NAMESPACE -> R.string.filter_tag_namespace
                        FilterMode.COMMENTER -> R.string.filter_commenter
                        FilterMode.COMMENT -> R.string.filter_comment
                    }
                    if (filters.isNotEmpty()) {
                        stickyHeader(key = filterMode) {
                            Text(
                                text = stringResource(id = title),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).animateItemPlacement(),
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(filters, key = { requireNotNull(it.id) }) { filter ->
                            val filterCheckBoxRecomposeScope = currentRecomposeScope
                            Row(
                                modifier = Modifier.fillMaxWidth().animateItemPlacement().clickable { filter.trigger { filterCheckBoxRecomposeScope.invalidate() } },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = filter.enable,
                                    onCheckedChange = { filter.trigger { filterCheckBoxRecomposeScope.invalidate() } },
                                )
                                Text(text = filter.text, modifier = Modifier.weight(1F))
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            dialogState.awaitPermissionOrCancel(confirmText = R.string.delete) {
                                                Text(text = stringResource(id = R.string.delete_filter, filter.text))
                                            }
                                            filter.forget {
                                                filters.remove(filter)
                                            }
                                        }
                                    },
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                                }
                            }
                        }
                        showTip = false
                    }
                }
                if (showTip) {
                    item {
                        Column(
                            modifier = Modifier.padding(paddingValues).fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = Modifier.size(80.dp))
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp).size(120.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(id = R.string.filter),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
