package com.hippo.ehviewer.ui.screen

import android.content.Intent
import android.view.ViewConfiguration
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import arrow.core.partially1
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.data.contains
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadManager.labelList
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.SortMode
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.Download
import com.hippo.ehviewer.ui.LocalSideSheetState
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.confirmRemoveDownloadRange
import com.hippo.ehviewer.ui.main.DownloadCard
import com.hippo.ehviewer.ui.main.FAB_ANIMATE_TIME
import com.hippo.ehviewer.ui.main.FabLayout
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.showMoveDownloadLabelList
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.DragHandle
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalStaggeredGrid
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.SwipeToDismissBox2
import com.hippo.ehviewer.ui.tools.delegateSnapshotUpdate
import com.hippo.ehviewer.ui.tools.draggingHapticFeedback
import com.hippo.ehviewer.ui.tools.rememberInVM
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
import moe.tarsin.coroutines.onEachLatest
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

@Destination
@Composable
fun DownloadsScreen(navigator: DestinationsNavigator) {
    var label by Settings.recentDownloadLabel.asMutableState()
    var gridView by Settings.gridView.asMutableState()
    var sortMode by Settings.downloadSortMode.asMutableState()
    var keyword by rememberSaveable { mutableStateOf<String?>(null) }
    var filterType by rememberSaveable { mutableIntStateOf(-1) }
    var searchBarOffsetY by remember(label) { mutableIntStateOf(0) }
    val searchFieldState = rememberTextFieldState()

    var fabExpanded by remember { mutableStateOf(false) }
    var fabHidden by remember { mutableStateOf(false) }
    val checkedInfoMap = remember { mutableStateMapOf<Long, DownloadInfo>() }
    val selectMode by rememberUpdatedState(checkedInfoMap.isNotEmpty())
    LockDrawer(selectMode)

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity<MainActivity>() }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dialogState = LocalDialogState.current
    val view = LocalView.current
    val allName = stringResource(R.string.download_all)
    val defaultName = stringResource(R.string.default_download_label_name)
    val title = stringResource(R.string.scene_download_title, if (label == "") allName else label ?: defaultName)
    val hint = stringResource(R.string.search_bar_hint, title)
    val sortModes = stringArrayResource(id = R.array.download_sort_modes)
    val downloadStates = stringArrayResource(id = R.array.download_state)
    val list = remember(label, filterType, keyword, sortMode) {
        DownloadManager.downloadInfoList.filterTo(mutableStateListOf()) { info ->
            (label == "" || info.label == label) && (filterType == -1 || info.state == filterType) &&
                keyword.orEmpty() in info
        }
    }

    val newLabel = stringResource(R.string.new_label_title)
    val renameLabel = stringResource(R.string.rename_label_title)
    val labelsStr = stringResource(R.string.download_labels)
    val labelEmpty = stringResource(R.string.label_text_is_empty)
    val defaultInvalid = stringResource(R.string.label_text_is_invalid)
    val labelExists = stringResource(R.string.label_text_exist)
    val downloadsCount by rememberInVM { EhDB.downloadsCount }.collectAsState(emptyMap())
    val totalCount = remember(downloadsCount) { downloadsCount.values.sum() }
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
                                        Settings.defaultDownloadLabel = null
                                    }
                                    labelList.forEach { (label) ->
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
                val fromPosition = from.index - 2
                val toPosition = to.index - 2
                labelList.apply { add(toPosition, removeAt(fromPosition)) }
                view.performHapticFeedback(draggingHapticFeedback)
            }
            var fromIndex by remember { mutableIntStateOf(-1) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = labelsListState,
                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
            ) {
                stickyHeader {
                    ListItem(
                        modifier = Modifier.clickable {
                            label = ""
                            closeSheet()
                        },
                        tonalElevation = 1.dp,
                        shadowElevation = 1.dp,
                        headlineContent = {
                            Text("$allName [$totalCount]")
                        },
                    )
                }
                stickyHeader {
                    ListItem(
                        modifier = Modifier.clickable {
                            label = null
                            closeSheet()
                        },
                        tonalElevation = 1.dp,
                        shadowElevation = 1.dp,
                        headlineContent = {
                            Text("$defaultName [${downloadsCount.getOrDefault(null, 0)}]")
                        },
                    )
                }
                itemsIndexed(labelList, key = { _, item -> item.label }) { index, (item) ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                coroutineScope.launch {
                                    DownloadManager.deleteLabel(item)
                                    if (label == item) {
                                        label = ""
                                    }
                                }
                            }
                            true
                        },
                    )
                    ReorderableItem(reorderableLabelState, key = item) { isDragging ->
                        SwipeToDismissBox2(
                            state = dismissState,
                            backgroundContent = {},
                        ) {
                            val elevation by animateDpAsState(
                                if (isDragging) {
                                    8.dp // md.sys.elevation.level4
                                } else {
                                    1.dp // md.sys.elevation.level1
                                },
                                label = "elevation",
                            )
                            ListItem(
                                modifier = Modifier.clickable {
                                    label = item
                                    closeSheet()
                                },
                                tonalElevation = 1.dp,
                                shadowElevation = elevation,
                                headlineContent = {
                                    Text("$item [${downloadsCount.getOrDefault(item, 0)}]")
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val new = dialogState.awaitInputText(initial = item, title = renameLabel, hint = labelsStr) { text ->
                                                        when {
                                                            text.isBlank() -> labelEmpty
                                                            text == defaultName -> defaultInvalid
                                                            DownloadManager.containLabel(text) -> labelExists
                                                            else -> null
                                                        }
                                                    }
                                                    DownloadManager.renameLabel(item, new)
                                                    label = new
                                                }
                                            },
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                        }
                                        DragHandle(
                                            onDragStarted = {
                                                fromIndex = index
                                            },
                                            onDragStopped = {
                                                if (fromIndex != -1) {
                                                    if (fromIndex != index) {
                                                        val range = if (fromIndex < index) fromIndex..index else index..fromIndex
                                                        val toUpdate = labelList.slice(range)
                                                        toUpdate.zip(range).forEach { it.first.position = it.second }
                                                        coroutineScope.launchIO {
                                                            EhDB.updateDownloadLabel(toUpdate)
                                                        }
                                                    }
                                                    fromIndex = -1
                                                }
                                            },
                                        )
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
        onApplySearch = { keyword = it.takeUnless { it.isBlank() } },
        onSearchExpanded = {
            checkedInfoMap.clear()
            fabHidden = true
        },
        onSearchHidden = { fabHidden = false },
        searchBarOffsetY = { searchBarOffsetY },
        trailingIcon = {
            var expanded by remember { mutableStateOf(false) }
            val sideSheetState = LocalSideSheetState.current
            IconButton(onClick = { gridView = !gridView }) {
                val icon = if (gridView) Icons.AutoMirrored.Default.ViewList else Icons.Default.GridView
                Icon(imageVector = icon, contentDescription = null)
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.download_labels)) },
                    onClick = {
                        expanded = false
                        coroutineScope.launch {
                            sideSheetState.open()
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
        val searchBarConnection = remember {
            val slop = ViewConfiguration.get(context).scaledTouchSlop
            val topPaddingPx = with(density) { contentPadding.calculateTopPadding().roundToPx() }
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    val dy = -consumed.y
                    if (dy >= slop) {
                        fabHidden = true
                    } else if (dy <= -slop / 2) {
                        fabHidden = false
                    }
                    searchBarOffsetY = (searchBarOffsetY - dy).roundToInt().coerceIn(-topPaddingPx, 0)
                    return Offset.Zero // We never consume it
                }
            }
        }
        fun onItemClick(info: DownloadInfo) {
            coroutineScope.launchIO {
                EhDB.putHistoryInfo(info.galleryInfo)
            }
            context.navToReader(info.galleryInfo)
        }
        Crossfade(targetState = gridView, label = "Downloads") { showGridView ->
            if (showGridView) {
                val gridInterval = dimensionResource(R.dimen.gallery_grid_interval)
                val thumbColumns by Settings.thumbColumns.collectAsState()
                FastScrollLazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(thumbColumns),
                    modifier = Modifier.nestedScroll(searchBarConnection).fillMaxSize(),
                    verticalItemSpacing = gridInterval,
                    horizontalArrangement = Arrangement.spacedBy(gridInterval),
                    contentPadding = realPadding,
                ) {
                    items(list) {
                        GalleryInfoGridItem(
                            onClick = ::onItemClick.partially1(it),
                            onLongClick = { navigator.navigate(it.galleryInfo.asDst()) },
                            info = it,
                            modifier = Modifier.animateItemPlacement(),
                        )
                    }
                }
            } else {
                FastScrollLazyColumn(
                    modifier = Modifier.nestedScroll(searchBarConnection).fillMaxSize(),
                    contentPadding = realPadding,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.gallery_list_interval)),
                ) {
                    items(list, key = { it.gid }) { info ->
                        val checked = info.gid in checkedInfoMap
                        CheckableItem(checked = checked, modifier = Modifier.animateItemPlacement()) { interactionSource ->
                            DownloadCard(
                                onClick = {
                                    if (selectMode) {
                                        if (checked) {
                                            checkedInfoMap.remove(info.gid)
                                        } else {
                                            checkedInfoMap[info.gid] = info
                                        }
                                    } else {
                                        onItemClick(info)
                                    }
                                },
                                onThumbClick = {
                                    navigator.navigate(info.galleryInfo.asDst())
                                },
                                onLongClick = {
                                    checkedInfoMap[info.gid] = info
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
                                interactionSource = interactionSource,
                            )
                        }
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

    val hideFab by delegateSnapshotUpdate {
        record { fabHidden }
        transform {
            // Bug: IDE failed to inference 'hide's type
            onEachLatest { hide: Boolean ->
                if (!hide) delay(FAB_ANIMATE_TIME.toLong())
            }
        }
    }

    FabLayout(
        hidden = hideFab && !selectMode,
        expanded = fabExpanded || selectMode,
        onExpandChanged = {
            fabExpanded = it
            checkedInfoMap.clear()
        },
        autoCancel = !selectMode,
    ) {
        if (!selectMode) {
            onClick(Icons.Default.Shuffle) {
                context.navToReader(list.random().galleryInfo)
            }
            onClick(Icons.AutoMirrored.Default.Sort) {
                val selected = dialogState.showSingleChoice(
                    sortModes.toList(),
                    SortMode.All.indexOfFirst { it.flag == sortMode },
                    R.string.sort_by,
                )
                val mode = SortMode.All[selected]
                DownloadManager.sortDownloads(mode)
                sortMode = mode.flag
            }
            onClick(Icons.Default.FilterList) {
                filterType = dialogState.showSingleChoice(
                    downloadStates.toList(),
                    filterType + 1,
                    R.string.download_filter,
                ) - 1
            }
        } else {
            onClick(Icons.Default.DoneAll) {
                val info = list.associateBy { it.gid }
                checkedInfoMap.putAll(info)
                throw CancellationException()
            }
            onClick(Icons.Default.PlayArrow) {
                val gidList = checkedInfoMap.run { toMap().values.also { clear() } }
                    .mapToLongArray(DownloadInfo::gid)
                val intent = Intent(activity, DownloadService::class.java)
                intent.action = DownloadService.ACTION_START_RANGE
                intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
                ContextCompat.startForegroundService(context, intent)
            }
            onClick(Icons.Default.Pause) {
                val gidList = checkedInfoMap.run { toMap().values.also { clear() } }
                    .mapToLongArray(DownloadInfo::gid)
                DownloadManager.stopRangeDownload(gidList)
            }
            onClick(Icons.Default.Delete) {
                val infoList = checkedInfoMap.run { toMap().values.also { clear() } }
                dialogState.confirmRemoveDownloadRange(infoList)
                list.removeAll(infoList)
            }
            onClick(Icons.AutoMirrored.Default.DriveFileMove) {
                val infoList = checkedInfoMap.run { toMap().values.also { clear() } }
                val toLabel = dialogState.showMoveDownloadLabelList(infoList)
                if (label != "" && label != toLabel) {
                    list.removeAll(infoList)
                }
            }
        }
    }
}

object DownloadsFragment {
    const val KEY_GID = "gid"
    const val KEY_ACTION = "action"
    const val ACTION_CLEAR_DOWNLOAD_SERVICE = "clear_download_service"
}
