package com.hippo.ehviewer.ui.screen

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.Download
import com.hippo.ehviewer.ui.LocalSideSheetState
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.confirmRemoveDownloadRange
import com.hippo.ehviewer.ui.destinations.GalleryDetailScreenDestination
import com.hippo.ehviewer.ui.main.DownloadCard
import com.hippo.ehviewer.ui.main.FabLayout
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.showMoveDownloadLabelList
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.DragHandle
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.SwipeToDismissBox2
import com.hippo.ehviewer.ui.tools.draggingHapticFeedback
import com.hippo.ehviewer.util.containsIgnoreCase
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.mapToLongArray
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

@Destination
@Composable
fun DownloadsScreen(navigator: DestinationsNavigator) {
    var label by rememberSaveable { mutableStateOf<String?>(null) }
    var keyword by rememberSaveable { mutableStateOf<String?>(null) }
    var filterType by rememberSaveable { mutableStateOf(-1) }
    var searchBarOffsetY by remember { mutableStateOf(0) }
    val searchFieldState = rememberTextFieldState()
    var selectMode by remember { mutableStateOf(false) }
    LockDrawer(selectMode)
    val checkedInfoMap = remember { mutableStateMapOf<Long, DownloadInfo>() }
    SideEffect {
        if (checkedInfoMap.isEmpty()) selectMode = false
    }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity<MainActivity>() }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dialogState = LocalDialogState.current
    val view = LocalView.current
    val title = stringResource(R.string.scene_download_title, label ?: stringResource(R.string.download_all))
    val hint = stringResource(R.string.search_bar_hint, title)
    val defaultName = stringResource(R.string.default_download_label_name)
    val allName = stringResource(R.string.download_all)
    val list = remember(label, filterType, keyword) {
        when (label) {
            null -> DownloadManager.allInfoList
            defaultName -> DownloadManager.defaultInfoList
            else -> DownloadManager.getLabelDownloadInfoList(label) ?: DownloadManager.allInfoList.also { label = null }
        }.filter { info ->
            (filterType == -1 || info.state == filterType) && keyword?.let { info.title.containsIgnoreCase(it) || info.titleJpn.containsIgnoreCase(it) || info.uploader.containsIgnoreCase(it) } ?: true
        }
    }
    val labelsList = DownloadManager.labelList

    val newLabel = stringResource(R.string.new_label_title)
    val labelsStr = stringResource(R.string.download_labels)
    val labelEmpty = stringResource(R.string.label_text_is_empty)
    val defaultInvalid = stringResource(R.string.label_text_is_invalid)
    val labelExists = stringResource(R.string.label_text_exist)
    with(activity) {
        ProvideSideSheetContent { drawerState ->
            fun closeSheet() = coroutineScope.launch { drawerState.close() }
            TopAppBar(
                title = { Text(text = labelsStr) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val text = dialogState.awaitInputText(title = newLabel, hint = labelsStr) { text ->
                                    when {
                                        text.isBlank() -> labelEmpty
                                        text == defaultName -> defaultInvalid
                                        DownloadManager.containLabel(text) -> labelExists
                                        else -> null
                                    }
                                }
                                DownloadManager.addLabel(text)
                            }
                        },
                    ) {
                        Icon(imageVector = Icons.Default.NewLabel, contentDescription = null)
                    }
                    val letMeSelect = stringResource(R.string.let_me_select)
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                dialogState.showSelectActions(R.string.default_download_label) {
                                    onSelect(letMeSelect) {
                                        Settings.hasDefaultDownloadLabel = false
                                    }
                                    onSelect(defaultName) {
                                        Settings.hasDefaultDownloadLabel = true
                                        Settings.defaultDownloadLabel = defaultName
                                    }
                                    DownloadManager.labelList.forEach { (label) ->
                                        onSelect(label) {
                                            Settings.hasDefaultDownloadLabel = true
                                            Settings.defaultDownloadLabel = label
                                        }
                                    }
                                }
                            }
                        },
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    }
                },
            )

            val labelsListState = rememberLazyListState()
            val reorderableLabelState = rememberReorderableLazyColumnState(labelsListState) { from, to ->
                coroutineScope.launch {
                    DownloadManager.moveLabel(from.index - 2, to.index - 2)
                }
                view.performHapticFeedback(draggingHapticFeedback)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = labelsListState,
                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
            ) {
                stickyHeader {
                    val all = DownloadManager.allInfoList
                    ListItem(
                        modifier = Modifier.clickable {
                            label = null
                            closeSheet()
                        },
                        tonalElevation = 1.dp,
                        shadowElevation = 1.dp,
                        headlineContent = {
                            Text("$allName [${all.size}]")
                        },
                    )
                }
                stickyHeader {
                    val default = DownloadManager.defaultInfoList
                    ListItem(
                        modifier = Modifier.clickable {
                            label = defaultName
                            closeSheet()
                        },
                        tonalElevation = 1.dp,
                        shadowElevation = 1.dp,
                        headlineContent = {
                            Text("$defaultName [${default.size}]")
                        },
                    )
                }
                items(labelsList, key = { it.label }) { (item) ->
                    val dismissState = rememberDismissState(
                        confirmValueChange = {
                            if (it == DismissValue.DismissedToStart) {
                                coroutineScope.launch {
                                    DownloadManager.deleteLabel(item)
                                }
                            }
                            true
                        },
                    )
                    ReorderableItem(reorderableLabelState, key = item) { isDragging ->
                        SwipeToDismissBox2(
                            state = dismissState,
                            backgroundContent = {},
                            directions = setOf(DismissDirection.EndToStart),
                        ) {
                            val elevation by animateDpAsState(
                                if (isDragging) {
                                    8.dp // md.sys.elevation.level4
                                } else {
                                    1.dp // md.sys.elevation.level1
                                },
                                label = "elevation",
                            )
                            val thatList = DownloadManager.getLabelDownloadInfoList(item)
                            val text = if (thatList != null) "$item [${thatList.size}]" else item
                            ListItem(
                                modifier = Modifier.clickable {
                                    label = item
                                    closeSheet()
                                },
                                tonalElevation = 1.dp,
                                shadowElevation = elevation,
                                headlineContent = {
                                    Text(text)
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val new = dialogState.awaitInputText(title = newLabel, hint = labelsStr) { text ->
                                                        when {
                                                            text.isBlank() -> labelEmpty
                                                            text == defaultName -> defaultInvalid
                                                            DownloadManager.containLabel(text) -> labelExists
                                                            else -> null
                                                        }
                                                    }
                                                    DownloadManager.renameLabel(item, new)
                                                }
                                            },
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                        }
                                        DragHandle()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    SearchBarScreen(
        title = title,
        searchFieldState = searchFieldState,
        searchFieldHint = hint,
        showSearchFab = selectMode,
        onApplySearch = { keyword = it.takeUnless { it.isBlank() } },
        onSearchExpanded = { selectMode = false },
        onSearchHidden = {},
        searchBarOffsetY = { searchBarOffsetY },
        trailingIcon = {
            var expanded by remember { mutableStateOf(false) }
            val sideSheetState = LocalSideSheetState.current
            IconButton(onClick = { coroutineScope.launch { sideSheetState.open() } }) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.Label, contentDescription = stringResource(id = R.string.download_labels))
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
            }
            val states = stringArrayResource(id = R.array.download_state)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.download_filter)) },
                    onClick = {
                        expanded = false
                        coroutineScope.launch {
                            filterType = dialogState.showSingleChoice(states, filterType + 1) - 1
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.download_start_all)) },
                    onClick = {
                        expanded = false
                        val intent = Intent(activity, DownloadService::class.java)
                        intent.action = DownloadService.ACTION_START_ALL
                        ContextCompat.startForegroundService(activity, intent)
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.download_stop_all)) },
                    onClick = {
                        expanded = false
                        coroutineScope.launchIO {
                            DownloadManager.stopAllDownload()
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.download_reset_reading_progress)) },
                    onClick = {
                        expanded = false
                        coroutineScope.launchIO {
                            dialogState.awaitPermissionOrCancel(
                                confirmText = android.R.string.ok,
                                dismissText = android.R.string.cancel,
                            ) {
                                Text(text = stringResource(id = R.string.reset_reading_progress_message))
                            }
                            withNonCancellableContext {
                                DownloadManager.resetAllReadingProgress()
                            }
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.download_start_all_reversed)) },
                    onClick = {
                        expanded = false
                        val gidList = list.filter { it.state != DownloadInfo.STATE_FINISH }.asReversed().mapToLongArray(DownloadInfo::gid)
                        val intent = Intent(activity, DownloadService::class.java)
                        intent.action = DownloadService.ACTION_START_RANGE
                        intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
                        ContextCompat.startForegroundService(activity, intent)
                    },
                )
            }
        },
    ) { contentPadding ->
        val height by collectListThumbSizeAsState()
        val layoutDirection = LocalLayoutDirection.current
        val marginH = dimensionResource(id = R.dimen.gallery_list_margin_h)
        val marginV = dimensionResource(id = R.dimen.gallery_list_margin_v)
        val realPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + marginV,
            bottom = contentPadding.calculateBottomPadding() + marginV,
            start = contentPadding.calculateStartPadding(layoutDirection) + marginH,
            end = contentPadding.calculateEndPadding(layoutDirection) + marginH,
        )
        val listState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyColumnState(listState) { from, to ->
            val fromIndex = from.index - 1
            val toIndex = to.index - 1
            val fromItem = list[fromIndex]
            val toItem = list[toIndex]
            val newList = DownloadManager.moveDownload(fromItem, toItem)
            coroutineScope.launchIO {
                EhDB.updateDownloadInfo(newList)
            }
            view.performHapticFeedback(draggingHapticFeedback)
        }
        val searchBarConnection = remember {
            val topPaddingPx = with(density) { contentPadding.calculateTopPadding().roundToPx() }
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    searchBarOffsetY = (searchBarOffsetY + consumed.y).roundToInt().coerceIn(-topPaddingPx, 0)
                    return Offset.Zero // We never consume it
                }
            }
        }
        FastScrollLazyColumn(
            modifier = Modifier.nestedScroll(searchBarConnection),
            state = listState,
            contentPadding = realPadding,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.gallery_list_interval)),
        ) {
            // Fix the first item's reorder animation
            item {}
            items(list, key = { it.gid }) { info ->
                ReorderableItem(reorderableState, key = info.gid) {
                    val checked = info.gid in checkedInfoMap
                    CheckableItem(checked = checked) {
                        DownloadCard(
                            onClick = {
                                if (selectMode) {
                                    if (checked) {
                                        checkedInfoMap.remove(info.gid)
                                    } else {
                                        checkedInfoMap[info.gid] = info
                                    }
                                } else {
                                    context.navToReader(info.galleryInfo)
                                }
                            },
                            onThumbClick = {
                                navigator.navigate(GalleryDetailScreenDestination(GalleryInfoArgs(info.galleryInfo)))
                            },
                            onLongClick = {
                                checkedInfoMap[info.gid] = info
                                selectMode = true
                            },
                            onStart = {
                                val intent = Intent(activity, DownloadService::class.java)
                                intent.action = DownloadService.ACTION_START
                                intent.putExtra(DownloadService.KEY_GALLERY_INFO, info.galleryInfo)
                                ContextCompat.startForegroundService(activity, intent)
                            },
                            onStop = {
                                coroutineScope.launchIO {
                                    DownloadManager.stopDownload(info.gid)
                                }
                            },
                            info = info,
                            modifier = Modifier.height(height),
                        )
                    }
                }
            }
        }

        Deferred({ delay(200) }) {
            if (list.isEmpty()) {
                Column(
                    modifier = Modifier.padding(realPadding).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = EhIcons.Big.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(id = R.string.no_download_info),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }
    }

    FabLayout(
        hidden = !selectMode,
        expanded = selectMode,
        onExpandChanged = {
            checkedInfoMap.clear()
            selectMode = false
        },
        autoCancel = false,
    ) {
        onClick(Icons.Default.DoneAll) {
            val info = list.associateBy { it.gid }
            checkedInfoMap.putAll(info)
            throw CancellationException()
        }
        onClick(Icons.Default.PlayArrow) {
            val gidList = checkedInfoMap.run { toMap().values.also { clear() } }.mapToLongArray(DownloadInfo::gid)
            val intent = Intent(activity, DownloadService::class.java)
            intent.action = DownloadService.ACTION_START_RANGE
            intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
            ContextCompat.startForegroundService(context, intent)
        }
        onClick(Icons.Default.Pause) {
            val gidList = checkedInfoMap.run { toMap().values.also { clear() } }.mapToLongArray(DownloadInfo::gid)
            DownloadManager.stopRangeDownload(gidList)
        }
        onClick(Icons.Default.Delete) {
            val infoList = checkedInfoMap.run { toMap().values.also { clear() } }
            dialogState.confirmRemoveDownloadRange(infoList)
        }
        onClick(Icons.AutoMirrored.Default.DriveFileMove) {
            val infoList = checkedInfoMap.run { toMap().values.also { clear() } }
            dialogState.showMoveDownloadLabelList(infoList)
        }
    }
}

object DownloadsFragment {
    const val KEY_GID = "gid"
    const val KEY_ACTION = "action"
    const val ACTION_CLEAR_DOWNLOAD_SERVICE = "clear_download_service"
}
