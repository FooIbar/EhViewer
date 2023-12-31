package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.GoTo
import com.hippo.ehviewer.ui.LocalSideSheetState
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.destinations.GalleryDetailScreenDestination
import com.hippo.ehviewer.ui.main.FAB_ANIMATE_TIME
import com.hippo.ehviewer.ui.main.FabLayout
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.main.GalleryList
import com.hippo.ehviewer.ui.showDatePicker
import com.hippo.ehviewer.ui.startDownload
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.mapToLongArray
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

@Destination
@Composable
fun FavouritesScreen(navigator: DestinationsNavigator) {
    // Meta State
    var urlBuilder by rememberSaveable { mutableStateOf(FavListUrlBuilder(favCat = Settings.recentFavCat)) }
    var searchBarOffsetY by remember { mutableIntStateOf(0) }

    // Derived State
    val keyword = remember(urlBuilder) { urlBuilder.keyword.orEmpty() }
    val localFavName = stringResource(R.string.local_favorites)
    val cloudFavName = stringResource(R.string.cloud_favorites)
    val favCatName = remember(urlBuilder) {
        when (val favCat = urlBuilder.favCat) {
            in 0..9 -> Settings.favCat[favCat]
            FavListUrlBuilder.FAV_CAT_LOCAL -> localFavName
            else -> cloudFavName
        }
    }
    val favTitle = stringResource(R.string.favorites_title, favCatName)
    val favTitleWithKeyword = stringResource(R.string.favorites_title_2, favCatName, keyword)
    val title = remember(urlBuilder) { if (keyword.isBlank()) favTitle else favTitleWithKeyword }
    val context = LocalContext.current
    val density = LocalDensity.current
    val dialogState = LocalDialogState.current
    val activity = remember(context) { context.findActivity<MainActivity>() }
    val coroutineScope = rememberCoroutineScope()
    val localFavCountFlow = rememberInVM { EhDB.localFavCount }
    val searchBarHint = stringResource(R.string.search_bar_hint, favCatName)
    val data = rememberInVM(urlBuilder.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
        if (urlBuilder.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
            Pager(PagingConfig(20, jumpThreshold = 40)) {
                val keywordNow = urlBuilder.keyword.orEmpty()
                if (keywordNow.isBlank()) {
                    EhDB.localFavLazyList
                } else {
                    EhDB.searchLocalFav(keywordNow)
                }
            }
        } else {
            Pager(PagingConfig(25)) {
                object : PagingSource<String, BaseGalleryInfo>() {
                    override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
                    override suspend fun load(params: LoadParams<String>) = withIOContext {
                        when (params) {
                            is LoadParams.Prepend -> urlBuilder.setIndex(params.key, isNext = false)
                            is LoadParams.Append -> urlBuilder.setIndex(params.key, isNext = true)
                            is LoadParams.Refresh -> {
                                val key = params.key
                                if (key.isNullOrBlank()) {
                                    if (urlBuilder.jumpTo != null) {
                                        urlBuilder.next ?: urlBuilder.setIndex("2", true)
                                    }
                                } else {
                                    urlBuilder.setIndex(key, false)
                                }
                            }
                        }
                        val r = runSuspendCatching {
                            EhEngine.getFavorites(urlBuilder.build())
                        }.onFailure {
                            return@withIOContext LoadResult.Error(it)
                        }.getOrThrow()
                        Settings.favCat = r.catArray.toTypedArray()
                        Settings.favCount = r.countArray.toIntArray()
                        Settings.favCloudCount = r.countArray.sum()
                        urlBuilder.jumpTo = null
                        LoadResult.Page(r.galleryInfoList, r.prev, r.next)
                    }
                }
            }
        }.flow.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()

    fun refresh(newUrlBuilder: FavListUrlBuilder = urlBuilder.copy(jumpTo = null, prev = null, next = null)) {
        urlBuilder = newUrlBuilder
        data.refresh()
    }

    with(activity) {
        ProvideSideSheetContent { sheetState ->
            val localFavCount by localFavCountFlow.collectAsState(0)
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.collections)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
            val scope = currentRecomposeScope
            LaunchedEffect(Unit) {
                Settings.favChangesFlow.collect {
                    scope.invalidate()
                }
            }
            val localFav = stringResource(id = R.string.local_favorites) to localFavCount
            val faves = if (EhCookieStore.hasSignedIn()) {
                arrayOf(
                    localFav,
                    stringResource(id = R.string.cloud_favorites) to Settings.favCloudCount,
                    *Settings.favCat.zip(Settings.favCount.toTypedArray()).toTypedArray(),
                )
            } else {
                arrayOf(localFav)
            }
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
            ) {
                faves.forEachIndexed { index, (name, count) ->
                    ListItem(
                        headlineContent = { Text(text = name) },
                        trailingContent = { Text(text = count.toString(), style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier.clickable {
                            val newCat = index - 2
                            refresh(FavListUrlBuilder(newCat))
                            Settings.recentFavCat = newCat
                            coroutineScope.launch { sheetState.close() }
                        },
                    )
                }
            }
        }
    }

    val searchFieldState = rememberTextFieldState()

    val refreshState = rememberPullToRefreshState {
        data.loadState.refresh is LoadState.NotLoading
    }

    var expanded by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    val checkedInfoMap = remember { mutableStateMapOf<Long, BaseGalleryInfo>() }
    val selectMode = checkedInfoMap.isNotEmpty()
    LockDrawer(selectMode)

    SearchBarScreen(
        title = title,
        searchFieldState = searchFieldState,
        searchFieldHint = searchBarHint,
        onApplySearch = { refresh(FavListUrlBuilder(urlBuilder.favCat, it)) },
        onSearchExpanded = {
            checkedInfoMap.clear()
            hidden = true
        },
        onSearchHidden = { hidden = false },
        refreshState = refreshState,
        searchBarOffsetY = { searchBarOffsetY },
        trailingIcon = {
            val sheetState = LocalSideSheetState.current
            IconButton(onClick = { coroutineScope.launch { sheetState.open() } }) {
                Icon(imageVector = Icons.Outlined.FolderSpecial, contentDescription = null)
            }
        },
    ) { contentPadding ->
        val listMode by Settings.listMode.collectAsState()
        val height by collectListThumbSizeAsState()
        val showPages = Settings.showGalleryPages
        val searchBarConnection = remember {
            val topPaddingPx = with(density) { contentPadding.calculateTopPadding().roundToPx() }
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    searchBarOffsetY = (searchBarOffsetY + consumed.y).roundToInt().coerceIn(-topPaddingPx, 0)
                    return Offset.Zero // We never consume it
                }
            }
        }
        GalleryList(
            data = data,
            contentModifier = Modifier.nestedScroll(searchBarConnection),
            contentPadding = contentPadding,
            listMode = listMode,
            detailItemContent = { info ->
                val checked = info.gid in checkedInfoMap
                CheckableItem(checked = checked) {
                    GalleryInfoListItem(
                        onClick = {
                            if (selectMode) {
                                if (checked) {
                                    checkedInfoMap.remove(info.gid)
                                } else {
                                    checkedInfoMap[info.gid] = info
                                }
                            } else {
                                navigator.navigate(GalleryDetailScreenDestination(GalleryInfoArgs(info)))
                            }
                        },
                        onLongClick = {
                            checkedInfoMap[info.gid] = info
                        },
                        info = info,
                        isInFavScene = true,
                        showPages = showPages,
                        modifier = Modifier.height(height),
                    )
                }
            },
            thumbItemContent = { info ->
                val checked = info.gid in checkedInfoMap
                CheckableItem(checked = checked) {
                    GalleryInfoGridItem(
                        onClick = {
                            if (selectMode) {
                                if (checked) {
                                    checkedInfoMap.remove(info.gid)
                                } else {
                                    checkedInfoMap[info.gid] = info
                                }
                            } else {
                                navigator.navigate(GalleryDetailScreenDestination(GalleryInfoArgs(info)))
                            }
                        },
                        onLongClick = {
                            checkedInfoMap[info.gid] = info
                        },
                        info = info,
                    )
                }
            },
            refreshState = refreshState,
            scrollToTopOnRefresh = urlBuilder.favCat != FavListUrlBuilder.FAV_CAT_LOCAL,
            onRefresh = { refresh() },
            onLoading = { searchBarOffsetY = 0 },
        )
    }

    val hideFab by remember {
        snapshotFlow {
            hidden
        }.onEach {
            if (!it) delay(FAB_ANIMATE_TIME.toLong())
        }.mapLatest { it }
    }.collectAsState(hidden)

    FabLayout(
        hidden = hideFab,
        expanded = expanded || selectMode,
        onExpandChanged = {
            expanded = it
            checkedInfoMap.clear()
        },
        autoCancel = !selectMode,
    ) {
        if (!selectMode) {
            onClick(EhIcons.Default.GoTo) {
                coroutineScope.launch {
                    val date = dialogState.showDatePicker()
                    refresh(urlBuilder.copy(jumpTo = date))
                }
            }
            onClick(Icons.Default.Refresh) {
                refresh()
            }
            onClick(Icons.AutoMirrored.Default.LastPage) {
                refresh(urlBuilder.copy(jumpTo = null, prev = "1-0", next = null))
            }
        } else {
            onClick(Icons.Default.DoneAll) {
                val info = data.itemSnapshotList.items.associateBy { it.gid }
                checkedInfoMap.putAll(info)
                throw CancellationException()
            }
            onClick(Icons.Default.Download) {
                val info = checkedInfoMap.run { toMap().values.also { clear() } }
                dialogState.startDownload(context, false, *info.toTypedArray())
            }
            onClick(Icons.Default.Delete) {
                val info = checkedInfoMap.run { toMap().values.also { clear() } }
                dialogState.awaitPermissionOrCancel(title = R.string.delete_favorites_dialog_title) {
                    Text(text = stringResource(R.string.delete_favorites_dialog_message, info.size))
                }
                val srcCat = urlBuilder.favCat
                if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Delete local fav
                    EhDB.removeLocalFavorites(info)
                } else {
                    val delList = info.mapToLongArray(BaseGalleryInfo::gid)
                    EhEngine.modifyFavorites(delList, srcCat, -1)
                }
                data.refresh()
            }
            onClick(Icons.AutoMirrored.Default.DriveFileMove) {
                // First is local favorite, the other 10 is cloud favorite
                val array = if (EhCookieStore.hasSignedIn()) {
                    arrayOf(localFavName, *Settings.favCat)
                } else {
                    arrayOf(localFavName)
                }
                val index = dialogState.showSelectItem(*array, title = R.string.move_favorites_dialog_title)
                val srcCat = urlBuilder.favCat
                val dstCat = if (index == 0) FavListUrlBuilder.FAV_CAT_LOCAL else index - 1
                val info = checkedInfoMap.run { toMap().values.also { clear() } }
                if (srcCat != dstCat) {
                    runSuspendCatching {
                        if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                            // Move from local to cloud
                            val galleryList = info.map { it.gid to it.token!! }
                            EhEngine.addFavorites(galleryList, dstCat)
                            EhDB.removeLocalFavorites(info)
                        } else if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                            // Move from cloud to local
                            EhDB.putLocalFavorites(info)
                        } else {
                            // Move from cloud to cloud
                            val gidArray = info.mapToLongArray(BaseGalleryInfo::gid)
                            EhEngine.modifyFavorites(gidArray, srcCat, dstCat)
                        }
                    }.onFailure {
                        activity.showTip(ExceptionUtils.getReadableString(it))
                    }
                    data.refresh()
                }
            }
        }
    }
}
