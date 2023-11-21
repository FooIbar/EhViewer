package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.SadAndroid
import com.hippo.ehviewer.icons.filled.GoTo
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.main.FabLayout
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.startDownload
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalStaggeredGrid
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.mapToLongArray
import com.ramcosta.composedestinations.annotation.Destination
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.pxToDp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

@Destination
@Composable
fun FavouritesScreen(navigator: NavController) {
    // Meta State
    var urlBuilder by rememberSaveable { mutableStateOf(FavListUrlBuilder(favCat = Settings.recentFavCat)) }
    var searchBarOffsetY by remember { mutableStateOf(0) }

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
    val dialogState = LocalDialogState.current
    val activity = remember(context) { context.findActivity<MainActivity>() }
    val coroutineScope = rememberCoroutineScope()
    val localFavCountFlow = rememberInVM { EhDB.localFavCount }
    val searchBarHint = stringResource(R.string.search_bar_hint, favCatName)

    fun switchFav(newCat: Int, keyword: String? = null) {
        urlBuilder = urlBuilder.copy(
            keyword = keyword,
            favCat = newCat,
            jumpTo = null,
        ).apply { setIndex(null, true) }
        Settings.recentFavCat = urlBuilder.favCat
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
                            switchFav(index - 2)
                            coroutineScope.launch { sheetState.close() }
                        },
                    )
                }
            }
        }
    }

    val searchFieldState = rememberTextFieldState()
    val data = rememberInVM(urlBuilder) {
        if (urlBuilder.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
            Pager(PagingConfig(20, jumpThreshold = 40)) {
                if (keyword.isBlank()) {
                    EhDB.localFavLazyList
                } else {
                    EhDB.searchLocalFav(keyword)
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
                        Settings.favCat = r.catArray
                        Settings.favCount = r.countArray
                        Settings.favCloudCount = r.countArray.sum()
                        urlBuilder.jumpTo = null
                        LoadResult.Page(r.galleryInfoList, r.prev, r.next)
                    }
                }
            }
        }.flow.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()

    val refreshState = rememberPullToRefreshState {
        data.loadState.refresh is LoadState.NotLoading
    }

    if (refreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            data.refresh()
        }
    }

    val searchBarConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                searchBarOffsetY = (searchBarOffsetY + consumed.y).roundToInt().coerceIn(-300, 0)
                return Offset.Zero // We never consume it
            }
        }
    }
    val combinedModifier = Modifier.nestedScroll(searchBarConnection).nestedScroll(refreshState.nestedScrollConnection)

    var expanded by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    var selectMode by remember { mutableStateOf(false) }
    val checkedInfoMap = remember { mutableStateMapOf<Long, BaseGalleryInfo>() }
    SideEffect {
        if (checkedInfoMap.isEmpty()) selectMode = false
        if (selectMode) expanded = true
    }

    SearchBarScreen(
        title = title,
        searchFieldState = searchFieldState,
        searchFieldHint = searchBarHint,
        onApplySearch = { switchFav(urlBuilder.favCat, it) },
        onSearchExpanded = { hidden = true },
        onSearchHidden = { hidden = false },
        refreshState = refreshState,
        searchBarOffsetY = searchBarOffsetY,
        trailingIcon = {
            IconButton(onClick = { activity.openSideSheet() }) {
                Icon(imageVector = Icons.Outlined.FolderSpecial, contentDescription = null)
            }
        },
    ) {
        val layoutDirection = LocalLayoutDirection.current
        val marginH = dimensionResource(id = R.dimen.gallery_list_margin_h)
        val marginV = dimensionResource(id = R.dimen.gallery_list_margin_v)
        val realPadding = PaddingValues(
            top = it.calculateTopPadding() + marginV,
            bottom = it.calculateBottomPadding() + marginV,
            start = it.calculateStartPadding(layoutDirection) + marginH,
            end = it.calculateEndPadding(layoutDirection) + marginH,
        )
        val listMode by remember {
            Settings.listModeBackField.valueFlow()
        }.collectAsState(Settings.listMode)
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = data.loadState.refresh) {
                is LoadState.Loading -> if (data.itemCount == 0) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LoadState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).widthIn(max = 228.dp)
                            .clip(ShapeDefaults.Small).clickable { data.retry() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = EhIcons.Big.Default.SadAndroid,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp).size(120.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = ExceptionUtils.getReadableString(state.error),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                is LoadState.NotLoading -> SideEffect {
                    refreshState.endRefresh()
                }
            }
            if (listMode == 0) {
                val height = (3 * Settings.listThumbSize * 3).pxToDp.dp
                val showPages = Settings.showGalleryPages
                FastScrollLazyColumn(
                    modifier = combinedModifier,
                    contentPadding = realPadding,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.gallery_list_interval)),
                ) {
                    items(
                        count = data.itemCount,
                        key = data.itemKey(key = { item -> item.gid }),
                        contentType = data.itemContentType(),
                    ) { index ->
                        val info = data[index]
                        if (info != null) {
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
                                            navigator.navAnimated(
                                                R.id.galleryDetailScene,
                                                bundleOf(GalleryDetailScene.KEY_ARGS to GalleryInfoArgs(info)),
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        selectMode = true
                                        checkedInfoMap[info.gid] = info
                                    },
                                    info = info,
                                    isInFavScene = true,
                                    showPages = showPages,
                                    modifier = Modifier.height(height),
                                )
                            }
                        }
                    }
                }
            } else {
                val gridInterval = dimensionResource(R.dimen.gallery_grid_interval)
                FastScrollLazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(Settings.thumbSizeDp.dp),
                    modifier = combinedModifier,
                    verticalItemSpacing = gridInterval,
                    horizontalArrangement = Arrangement.spacedBy(gridInterval),
                    contentPadding = realPadding,
                ) {
                    items(
                        count = data.itemCount,
                        key = data.itemKey(key = { item -> item.gid }),
                        contentType = data.itemContentType(),
                    ) { index ->
                        val info = data[index]
                        if (info != null) {
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
                                            navigator.navAnimated(
                                                R.id.galleryDetailScene,
                                                bundleOf(GalleryDetailScene.KEY_ARGS to GalleryInfoArgs(info)),
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        selectMode = true
                                        checkedInfoMap[info.gid] = info
                                    },
                                    info = info,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Explicitly reallocate fabBuilder lambda to recompose secondary fab
    val select = selectMode
    FabLayout(
        hidden = hidden,
        expanded = expanded,
        onExpandChanged = {
            expanded = it
            checkedInfoMap.clear()
            selectMode = false
        },
        autoCancel = !selectMode,
    ) {
        if (!select) {
            onClick(EhIcons.Default.GoTo) {
                val local = LocalDateTime.of(2007, 3, 21, 0, 0)
                val fromDate = local.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant().toEpochMilli()
                val toDate = MaterialDatePicker.todayInUtcMilliseconds()
                val listValidators = ArrayList<CalendarConstraints.DateValidator>()
                listValidators.add(DateValidatorPointForward.from(fromDate))
                listValidators.add(DateValidatorPointBackward.before(toDate))
                val constraintsBuilder = CalendarConstraints.Builder()
                    .setStart(fromDate)
                    .setEnd(toDate)
                    .setValidator(CompositeDateValidator.allOf(listValidators))
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setCalendarConstraints(constraintsBuilder.build())
                    .setTitleText(R.string.go_to)
                    .setSelection(toDate)
                    .build()
                datePicker.show(activity.supportFragmentManager, "date-picker")
                datePicker.addOnPositiveButtonClickListener { time: Long ->
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).withZone(ZoneOffset.UTC)
                    val jumpTo = formatter.format(Instant.ofEpochMilli(time))
                    urlBuilder = urlBuilder.copy(jumpTo = jumpTo)
                }
            }
            onClick(Icons.Default.Refresh) {
                switchFav(urlBuilder.favCat)
            }
            onClick(Icons.AutoMirrored.Default.LastPage) {
                urlBuilder = urlBuilder.copy().apply { setIndex("1-0", false) }
            }
        } else {
            onClick(Icons.Default.DoneAll) {
                val info = data.itemSnapshotList.items.associateBy { it.gid }
                checkedInfoMap.putAll(info)
                throw CancellationException()
            }
            onClick(Icons.Default.Download) {
                val info = checkedInfoMap.run { values.also { clear() } }
                dialogState.startDownload(context, false, *info.toTypedArray())
            }
            onClick(Icons.Default.Delete) {
                val info = checkedInfoMap.run { values.also { clear() } }
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
                val info = checkedInfoMap.run { values.also { clear() } }
                if (srcCat != dstCat) {
                    if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                        // Move from local to cloud
                        EhDB.removeLocalFavorites(info)
                        val galleryList = info.map { it.gid to it.token!! }
                        runSuspendCatching {
                            EhEngine.addFavorites(galleryList, dstCat)
                        }
                    } else if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                        // Move from cloud to local
                        EhDB.putLocalFavorites(info)
                    } else {
                        // Move from cloud to cloud
                        val gidArray = info.mapToLongArray(BaseGalleryInfo::gid)
                        runSuspendCatching {
                            EhEngine.modifyFavorites(gidArray, srcCat, dstCat)
                        }
                    }
                    data.refresh()
                }
            }
        }
    }
}

class FavoritesFragment : BaseScene() {
    override val enableDrawerGestures = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeWithMD3 {
            val navController = remember { findNavController() }
            FavouritesScreen(navController)
        }
    }
}
