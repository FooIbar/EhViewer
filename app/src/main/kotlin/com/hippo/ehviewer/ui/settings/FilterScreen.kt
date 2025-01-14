package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.client.EhFilter.forget
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhFilter.trigger
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.tools.Await
import com.hippo.ehviewer.ui.tools.thenIf
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.coroutines.resume
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.groupByToObserved

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.FilterScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val allFilterMap = remember { async { EhFilter.filters.await().groupByToObserved { it.mode } } }
    val textIsEmpty = stringResource(R.string.text_is_empty)
    val labelExist = stringResource(R.string.label_text_exist)
    val animateItems by Settings.animateItems.collectAsState()

    fun addFilter() {
        launch {
            dialog { cont ->
                val types = stringArrayResource(id = R.array.filter_entries)
                val type = rememberTextFieldState(types[0])
                val state = rememberTextFieldState()
                var error by remember { mutableStateOf<String?>(null) }
                fun invalidateAndSave() {
                    if (state.text.isBlank()) {
                        error = textIsEmpty
                        return
                    }
                    error = null
                    val mode = FilterMode.entries[types.indexOf(type.text)]
                    val filter = Filter(mode, state.text.toString())
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
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    readOnly = true,
                                    state = type,
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
                                                type.setTextAndPlaceCursorAtEnd(it)
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.size(16.dp))
                            val isError = error != null
                            OutlinedTextField(
                                state = state,
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
                                lineLimits = TextFieldLineLimits.SingleLine,
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
                        launch {
                            awaitConfirmationOrCancel(
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
        Await({ allFilterMap.await() }) { filters ->
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
                        item(key = filterMode) {
                            Text(
                                text = stringResource(id = title),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).thenIf(animateItems) { animateItem() },
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(filters, key = { requireNotNull(it.id) }) { filter ->
                            val filterCheckBoxRecomposeScope = currentRecomposeScope
                            Row(
                                modifier = Modifier.fillMaxWidth().thenIf(animateItems) { animateItem() }.clickable { filter.trigger { filterCheckBoxRecomposeScope.invalidate() } },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = filter.enable,
                                    onCheckedChange = { filter.trigger { filterCheckBoxRecomposeScope.invalidate() } },
                                )
                                Text(text = filter.text, modifier = Modifier.weight(1F))
                                IconButton(
                                    onClick = {
                                        launch {
                                            awaitConfirmationOrCancel(confirmText = R.string.delete) {
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
