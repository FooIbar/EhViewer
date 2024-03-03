package com.hippo.ehviewer.ui.screen

import android.net.Uri
import android.view.ViewConfiguration
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.lerp
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
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_IMAGE_SEARCH
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_NORMAL
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_SUBSCRIPTION
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TAG
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TOPLIST
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_UPLOADER
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_WHATS_HOT
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.GoTo
import com.hippo.ehviewer.ui.LocalSideSheetState
import com.hippo.ehviewer.ui.composing
import com.hippo.ehviewer.ui.destinations.ProgressScreenDestination
import com.hippo.ehviewer.ui.doGalleryInfoAction
import com.hippo.ehviewer.ui.main.AdvancedSearchOption
import com.hippo.ehviewer.ui.main.FAB_ANIMATE_TIME
import com.hippo.ehviewer.ui.main.FabLayout
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.main.GalleryList
import com.hippo.ehviewer.ui.main.ImageSearch
import com.hippo.ehviewer.ui.main.SearchFilter
import com.hippo.ehviewer.ui.showDatePicker
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.DragHandle
import com.hippo.ehviewer.ui.tools.SwipeToDismissBox2
import com.hippo.ehviewer.ui.tools.animateFloatMergePredictiveBackAsState
import com.hippo.ehviewer.ui.tools.delegateSnapshotUpdate
import com.hippo.ehviewer.ui.tools.draggingHapticFeedback
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.ui.tools.snackBarPadding
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.pickVisualMedia
import com.hippo.unifile.asUniFile
import com.hippo.unifile.sha1
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.onEachLatest
import moe.tarsin.coroutines.runSuspendCatching
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

@Destination
@Composable
fun HomePageScreen(navigator: DestinationsNavigator) = GalleryListScreen(ListUrlBuilder(), navigator)

@Destination
@Composable
fun SubscriptionScreen(navigator: DestinationsNavigator) = GalleryListScreen(ListUrlBuilder(MODE_SUBSCRIPTION), navigator)

@Destination
@Composable
fun WhatshotScreen(navigator: DestinationsNavigator) = GalleryListScreen(ListUrlBuilder(MODE_WHATS_HOT), navigator)

@Destination
@Composable
fun ToplistScreen(navigator: DestinationsNavigator) = GalleryListScreen(ListUrlBuilder(MODE_TOPLIST, mKeyword = Settings.recentToplist), navigator)

@Destination
@Composable
fun GalleryListScreen(lub: ListUrlBuilder, navigator: DestinationsNavigator) = composing(navigator) {
    val searchFieldState = rememberTextFieldState()
    var urlBuilder by rememberSaveable(lub) { mutableStateOf(lub) }
    var searchBarOffsetY by remember { mutableIntStateOf(0) }
    var showSearchLayout by rememberSaveable { mutableStateOf(false) }

    var category by rememberSaveable { mutableIntStateOf(Settings.searchCategory) }
    var searchMethod by rememberSaveable { mutableIntStateOf(1) }
    var advancedSearchOption by rememberSaveable { mutableStateOf(AdvancedSearchOption()) }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    LaunchedEffect(urlBuilder) {
        if (urlBuilder.mode == MODE_SUBSCRIPTION) searchMethod = 2
        if (urlBuilder.category != EhUtils.NONE) category = urlBuilder.category
        if (urlBuilder.mode != MODE_TOPLIST) {
            var keyword = urlBuilder.keyword.orEmpty()
            if (urlBuilder.mode == MODE_TAG) {
                keyword = wrapTagKeyword(keyword)
            }
            if (keyword.isNotBlank()) {
                searchFieldState.setTextAndPlaceCursorAtEnd(keyword)
            }
        }
    }

    val animatedSearchLayout by animateFloatMergePredictiveBackAsState(
        enable = showSearchLayout,
        animationSpec = tween(FAB_ANIMATE_TIME * 2),
        onBack = { showSearchLayout = false },
    )
    val density = LocalDensity.current
    val listState = rememberLazyGridState()
    val gridState = rememberLazyStaggeredGridState()
    val isTopList = remember(urlBuilder) { urlBuilder.mode == MODE_TOPLIST }
    val ehHint = stringResource(R.string.gallery_list_search_bar_hint_e_hentai)
    val exHint = stringResource(R.string.gallery_list_search_bar_hint_exhentai)
    val searchBarHint by rememberUpdatedState(if (EhUtils.isExHentai) exHint else ehHint)
    val suitableTitle = getSuitableTitleForUrlBuilder(urlBuilder)
    val data = rememberInVM {
        Pager(PagingConfig(25)) {
            object : PagingSource<String, BaseGalleryInfo>() {
                override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
                override suspend fun load(params: LoadParams<String>) = withIOContext {
                    if (urlBuilder.mode == MODE_TOPLIST) {
                        // TODO: Since we know total pages, let pager support jump
                        val key = (params.key ?: urlBuilder.mJumpTo)?.toInt() ?: 0
                        val prev = (key - 1).takeIf { it > 0 }
                        val next = (key + 1).takeIf { it < TOPLIST_PAGES }
                        runSuspendCatching {
                            urlBuilder.setJumpTo(key)
                            EhEngine.getGalleryList(urlBuilder.build())
                        }.foldToLoadResult { result ->
                            LoadResult.Page(result.galleryInfoList, prev?.toString(), next?.toString())
                        }
                    } else {
                        when (params) {
                            is LoadParams.Prepend -> urlBuilder.setIndex(params.key, isNext = false)
                            is LoadParams.Append -> urlBuilder.setIndex(params.key, isNext = true)
                            is LoadParams.Refresh -> {
                                val key = params.key
                                if (key.isNullOrBlank()) {
                                    if (urlBuilder.mJumpTo != null) {
                                        urlBuilder.mNext ?: urlBuilder.setIndex("2", true)
                                    }
                                } else {
                                    urlBuilder.setIndex(key, false)
                                }
                            }
                        }
                        runSuspendCatching {
                            val url = urlBuilder.build()
                            EhEngine.getGalleryList(url)
                        }.foldToLoadResult { result ->
                            urlBuilder.mJumpTo = null
                            LoadResult.Page(result.galleryInfoList, result.prev, result.next)
                        }
                    }
                }
            }
        }.flow.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()
    FavouriteStatusRouter.Observe(data)
    val listMode by Settings.listMode.collectAsState()

    val quickSearchList = remember { mutableStateListOf<QuickSearch>() }
    val entries = stringArrayResource(id = R.array.toplist_entries)
    val values = stringArrayResource(id = R.array.toplist_values)
    val toplists = remember { entries zip values }
    val quickSearchName = getSuitableTitleForUrlBuilder(urlBuilder, false)
    var saveProgress by Settings.qSSaveProgress.asMutableState()

    fun getFirstVisibleItemIndex() = if (listMode == 0) {
        listState.firstVisibleItemIndex
    } else {
        gridState.firstVisibleItemIndex
    }

    LaunchedEffect(Unit) {
        val list = EhDB.getAllQuickSearch()
        quickSearchList.addAll(list)
    }

    if (isTopList) {
        ProvideSideSheetContent { sheetState ->
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.toplist)) },
                windowInsets = WindowInsets(0),
            )
            toplists.forEach { (name, keyword) ->
                ListItem(
                    modifier = Modifier.clickable {
                        Settings.recentToplist = keyword
                        urlBuilder = ListUrlBuilder(MODE_TOPLIST, mKeyword = keyword)
                        data.refresh()
                        showSearchLayout = false
                        launch { sheetState.close() }
                    },
                    headlineContent = {
                        Text(text = name)
                    },
                )
            }
        }
    } else {
        ProvideSideSheetContent { sheetState ->
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.quick_search)) },
                actions = {
                    IconButton(onClick = {
                        launch {
                            awaitPermissionOrCancel(title = R.string.quick_search, showCancelButton = false) {
                                Text(text = stringResource(id = R.string.add_quick_search_tip))
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Help,
                            contentDescription = stringResource(id = R.string.readme),
                        )
                    }
                    val invalidImageQuickSearch = stringResource(R.string.image_search_not_quick_search)
                    val nameEmpty = stringResource(R.string.name_is_empty)
                    val dupName = stringResource(R.string.duplicate_name)
                    IconButton(onClick = {
                        if (data.itemCount == 0) return@IconButton
                        launch {
                            if (urlBuilder.mode == MODE_IMAGE_SEARCH) {
                                showSnackbar(invalidImageQuickSearch)
                            } else {
                                val firstItem = data.itemSnapshotList.items[getFirstVisibleItemIndex()]
                                val next = firstItem.gid + 1
                                quickSearchList.fastForEach { q ->
                                    if (urlBuilder.equalsQuickSearch(q)) {
                                        val nextStr = q.name.substringAfterLast('@', "")
                                        if (nextStr.toLongOrNull() == next) {
                                            showSnackbar(getString(R.string.duplicate_quick_search, q.name))
                                            return@launch
                                        }
                                    }
                                }
                                awaitInputTextWithCheckBox(
                                    initial = quickSearchName ?: urlBuilder.keyword.orEmpty(),
                                    title = R.string.add_quick_search_dialog_title,
                                    hint = R.string.quick_search,
                                    checked = saveProgress,
                                    checkBoxText = R.string.save_progress,
                                ) { input, checked ->
                                    var text = input.trim()
                                    nameEmpty.takeIf {
                                        text.isEmpty()
                                    } ?: dupName.takeIf {
                                        if (checked) text += "@$next"
                                        quickSearchList.fastAny { it.name == text }
                                    } ?: run {
                                        val quickSearch = urlBuilder.toQuickSearch(text)
                                        quickSearch.position = quickSearchList.size
                                        // Insert to DB first to update the id
                                        EhDB.insertQuickSearch(quickSearch)
                                        quickSearchList.add(quickSearch)
                                        saveProgress = checked
                                        null
                                    }
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.add),
                        )
                    }
                },
                windowInsets = WindowInsets(0),
            )
            val view = LocalView.current
            Box(modifier = Modifier.fillMaxSize()) {
                val quickSearchListState = rememberLazyListState()
                val reorderableLazyListState = rememberReorderableLazyColumnState(quickSearchListState) { from, to ->
                    val fromIndex = from.index - 1
                    val toIndex = to.index - 1
                    quickSearchList.apply { add(toIndex, removeAt(fromIndex)) }
                    view.performHapticFeedback(draggingHapticFeedback)
                }
                var fromIndex by remember { mutableIntStateOf(-1) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = quickSearchListState,
                    contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
                ) {
                    // Fix the first item's reorder animation
                    stickyHeader {
                        HorizontalDivider()
                    }
                    itemsIndexed(quickSearchList, key = { _, item -> item.id!! }) { index, item ->
                        ReorderableItem(reorderableLazyListState, key = item.id!!) { isDragging ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        with(quickSearchList) {
                                            val removeIndex = indexOf(item)
                                            subList(removeIndex + 1, size).forEach {
                                                it.position--
                                            }
                                            removeAt(removeIndex)
                                        }
                                        launchIO { EhDB.deleteQuickSearch(item) }
                                    }
                                    true
                                },
                            )
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
                                        if (urlBuilder.mode == MODE_WHATS_HOT) {
                                            val builder = ListUrlBuilder(item)
                                            navigator.navigate(builder.asDst())
                                        } else {
                                            urlBuilder = ListUrlBuilder(item)
                                            data.refresh()
                                        }
                                        showSearchLayout = false
                                        launch { sheetState.close() }
                                    },
                                    tonalElevation = 1.dp,
                                    shadowElevation = elevation,
                                    headlineContent = {
                                        Text(text = item.name)
                                    },
                                    trailingContent = {
                                        DragHandle(
                                            onDragStarted = {
                                                fromIndex = index
                                            },
                                            onDragStopped = {
                                                if (fromIndex != -1) {
                                                    if (fromIndex != index) {
                                                        val range = if (fromIndex < index) fromIndex..index else index..fromIndex
                                                        val toUpdate = quickSearchList.slice(range)
                                                        toUpdate.zip(range).forEach { it.first.position = it.second }
                                                        launchIO { EhDB.updateQuickSearch(toUpdate) }
                                                    }
                                                    fromIndex = -1
                                                }
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                Deferred({ delay(200) }) {
                    if (quickSearchList.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.quick_search_tip),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }

    val refreshState = rememberPullToRefreshState {
        data.loadState.refresh is LoadState.NotLoading
    }

    var fabExpanded by remember { mutableStateOf(false) }
    var fabHidden by remember { mutableStateOf(false) }

    val openGalleryKeyword = stringResource(R.string.gallery_list_search_bar_open_gallery)
    abstract class UrlSuggestion : Suggestion() {
        override val keyword = openGalleryKeyword
        override val canOpenDirectly = true
        override fun onClick() {
            navigator.navigate(destination)
            showSearchLayout = false
        }
        abstract val destination: Direction
    }

    class GalleryDetailUrlSuggestion(gid: Long, token: String) : UrlSuggestion() {
        override val destination = gid asDstWith token
    }

    class GalleryPageUrlSuggestion(gid: Long, pToken: String, page: Int) : UrlSuggestion() {
        override val destination = ProgressScreenDestination(gid, pToken, page)
    }

    val selectImageFirst = stringResource(R.string.select_image_first)
    fun onApplySearch(query: String) = launchIO {
        val builder = ListUrlBuilder()
        val oldMode = urlBuilder.mode
        if (!showSearchLayout) {
            // If it's MODE_SUBSCRIPTION, keep it
            val newMode = if (oldMode == MODE_SUBSCRIPTION) MODE_SUBSCRIPTION else MODE_NORMAL
            builder.mode = newMode
            builder.keyword = query
            builder.category = category
            builder.advanceSearch = advancedSearchOption.advanceSearch
            builder.minRating = advancedSearchOption.minRating
            builder.pageFrom = advancedSearchOption.fromPage
            builder.pageTo = advancedSearchOption.toPage
        } else {
            if (imageUri == null) {
                showSnackbar(selectImageFirst)
                return@launchIO
            }
            builder.mode = MODE_IMAGE_SEARCH
            builder.hash = imageUri!!.asUniFile().sha1()
        }
        when (oldMode) {
            MODE_TOPLIST, MODE_WHATS_HOT -> {
                // Wait for search view to hide
                delay(300)
                withUIContext { navigator.navigate(builder.asDst()) }
            }
            else -> {
                urlBuilder = builder
                data.refresh()
            }
        }
        showSearchLayout = false
    }

    SearchBarScreen(
        title = suitableTitle,
        searchFieldState = searchFieldState,
        searchFieldHint = searchBarHint,
        onApplySearch = ::onApplySearch,
        onSearchExpanded = { fabHidden = true },
        onSearchHidden = { fabHidden = false },
        refreshState = refreshState,
        suggestionProvider = {
            GalleryDetailUrlParser.parse(it, false)?.run {
                GalleryDetailUrlSuggestion(gid, token)
            } ?: GalleryPageUrlParser.parse(it, false)?.run {
                GalleryPageUrlSuggestion(gid, pToken, page)
            }
        },
        searchBarOffsetY = { searchBarOffsetY },
        trailingIcon = {
            val sheetState = LocalSideSheetState.current
            IconButton(onClick = { launch { sheetState.open() } }) {
                Icon(imageVector = Icons.Outlined.Bookmarks, contentDescription = stringResource(id = R.string.quick_search))
            }
            IconButton(onClick = { showSearchLayout = !showSearchLayout }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = lerp(135f, 0f, animatedSearchLayout)
                    },
                )
            }
        },
        filter = {
            SearchFilter(
                category = category,
                onCategoryChanged = {
                    Settings.searchCategory = it
                    category = it
                },
                advancedOption = advancedSearchOption,
                onAdvancedOptionChanged = { advancedSearchOption = it },
            )
        },
        floatingActionButton = {
            val hiddenState by animateFloatAsState(
                targetValue = if (showSearchLayout && !fabHidden) 1f else 0f,
                animationSpec = tween(
                    FAB_ANIMATE_TIME,
                    if (showSearchLayout && !fabHidden) FAB_ANIMATE_TIME else 0,
                ),
                label = "hiddenState",
            )
            FloatingActionButton(
                onClick = { onApplySearch("") },
                modifier = Modifier.snackBarPadding().graphicsLayer {
                    rotationZ = lerp(90f, 0f, hiddenState)
                    scaleX = hiddenState
                    scaleY = hiddenState
                },
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            }
        },
    ) { contentPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val marginH = dimensionResource(id = R.dimen.gallery_list_margin_h)
        val marginV = dimensionResource(id = R.dimen.gallery_list_margin_v)
        Column(
            modifier = Modifier.imePadding().verticalScroll(rememberScrollState())
                .padding(
                    top = contentPadding.calculateTopPadding() + marginV,
                    start = contentPadding.calculateStartPadding(layoutDirection) + marginH,
                    end = contentPadding.calculateEndPadding(layoutDirection) + marginH,
                    bottom = 8.dp,
                )
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                .graphicsLayer {
                    scaleX = 1 - animatedSearchLayout
                    scaleY = 1 - animatedSearchLayout
                    alpha = 1 - animatedSearchLayout
                },
        ) {
            ElevatedCard(modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.search_layout_margin_v))) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(id = R.dimen.search_category_padding_h),
                        vertical = dimensionResource(id = R.dimen.search_category_padding_v),
                    ).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.search_image),
                        modifier = Modifier.height(dimensionResource(id = R.dimen.search_category_title_height)),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    ImageSearch(
                        image = imageUri,
                        onSelectImage = {
                            launch { imageUri = pickVisualMedia(ActivityResultContracts.PickVisualMedia.ImageOnly) }
                        },
                    )
                }
            }
        }

        val height by collectListThumbSizeAsState()
        val showPages by Settings.showGalleryPages.collectAsState()
        val context = LocalContext.current
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
        GalleryList(
            modifier = Modifier.graphicsLayer {
                scaleX = animatedSearchLayout
                scaleY = animatedSearchLayout
                alpha = animatedSearchLayout
            },
            data = data,
            contentModifier = Modifier.nestedScroll(searchBarConnection),
            contentPadding = contentPadding,
            listMode = listMode,
            detailListState = listState,
            detailItemContent = { info ->
                GalleryInfoListItem(
                    onClick = { navigate(info.asDst()) },
                    onLongClick = { launch { doGalleryInfoAction(info) } },
                    info = info,
                    showPages = showPages,
                    modifier = Modifier.height(height),
                )
            },
            thumbListState = gridState,
            thumbItemContent = { info ->
                GalleryInfoGridItem(
                    onClick = { navigate(info.asDst()) },
                    onLongClick = { launch { doGalleryInfoAction(info) } },
                    info = info,
                )
            },
            refreshState = refreshState,
            onRefresh = {
                urlBuilder.setRange(0)
                data.refresh()
            },
            onLoading = { searchBarOffsetY = 0 },
        )
    }

    val gotoTitle = stringResource(R.string.go_to)
    val invalidNum = stringResource(R.string.error_invalid_number)
    val outOfRange = stringResource(R.string.error_out_of_range)

    val hideFab by delegateSnapshotUpdate {
        record { fabHidden || showSearchLayout }
        transform {
            // Bug: IDE failed to inference 'hide's type
            onEachLatest { hide: Boolean ->
                if (!hide) delay(FAB_ANIMATE_TIME.toLong())
            }
        }
    }

    FabLayout(
        hidden = hideFab,
        expanded = fabExpanded,
        onExpandChanged = { fabExpanded = it },
        autoCancel = true,
    ) {
        if (urlBuilder.mode in arrayOf(MODE_NORMAL, MODE_UPLOADER, MODE_TAG)) {
            onClick(Icons.Default.Shuffle) {
                urlBuilder.setRange(Random.nextInt(100))
                data.refresh()
            }
        }
        onClick(Icons.Default.Refresh) {
            urlBuilder.setRange(0)
            data.refresh()
        }
        if (urlBuilder.mode != MODE_WHATS_HOT) {
            onClick(EhIcons.Default.GoTo) {
                if (isTopList) {
                    val page = urlBuilder.mJumpTo?.toIntOrNull() ?: 0
                    val hint = getString(R.string.go_to_hint, page + 1, TOPLIST_PAGES)
                    val text = awaitInputText(title = gotoTitle, hint = hint, isNumber = true) { oriText ->
                        val text = oriText.trim()
                        val goTo = runCatching {
                            text.toInt() - 1
                        }.onFailure {
                            return@awaitInputText invalidNum
                        }.getOrThrow()
                        if (goTo !in 0..<TOPLIST_PAGES) outOfRange else null
                    }.trim().toInt() - 1
                    urlBuilder.setJumpTo(text)
                    data.refresh()
                } else {
                    launch {
                        val date = showDatePicker()
                        urlBuilder.mJumpTo = date
                        data.refresh()
                    }
                }
            }
            onClick(Icons.AutoMirrored.Default.LastPage) {
                if (isTopList) {
                    urlBuilder.setJumpTo(TOPLIST_PAGES - 1)
                    data.refresh()
                } else {
                    urlBuilder.setIndex("1", false)
                    data.refresh()
                }
            }
        }
    }
}

private const val TOPLIST_PAGES = 200

@Composable
@Stable
private fun getSuitableTitleForUrlBuilder(urlBuilder: ListUrlBuilder, appName: Boolean = true): String? {
    val context = LocalContext.current
    val keyword = urlBuilder.keyword
    val category = urlBuilder.category
    val mode = urlBuilder.mode
    return if (mode == MODE_WHATS_HOT) {
        stringResource(R.string.whats_hot)
    } else if (!keyword.isNullOrEmpty()) {
        when (mode) {
            MODE_TOPLIST -> {
                when (keyword) {
                    "11" -> stringResource(R.string.toplist_alltime)
                    "12" -> stringResource(R.string.toplist_pastyear)
                    "13" -> stringResource(R.string.toplist_pastmonth)
                    "15" -> stringResource(R.string.toplist_yesterday)
                    else -> null
                }
            }
            MODE_TAG -> {
                val canTranslate = Settings.showTagTranslations && EhTagDatabase.isTranslatable(context) && EhTagDatabase.initialized
                wrapTagKeyword(keyword, canTranslate)
            }
            else -> keyword
        }
    } else if (category == EhUtils.NONE && urlBuilder.advanceSearch == -1) {
        val appNameStr = stringResource(R.string.app_name)
        val homepageStr = stringResource(R.string.homepage)
        when (mode) {
            MODE_NORMAL -> if (appName) appNameStr else homepageStr
            MODE_SUBSCRIPTION -> stringResource(R.string.subscription)
            else -> null
        }
    } else if (category.countOneBits() == 1) {
        EhUtils.getCategory(category)
    } else {
        null
    }
}
