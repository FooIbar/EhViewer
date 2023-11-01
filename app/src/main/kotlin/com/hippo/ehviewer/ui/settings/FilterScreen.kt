package com.hippo.ehviewer.ui.settings

import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.client.EhFilter.forget
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhFilter.trigger
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.databinding.DialogAddFilterBinding
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.LocalDialogState
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.groupByToObserved

@Composable
fun FilterScreen() {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val allFilterMap = remember { scope.async { EhFilter.filters.await().groupByToObserved { it.mode } } }
    val dialogState = LocalDialogState.current
    class AddFilterDialogHelper(private val dialog: AlertDialog) : View.OnClickListener {
        private val mArray = dialog.context.resources.getStringArray(R.array.filter_entries)
        private val binding = DialogAddFilterBinding.bind(dialog.findViewById(R.id.base)!!)
        init {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)!!.setOnClickListener(this)
        }
        override fun onClick(v: View) {
            val emptyError = context.getString(R.string.text_is_empty)
            val text1: String?
            binding.spinner.run {
                text1 = editText?.text?.toString()
                if (text1.isNullOrBlank()) {
                    error = emptyError
                    return
                } else {
                    error = null
                }
            }
            val text: String?
            binding.textInputLayout.run {
                text = editText?.text?.toString()?.trim()
                if (text.isNullOrBlank()) {
                    error = emptyError
                    return
                } else {
                    error = null
                }
            }
            binding.textInputLayout.run {
                val mode = FilterMode.entries[mArray.indexOf(text1)]
                val filter = Filter(mode, text!!)
                filter.remember {
                    if (it) {
                        post { dialog.dismiss() }
                        requireNotNull(allFilterMap.getCompleted()[mode]).add(filter)
                    } else {
                        post { error = context.getString(R.string.label_text_exist) }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.filter)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { BaseDialogBuilder(context).setTitle(R.string.filter).setMessage(R.string.filter_tip).setPositiveButton(android.R.string.ok, null).show() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.Help, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { AddFilterDialogHelper(BaseDialogBuilder(context).setTitle(R.string.add_filter).setView(R.layout.dialog_add_filter).setPositiveButton(R.string.add, null).setNegativeButton(android.R.string.cancel, null).show()) }) {
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
                        stickyHeader {
                            Text(
                                text = stringResource(id = title),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(filters) { filter ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val filterCheckBoxRecomposeScope = currentRecomposeScope
                                Checkbox(
                                    checked = filter.enable,
                                    onCheckedChange = { filter.trigger { filterCheckBoxRecomposeScope.invalidate() } },
                                )
                                Text(text = filter.text, modifier = Modifier.weight(1F))
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            dialogState.awaitPermissionOrCancel(
                                                confirmText = R.string.delete,
                                                dismissText = android.R.string.cancel,
                                            ) {
                                                Text(text = stringResource(id = R.string.delete_filter, filter.text))
                                            }
                                            filter.forget {
                                                withUIContext {
                                                    filters.remove(filter)
                                                }
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
