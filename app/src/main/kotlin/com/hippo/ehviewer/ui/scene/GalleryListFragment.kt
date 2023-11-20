package com.hippo.ehviewer.ui.scene

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.google.android.material.datepicker.CalendarConstraints.DateValidator
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_IMAGE_SEARCH
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_NORMAL
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_SUBSCRIPTION
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TAG
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TOPLIST
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_WHATS_HOT
import com.hippo.ehviewer.client.exception.CloudflareBypassException
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.SadAndroid
import com.hippo.ehviewer.icons.filled.GoTo
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.WebViewActivity
import com.hippo.ehviewer.ui.doGalleryInfoAction
import com.hippo.ehviewer.ui.main.FabLayout
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalStaggeredGrid
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.getParcelableCompat
import com.ramcosta.composedestinations.annotation.Destination
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.pxToDp
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import moe.tarsin.coroutines.runSuspendCatching

@Destination
@Composable
fun GalleryListScreen(lub: ListUrlBuilder, navigator: NavController) {
    val urlBuilder by rememberSaveable(lub) { mutableStateOf(lub) }
    var searchBarOffsetY by remember { mutableStateOf(0) }
    var state by rememberSaveable { mutableStateOf(State.NORMAL) }

    val context = LocalContext.current
    val activity = remember { context.findActivity<MainActivity>() }
    val dialogState = LocalDialogState.current
    val coroutineScope = rememberCoroutineScope()
    val isTopList = remember(urlBuilder) { urlBuilder.mode == MODE_TOPLIST }
    val ehHint = stringResource(R.string.gallery_list_search_bar_hint_e_hentai)
    val exHint = stringResource(R.string.gallery_list_search_bar_hint_exhentai)
    val searchBarHint = remember { if (EhUtils.isExHentai) exHint else ehHint }
    val search = stringResource(R.string.search)
    val title = remember(urlBuilder) { context.getSuitableTitleForUrlBuilder(urlBuilder, true) ?: search }
    val data = rememberInVM(urlBuilder) {
        Pager(PagingConfig(25)) {
            object : PagingSource<String, BaseGalleryInfo>() {
                override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
                override suspend fun load(params: LoadParams<String>) = withIOContext {
                    if (urlBuilder.mode == MODE_TOPLIST) {
                        // TODO: Since we know total pages, let pager support jump
                        val key = (params.key ?: urlBuilder.mJumpTo ?: "0").toInt()
                        val prev = (key - 1).takeIf { it > 0 }
                        val next = (key + 1).takeIf { it < TOPLIST_PAGES }
                        runSuspendCatching {
                            urlBuilder.setJumpTo(key)
                            EhEngine.getGalleryList(urlBuilder.build())
                        }.onFailure {
                            return@withIOContext LoadResult.Error(it)
                        }.onSuccess {
                            return@withIOContext LoadResult.Page(it.galleryInfoList, prev?.toString(), next?.toString())
                        }
                    }
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
                    val r = runSuspendCatching {
                        if (MODE_IMAGE_SEARCH == urlBuilder.mode) {
                            EhEngine.imageSearch(
                                File(urlBuilder.imagePath!!),
                                urlBuilder.isUseSimilarityScan,
                                urlBuilder.isOnlySearchCovers,
                            )
                        } else {
                            val url = urlBuilder.build()
                            EhEngine.getGalleryList(url)
                        }
                    }.onFailure {
                        return@withIOContext LoadResult.Error(it)
                    }.getOrThrow()
                    urlBuilder.mJumpTo = null
                    LoadResult.Page(r.galleryInfoList, r.prev, r.next)
                }
            }
        }.flow.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()

    val refreshState = rememberPullToRefreshState {
        data.loadState.refresh is LoadState.NotLoading
    }

    if (refreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            urlBuilder.setIndex(null, true)
            urlBuilder.mJumpTo = null
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

    val searchFieldState = rememberTextFieldState()

    val openGalleryKeyword = stringResource(R.string.gallery_list_search_bar_open_gallery)
    abstract class UrlSuggestion : Suggestion() {
        override val keyword = openGalleryKeyword
        override val canOpenDirectly = true
        override fun onClick() {
            navigator.navAnimated(destination, args)
            if (state == State.SIMPLE_SEARCH) {
                state = State.NORMAL
            }
            if (state == State.SEARCH_SHOW_LIST) {
                state = State.SEARCH
            }
        }
        abstract val destination: Int
        abstract val args: Bundle
    }

    class GalleryDetailUrlSuggestion(
        gid: Long,
        token: String,
    ) : UrlSuggestion() {
        override val destination = R.id.galleryDetailScene
        override val args = bundleOf(GalleryDetailScene.KEY_ARGS to TokenArgs(gid, token))
    }

    class GalleryPageUrlSuggestion(
        gid: Long,
        pToken: String,
        page: Int,
    ) : UrlSuggestion() {
        override val destination = R.id.progressScene
        override val args = bundleOf(
            ProgressFragment.KEY_GID to gid,
            ProgressFragment.KEY_PTOKEN to pToken,
            ProgressFragment.KEY_PAGE to page,
        )
    }

    var expanded by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    SearchBarScreen(
        title = title,
        searchFieldState = searchFieldState,
        searchFieldHint = searchBarHint,
        onApplySearch = { },
        onSearchExpanded = { hidden = true },
        onSearchHidden = { hidden = false },
        refreshState = refreshState,
        suggestionProvider = {
            GalleryDetailUrlParser.parse(it, false)?.run {
                GalleryDetailUrlSuggestion(gid, token)
            } ?: GalleryPageUrlParser.parse(it, false)?.run {
                GalleryPageUrlSuggestion(gid, pToken, page)
            }
        },
        searchBarOffsetY = searchBarOffsetY,
        trailingIcon = {
        },
    ) {
        val realPadding = PaddingValues(
            top = it.calculateTopPadding() + 8.dp,
            bottom = it.calculateBottomPadding(),
        )
        val listMode by remember {
            Settings.listModeBackField.valueFlow()
        }.collectAsState(Settings.listMode)
        val marginH = dimensionResource(id = R.dimen.gallery_list_margin_h)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (val loadState = data.loadState.refresh) {
                is LoadState.Loading -> if (data.itemCount == 0) CircularProgressIndicator()
                is LoadState.Error -> {
                    LaunchedEffect(loadState) {
                        if (loadState.error.cause is CloudflareBypassException) {
                            dialogState.awaitPermissionOrCancel(title = R.string.cloudflare_bypass_failed) {
                                Text(text = stringResource(id = R.string.open_in_webview))
                            }
                            withUIContext { navigator.navAnimated(R.id.webView, bundleOf(WebViewActivity.KEY_URL to EhUrl.host)) }
                        }
                    }
                    Column(
                        modifier = Modifier.widthIn(max = 228.dp).clip(ShapeDefaults.Small).clickable { data.retry() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = EhIcons.Big.Default.SadAndroid,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp).size(120.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = ExceptionUtils.getReadableString(loadState.error),
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
                    modifier = Modifier.padding(horizontal = marginH) then combinedModifier,
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
                            GalleryInfoListItem(
                                onClick = {
                                    navigator.navAnimated(
                                        R.id.galleryDetailScene,
                                        bundleOf(GalleryDetailScene.KEY_ARGS to GalleryInfoArgs(info)),
                                    )
                                },
                                onLongClick = {
                                    coroutineScope.launch {
                                        dialogState.doGalleryInfoAction(info, context)
                                    }
                                },
                                info = info,
                                isInFavScene = false,
                                showPages = showPages,
                                modifier = Modifier.height(height),
                            )
                        }
                    }
                }
            } else {
                val gridInterval = dimensionResource(R.dimen.gallery_grid_interval)
                FastScrollLazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(Settings.thumbSizeDp.dp),
                    modifier = Modifier.padding(horizontal = marginH) then combinedModifier,
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
                            GalleryInfoGridItem(
                                onClick = {
                                    navigator.navAnimated(
                                        R.id.galleryDetailScene,
                                        bundleOf(GalleryDetailScene.KEY_ARGS to GalleryInfoArgs(info)),
                                    )
                                },
                                onLongClick = {
                                    coroutineScope.launch {
                                        dialogState.doGalleryInfoAction(info, context)
                                    }
                                },
                                info = info,
                            )
                        }
                    }
                }
            }
        }
    }

    val gotoTitle = stringResource(R.string.go_to)
    val invalidNum = stringResource(R.string.error_invalid_number)
    val outOfRange = stringResource(R.string.error_out_of_range)
    FabLayout(
        hidden = hidden,
        expanded = expanded,
        onExpandChanged = { expanded = it },
        autoCancel = true,
    ) {
        onClick(Icons.Default.Refresh) {
            urlBuilder.setIndex(null, true)
            urlBuilder.mJumpTo = null
            data.refresh()
        }
        if (urlBuilder.mode != MODE_WHATS_HOT) {
            onClick(EhIcons.Default.GoTo) {
                if (isTopList) {
                    val page = urlBuilder.mJumpTo?.toIntOrNull() ?: 0
                    val hint = context.getString(R.string.go_to_hint, page + 1, TOPLIST_PAGES)
                    coroutineScope.launch {
                        val text = dialogState.awaitInputText(title = gotoTitle, hint = hint, isNumber = true) { oriText ->
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
                    }
                } else {
                    val local = LocalDateTime.of(2007, 3, 21, 0, 0)
                    val fromDate =
                        local.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant().toEpochMilli()
                    val toDate = MaterialDatePicker.todayInUtcMilliseconds()
                    val listValidators = ArrayList<DateValidator>()
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
                    datePicker.addOnPositiveButtonClickListener { time ->
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).withZone(ZoneOffset.UTC)
                        val jumpTo = formatter.format(Instant.ofEpochMilli(time))
                        urlBuilder.mJumpTo = jumpTo
                        data.refresh()
                    }
                }
            }
            onClick(Icons.AutoMirrored.Default.LastPage) {
                if (isTopList) {
                    urlBuilder.mJumpTo = "${TOPLIST_PAGES - 1}"
                    data.refresh()
                } else {
                    urlBuilder.setIndex("1", false)
                    data.refresh()
                }
            }
        }
    }
}

class GalleryListFragment : BaseScene() {
    override val enableDrawerGestures = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val navController = findNavController()
        val args = when (arguments?.getString(KEY_ACTION) ?: ACTION_HOMEPAGE) {
            ACTION_HOMEPAGE -> ListUrlBuilder()
            ACTION_SUBSCRIPTION -> ListUrlBuilder(MODE_SUBSCRIPTION)
            ACTION_WHATS_HOT -> ListUrlBuilder(MODE_WHATS_HOT)
            ACTION_TOP_LIST -> ListUrlBuilder(MODE_TOPLIST, mKeyword = Settings.recentToplist)
            ACTION_LIST_URL_BUILDER -> arguments?.getParcelableCompat<ListUrlBuilder>(KEY_LIST_URL_BUILDER)?.copy() ?: ListUrlBuilder()
            else -> error("Wrong KEY_ACTION:${arguments?.getString(KEY_ACTION)} when handle args!")
        }
        return ComposeWithMD3 { GalleryListScreen(args, navController) }
    }

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_HOMEPAGE = "action_homepage"
        const val ACTION_SUBSCRIPTION = "action_subscription"
        const val ACTION_WHATS_HOT = "action_whats_hot"
        const val ACTION_TOP_LIST = "action_top_list"
        const val ACTION_LIST_URL_BUILDER = "action_list_url_builder"
        const val KEY_LIST_URL_BUILDER = "list_url_builder"
        fun ListUrlBuilder.toStartArgs() = bundleOf(
            KEY_ACTION to ACTION_LIST_URL_BUILDER,
            KEY_LIST_URL_BUILDER to this,
        )
    }
}

private const val TOPLIST_PAGES = 200

@Parcelize
enum class State : Parcelable {
    NORMAL,
    SIMPLE_SEARCH,
    SEARCH,
    SEARCH_SHOW_LIST,
}

private fun Context.getSuitableTitleForUrlBuilder(
    urlBuilder: ListUrlBuilder,
    appName: Boolean,
): String? {
    val keyword = urlBuilder.keyword
    val category = urlBuilder.category
    val mode = urlBuilder.mode
    return if (mode == MODE_WHATS_HOT) {
        getString(R.string.whats_hot)
    } else if (!keyword.isNullOrEmpty()) {
        when (mode) {
            MODE_TOPLIST -> {
                when (keyword) {
                    "11" -> getString(R.string.toplist_alltime)
                    "12" -> getString(R.string.toplist_pastyear)
                    "13" -> getString(R.string.toplist_pastmonth)
                    "15" -> getString(R.string.toplist_yesterday)
                    else -> null
                }
            }

            MODE_TAG -> {
                val canTranslate = Settings.showTagTranslations && EhTagDatabase.isTranslatable(this) && EhTagDatabase.initialized
                wrapTagKeyword(keyword, canTranslate)
            }
            else -> keyword
        }
    } else if (category == EhUtils.NONE && urlBuilder.advanceSearch == -1) {
        when (mode) {
            MODE_NORMAL -> getString(if (appName) R.string.app_name else R.string.homepage)
            MODE_SUBSCRIPTION -> getString(R.string.subscription)
            else -> null
        }
    } else if (category.countOneBits() == 1) {
        EhUtils.getCategory(category)
    } else {
        null
    }
}
