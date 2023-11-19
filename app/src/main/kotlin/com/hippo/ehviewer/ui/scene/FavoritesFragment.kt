package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalStaggeredGrid
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.findActivity
import com.ramcosta.composedestinations.annotation.Destination
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.pxToDp
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

@Destination
@Composable
fun FavouritesScreen(navigator: NavController) {
    // Meta State
    var urlBuilder by rememberSaveable { mutableStateOf(FavListUrlBuilder(favCat = Settings.recentFavCat)) }

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
            TopAppBar(title = { Text(text = stringResource(id = R.string.collections)) })
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

    SearchBarScreen(
        title = title,
        searchFieldState = searchFieldState,
        searchFieldHint = searchBarHint,
        onApplySearch = {
            switchFav(urlBuilder.favCat, it)
        },
        onSearchExpanded = {
        },
        onSearchHidden = {
        },
        searchBarOffsetY = mutableStateOf(0),
        trailingIcon = {
            IconButton(onClick = { activity.openSideSheet() }) {
                Icon(imageVector = Icons.Outlined.FolderSpecial, contentDescription = null)
            }
        },
    ) {
        val realPadding = PaddingValues(
            top = it.calculateTopPadding() + 8.dp,
            bottom = it.calculateBottomPadding(),
        )
        if (Settings.listMode == 0) {
            val height = (3 * Settings.listThumbSize * 3).pxToDp.dp
            val showPages = Settings.showGalleryPages
            FastScrollLazyColumn(
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.gallery_list_margin_h)),
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
                            },
                            info = info,
                            isInFavScene = true,
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
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.gallery_list_margin_h)),
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
                            },
                            info = info,
                        )
                    }
                }
            }
        }
    }
}

class FavoritesFragment : BaseScene() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeWithMD3 {
            val navController = remember { findNavController() }
            FavouritesScreen(navController)
        }
    }
}
