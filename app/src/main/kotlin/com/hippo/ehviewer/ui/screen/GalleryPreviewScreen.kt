package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import arrow.fx.coroutines.parMap
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.coil.justDownload
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ktbuilder.launchIn
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.composing
import com.hippo.ehviewer.ui.main.EhPreviewItem
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalGrid
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import com.hippo.ehviewer.ui.tools.getClippedRefreshKey
import com.hippo.ehviewer.ui.tools.getLimit
import com.hippo.ehviewer.ui.tools.getOffset
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.flattenForEach
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withIOContext
import moe.tarsin.coroutines.runSuspendCatching

@Destination
@Composable
fun GalleryPreviewScreen(detail: GalleryDetail, toNextPage: Boolean, navigator: DestinationsNavigator) = composing(navigator) {
    LockDrawer(true)
    val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()
    val pgSize = detail.previewList.size
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = if (toNextPage) pgSize else 0)
    val thumbColumns by Settings.thumbColumns.collectAsState()
    val data = rememberInVM {
        val pages = detail.pages
        val previewPagesMap = detail.previewList.associateBy { it.position } as MutableMap
        Pager(
            PagingConfig(
                pageSize = pgSize,
                prefetchDistance = thumbColumns,
                initialLoadSize = pgSize,
                jumpThreshold = 2 * pgSize,
            ),
        ) {
            object : PagingSource<Int, GalleryPreview>() {
                override fun getRefreshKey(state: PagingState<Int, GalleryPreview>) = state.getClippedRefreshKey()
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryPreview> = withIOContext {
                    val key = params.key ?: 0
                    val up = getOffset(params, key, pages)
                    val end = (up + getLimit(params, key) - 1).coerceAtMost(pages - 1)
                    detail.runSuspendCatching {
                        (up..end).filterNot { it in previewPagesMap }.map { it / pgSize }.toSet()
                            .parMap(concurrency = Settings.multiThreadDownload) { page ->
                                val url = EhUrl.getGalleryDetailUrl(gid, token, page, false)
                                EhEngine.getPreviewList(url).first
                            }.flattenForEach {
                                previewPagesMap[it.position] = it
                                if (Settings.preloadThumbAggressively) {
                                    imageRequest(it) { justDownload() }.launchIn(viewModelScope)
                                }
                            }
                    }.foldToLoadResult {
                        val r = (up..end).map { requireNotNull(previewPagesMap[it]) }
                        val prevK = if (up <= 0 || r.isEmpty()) null else up
                        val nextK = if (end == pages - 1) null else end + 1
                        LoadResult.Page(r, prevK, nextK, up, pages - end - 1)
                    }
                }
                override val jumpingSupported = true
            }
        }.flow.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gallery_previews)) },
                navigationIcon = {
                    IconButton(onClick = { popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehaviour,
            )
        },
    ) { paddingValues ->
        FastScrollLazyVerticalGrid(
            columns = GridCells.Fixed(thumbColumns),
            modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection).padding(horizontal = dimensionResource(id = R.dimen.gallery_list_margin_h)),
            state = state,
            contentPadding = paddingValues,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                count = data.itemCount,
                key = data.itemKey(key = { item -> item.position }),
                contentType = data.itemContentType(),
            ) { index ->
                val item = data[index]
                EhPreviewItem(item, index) { navToReader(detail.galleryInfo, index) }
            }
        }
    }
}
