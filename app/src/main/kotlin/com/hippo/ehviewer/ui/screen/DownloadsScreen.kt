package com.hippo.ehviewer.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.fork.SwipeToDismissBox
import androidx.compose.material3.fork.SwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import arrow.core.partially1
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.data.TagNamespace
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadManager.downloadInfoList
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.DownloadsFilterMode
import com.hippo.ehviewer.download.SortMode
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.Download
import com.hippo.ehviewer.ui.DrawerHandle
import com.hippo.ehviewer.ui.LocalSideSheetState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.confirmRemoveDownloadRange
import com.hippo.ehviewer.ui.main.DownloadCard
import com.hippo.ehviewer.ui.main.FAB_ANIMATE_TIME
import com.hippo.ehviewer.ui.main.FabLayout
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.plus
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.showMoveDownloadLabelList
import com.hippo.ehviewer.ui.tools.Await
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.EmptyWindowInsets
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalStaggeredGrid
import com.hippo.ehviewer.ui.tools.HapticFeedbackType
import com.hippo.ehviewer.ui.tools.asyncState
import com.hippo.ehviewer.ui.tools.rememberHapticFeedback
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.ui.tools.thenIf
import com.hippo.ehviewer.util.mapToLongArray
import com.hippo.ehviewer.util.takeAndClear
import com.jamal.composeprefs3.ui.ifTrueThen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.onEachLatest
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.DownloadsScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    var gridView by Settings.gridView.asMutableState()
    var sortMode by Settings.downloadSortMode.asMutableState()
    val filterMode by Settings.downloadFilterMode.collectAsState { DownloadsFilterMode.from(it) }
    var filterState by rememberSaveable { mutableStateOf(DownloadsFilterState(filterMode, Settings.recentDownloadLabel.value)) }
    var invalidateKey by rememberSaveable { mutableStateOf(false) }
    var searchBarExpanded by rememberSaveable { mutableStateOf(false) }
    var searchBarOffsetY by remember { mutableIntStateOf(0) }
    val animateItems by Settings.animateItems.collectAsState()

    var fabExpanded by remember { mutableStateOf(false) }
    var fabHidden by remember { mutableStateOf(false) }
    val checkedInfoMap = remember { mutableStateMapOf<Long, DownloadInfo>() }
    val selectMode by rememberUpdatedState(checkedInfoMap.isNotEmpty())
    DrawerHandle(!selectMode && !searchBarExpanded)

    val density = LocalDensity.current
    val canTranslate = Settings.showTagTranslations && EhTagDatabase.isTranslatable(implicit<Context>()) && EhTagDatabase.initialized
    val ehTags = EhTagDatabase.takeIf { canTranslate }
    fun getTranslation(tag: String) = ehTags?.run {
        getTranslation(TagNamespace.Artist.prefix, tag) ?: getTranslation(TagNamespace.Cosplayer.prefix, tag)
    } ?: tag
    val allName = stringResource(R.string.download_all)
    val defaultName = stringResource(R.string.default_download_label_name)
    val unknownName = stringResource(R.string.unknown_artists)
    val emptyLabelName = when (filterMode) {
        DownloadsFilterMode.ARTIST -> unknownName
        DownloadsFilterMode.CUSTOM -> defaultName
    }
    val title = stringResource(
        R.string.scene_download_title,
        with(filterState) {
            when (label) {
                "" -> allName
                null -> emptyLabelName
                else -> if (mode == DownloadsFilterMode.ARTIST) getTranslation(label) else label
            }
        },
    )
    val hint = stringResource(R.string.search_bar_hint, title)
    val list = remember(filterState, invalidateKey) {
        downloadInfoList.filterTo(mutableStateListOf()) { info ->
            filterState.take(info)
        }
    }

    val newLabel = stringResource(R.string.new_label_title)
    val renameLabel = stringResource(R.string.rename_label_title)
    val labelsStr = stringResource(R.string.download_labels)
    val labelEmpty = stringResource(R.string.label_text_is_empty)
    val defaultInvalid = stringResource(R.string.label_text_is_invalid)
    val labelExists = stringResource(R.string.label_text_exist)
    val downloadsCountGroupByArtist by rememberInVM { EhDB.downloadsCountByArtist }.collectAsState(emptyMap())
    val downloadsCountGroupByLabel by rememberInVM { EhDB.downloadsCountByLabel }.collectAsState(emptyMap())
    val downloadsCount = when (filterMode) {
        DownloadsFilterMode.CUSTOM -> downloadsCountGroupByLabel
        DownloadsFilterMode.ARTIST -> downloadsCountGroupByArtist
    }
    val artistList = remember(downloadsCountGroupByArtist) {
        downloadsCountGroupByArtist.keys.mapNotNull { artist -> artist?.let { it to it } }
    }
    val labelList by remember {
        derivedStateOf {
            DownloadManager.labelList.map { it.id!! to it.label }
        }
    }
    val groupList = when (filterMode) {
        DownloadsFilterMode.CUSTOM -> labelList
        DownloadsFilterMode.ARTIST -> artistList
    }
    val totalCount = remember(downloadsCountGroupByLabel) { downloadsCountGroupByLabel.values.sum() }

    fun switchLabel(label: String?) {
        Settings.recentDownloadLabel.value = label
        filterState = filterState.copy(label = label)
    }

    LaunchedEffect(filterState) {
        searchBarOffsetY = 0
    }

    ProvideSideSheetContent { drawerState ->
        fun closeSheet() = launch { drawerState.close() }
        TopAppBar(
            title = { Text(text = labelsStr) },
            windowInsets = EmptyWindowInsets,
            colors = topBarOnDrawerColor(),
            actions = {
                if (DownloadsFilterMode.CUSTOM == filterMode) {
                    IconButton(
                        onClick = {
                            launch {
                                val text = awaitInputText(title = newLabel, hint = labelsStr) { text ->
                                    when {
                                        text.isBlank() -> raise(labelEmpty)
                                        text == defaultName -> raise(defaultInvalid)
                                        DownloadManager.containLabel(text) -> raise(labelExists)
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
                            launch {
                                val selected = if (!Settings.hasDefaultDownloadLabel) {
                                    0
                                } else {
                                    DownloadManager.labelList.indexOfFirst { it.label == Settings.defaultDownloadLabel } + 2
                                }
                                awaitSelectAction(R.string.default_download_label, selected) {
                                    onSelect(letMeSelect) {
                                        Settings.hasDefaultDownloadLabel = false
                                    }
                                    onSelect(defaultName) {
                                        Settings.hasDefaultDownloadLabel = true
                                        Settings.defaultDownloadLabel = null
                                    }
                                    DownloadManager.labelList.forEach { (label) ->
                                        onSelect(label) {
                                            Settings.hasDefaultDownloadLabel = true
                                            Settings.defaultDownloadLabel = label
                                        }
                                    }
                                }()
                            }
                        },
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    }
                }
                val custom = stringResource(R.string.select_grouping_mode_custom)
                val artist = stringResource(R.string.select_grouping_mode_artist)
                IconButton(
                    onClick = {
                        launch {
                            awaitSelectAction(R.string.select_grouping_mode) {
                                val select = { mode: DownloadsFilterMode ->
                                    filterState = filterState.copy(mode = mode, label = "")
                                    Settings.downloadFilterMode.value = mode.flag
                                    Settings.recentDownloadLabel.value = ""
                                }
                                onSelect(custom) { select(DownloadsFilterMode.CUSTOM) }
                                onSelect(artist) { select(DownloadsFilterMode.ARTIST) }
                            }()
                        }
                    },
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                }
            },
        )

        val dialogState by rememberUpdatedState(implicit<DialogState>())
        val labelsListState = rememberLazyListState()
        val editEnable = DownloadsFilterMode.CUSTOM == filterMode
        val hapticFeedback = rememberHapticFeedback()
        val reorderableLabelState = rememberReorderableLazyListState(labelsListState) { from, to ->
            val fromPosition = from.index - 2
            val toPosition = to.index - 2
            DownloadManager.labelList.apply { add(toPosition, removeAt(fromPosition)) }
            hapticFeedback.performHapticFeedback(HapticFeedbackType.MOVE)
        }
        var fromIndex by remember { mutableIntStateOf(-1) }
        FastScrollLazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            state = labelsListState,
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
        ) {
            item {
                ListItem(
                    modifier = Modifier.clip(CardDefaults.shape).clickable {
                        switchLabel("")
                        closeSheet()
                    },
                    shadowElevation = 1.dp,
                    headlineContent = {
                        Text("$allName [$totalCount]")
                    },
                    colors = listItemOnDrawerColor(filterState.label == ""),
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clip(CardDefaults.shape).clickable {
                        switchLabel(null)
                        closeSheet()
                    },
                    shadowElevation = 1.dp,
                    headlineContent = {
                        Text("$emptyLabelName [${downloadsCount.getOrDefault(null, 0)}]")
                    },
                    colors = listItemOnDrawerColor(filterState.label == null),
                )
            }

            itemsIndexed(groupList, key = { _, (id) -> id }) { itemIndex, (id, label) ->
                val index by rememberUpdatedState(itemIndex)
                val item by rememberUpdatedState(label)
                // Not using rememberSwipeToDismissBoxState to prevent LazyColumn from reusing it
                // SQLite may reuse ROWIDs from previously deleted rows so they'll have the same key
                val dismissState = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, density) }
                LaunchedEffect(dismissState) {
                    snapshotFlow { dismissState.currentValue }.collect {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            runCatching {
                                dialogState.awaitConfirmationOrCancel(confirmText = R.string.delete) {
                                    Text(text = stringResource(R.string.delete_label, item))
                                }
                            }.onSuccess {
                                DownloadManager.deleteLabel(item)
                                when (filterState.label) {
                                    item -> switchLabel("")
                                    null -> invalidateKey = !invalidateKey
                                }
                            }.onFailure {
                                dismissState.reset()
                            }
                        }
                    }
                }
                ReorderableItem(
                    reorderableLabelState,
                    id,
                    enabled = editEnable,
                    animateItemModifier = Modifier.thenIf(animateItems) { animateItem() },
                ) { isDragging ->
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                        enableDismissFromStartToEnd = false,
                        gesturesEnabled = editEnable,
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
                            modifier = Modifier.clip(CardDefaults.shape).clickable {
                                switchLabel(item)
                                closeSheet()
                            },
                            shadowElevation = elevation,
                            headlineContent = {
                                val name = if (filterMode == DownloadsFilterMode.ARTIST) getTranslation(label) else label
                                Text("$name [${downloadsCount.getOrDefault(item, 0)}]")
                            },
                            trailingContent = editEnable.ifTrueThen {
                                Row {
                                    IconButton(
                                        onClick = {
                                            launch {
                                                val new = awaitInputText(initial = item, title = renameLabel, hint = labelsStr) { text ->
                                                    when {
                                                        text.isBlank() -> raise(labelEmpty)
                                                        text == defaultName -> raise(defaultInvalid)
                                                        DownloadManager.containLabel(text) -> raise(labelExists)
                                                    }
                                                }
                                                DownloadManager.renameLabel(item, new)
                                                if (filterState.label == item) {
                                                    switchLabel(new)
                                                }
                                            }
                                        },
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                    }
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier.draggableHandle(
                                            onDragStarted = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.START)
                                                fromIndex = index
                                            },
                                            onDragStopped = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.END)
                                                if (fromIndex != -1) {
                                                    if (fromIndex != index) {
                                                        val range = if (fromIndex < index) fromIndex..index else index..fromIndex
                                                        val toUpdate = DownloadManager.labelList.slice(range)
                                                        toUpdate.zip(range).forEach { it.first.position = it.second }
                                                        launchIO { EhDB.updateDownloadLabel(toUpdate) }
                                                    }
                                                    fromIndex = -1
                                                }
                                            },
                                        ),
                                    ) {
                                        Icon(imageVector = Icons.Default.Reorder, contentDescription = null)
                                    }
                                }
                            },
                            colors = listItemOnDrawerColor(filterState.label == item),
                        )
                    }
                }
            }
        }
    }

    SearchBarScreen(
        onApplySearch = { filterState = filterState.copy(keyword = it) },
        expanded = searchBarExpanded,
        onExpandedChange = {
            searchBarExpanded = it
            fabHidden = it
            if (it) checkedInfoMap.clear()
        },
        title = title,
        searchFieldHint = hint,
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
                        launch { sideSheetState.open() }
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.download_start_all)) },
                    onClick = {
                        expanded = false
                        val intent = Intent(implicit<Activity>(), DownloadService::class.java)
                        intent.action = DownloadService.ACTION_START_ALL
                        ContextCompat.startForegroundService(implicit<Activity>(), intent)
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.download_stop_all)) },
                    onClick = {
                        expanded = false
                        launchIO { DownloadManager.stopAllDownload() }
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.download_reset_reading_progress)) },
                    onClick = {
                        expanded = false
                        launchIO {
                            awaitConfirmationOrCancel(
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
                        val intent = Intent(implicit<Activity>(), DownloadService::class.java)
                        intent.action = DownloadService.ACTION_START_RANGE
                        intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
                        ContextCompat.startForegroundService(implicit<Activity>(), intent)
                    },
                )
            }
        },
    ) { contentPadding ->
        val height by collectListThumbSizeAsState()
        val realPadding = contentPadding + PaddingValues(dimensionResource(id = R.dimen.gallery_list_margin_h), dimensionResource(id = R.dimen.gallery_list_margin_v))
        val searchBarConnection = remember {
            val slop = ViewConfiguration.get(implicit<Context>()).scaledTouchSlop
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
            launchIO { EhDB.putHistoryInfo(info.galleryInfo) }
            navToReader(info.galleryInfo)
        }

        Crossfade(targetState = gridView, label = "Downloads") { showGridView ->
            if (showGridView) {
                val gridInterval = dimensionResource(R.dimen.gallery_grid_interval)
                val thumbColumns by Settings.thumbColumns.collectAsState()
                FastScrollLazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(thumbColumns),
                    modifier = Modifier.nestedScroll(searchBarConnection).fillMaxSize(),
                    contentPadding = realPadding,
                    verticalItemSpacing = gridInterval,
                    horizontalArrangement = Arrangement.spacedBy(gridInterval),
                ) {
                    items(list, key = { it.gid }) { info ->
                        GalleryInfoGridItem(
                            onClick = ::onItemClick.partially1(info),
                            onLongClick = { navigate(info.galleryInfo.asDst()) },
                            info = info,
                            modifier = Modifier.thenIf(animateItems) { animateItem() },
                            showLanguage = false,
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
                        CheckableItem(
                            checked = checked,
                            modifier = Modifier.thenIf(animateItems) { animateItem() },
                        ) { interactionSource ->
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
                                    navigate(info.galleryInfo.asDst())
                                },
                                onLongClick = {
                                    checkedInfoMap[info.gid] = info
                                },
                                onStart = {
                                    val intent = Intent(implicit<Activity>(), DownloadService::class.java)
                                    intent.action = DownloadService.ACTION_START
                                    intent.putExtra(DownloadService.KEY_GALLERY_INFO, info.galleryInfo)
                                    ContextCompat.startForegroundService(implicit<Activity>(), intent)
                                },
                                onStop = { launchIO { DownloadManager.stopDownload(info.gid) } },
                                info = info,
                                selectMode = selectMode,
                                modifier = Modifier.height(height),
                                interactionSource = interactionSource,
                            )
                        }
                    }
                }
            }
        }

        Await({ delay(200) }) {
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

    val hideFab by asyncState(
        produce = { fabHidden },
        transform = {
            onEachLatest { hide ->
                if (!hide) delay(FAB_ANIMATE_TIME.toLong())
            }
        },
    )

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
                if (list.isNotEmpty()) {
                    withUIContext { navToReader(list.random().galleryInfo) }
                }
            }
            onClick(Icons.AutoMirrored.Default.Sort) {
                val oldMode = SortMode.from(sortMode)
                val sortModes = resources.getStringArray(R.array.download_sort_modes).toList()
                val (selected, checked) = awaitSelectItemWithCheckBox(
                    sortModes,
                    R.string.sort_by,
                    R.string.group_by_download_label,
                    SortMode.All.indexOfFirst { it.field == oldMode.field && it.order == oldMode.order },
                    oldMode.groupByDownloadLabel,
                )
                val mode = SortMode.All[selected].copy(groupByDownloadLabel = checked)
                DownloadManager.sortDownloads(mode)
                sortMode = mode.flag
                invalidateKey = !invalidateKey
            }
            onClick(Icons.Default.FilterList) {
                val downloadStates = resources.getStringArray(R.array.download_state).toList()
                val state = awaitSingleChoice(
                    downloadStates,
                    filterState.state + 1,
                    R.string.download_filter,
                ) - 1
                filterState = filterState.copy(state = state)
            }
        } else {
            onClick(Icons.Default.DoneAll, autoClose = false) {
                val info = list.associateBy { it.gid }
                checkedInfoMap.putAll(info)
            }
            onClick(Icons.Default.PlayArrow) {
                val gidList = checkedInfoMap.takeAndClear().mapToLongArray(DownloadInfo::gid)
                val intent = Intent(implicit<Activity>(), DownloadService::class.java)
                intent.action = DownloadService.ACTION_START_RANGE
                intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
                ContextCompat.startForegroundService(implicit<Context>(), intent)
            }
            onClick(Icons.Default.Pause) {
                val gidList = checkedInfoMap.takeAndClear().mapToLongArray(DownloadInfo::gid)
                DownloadManager.stopRangeDownload(gidList)
            }
            onClick(Icons.Default.Delete) {
                val infoList = checkedInfoMap.takeAndClear()
                confirmRemoveDownloadRange(infoList)
                list.removeAll(infoList)
            }
            onClick(Icons.AutoMirrored.Default.DriveFileMove) {
                val infoList = checkedInfoMap.takeAndClear()
                val toLabel = showMoveDownloadLabelList(infoList)
                with(filterState) {
                    if (label != "" && label != toLabel) {
                        list.removeAll(infoList)
                    }
                }
            }
        }
    }
}
