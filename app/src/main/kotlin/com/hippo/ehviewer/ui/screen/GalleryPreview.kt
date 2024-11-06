package com.hippo.ehviewer.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
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
import com.hippo.ehviewer.ui.main.EhPreviewItem
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalGrid
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import com.hippo.ehviewer.ui.tools.getClippedRefreshKey
import com.hippo.ehviewer.ui.tools.getLimit
import com.hippo.ehviewer.ui.tools.getOffset
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.flattenForEach
import eu.kanade.tachiyomi.util.lang.withIOContext
import moe.tarsin.coroutines.runSuspendCatching

context(Context)
@Composable
fun GalleryPreview(
    detail: GalleryDetail,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val thumbColumns by Settings.thumbColumns.collectAsState()
    val data = collectPreviewItems(detail, thumbColumns)
    FastScrollLazyVerticalGrid(
        columns = GridCells.Fixed(thumbColumns),
        containerModifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding_v)),
    ) {
        items(
            count = data.itemCount,
            key = data.itemKey(key = { item -> item.position }),
            contentType = { "preview" },
        ) { index ->
            val item = data[index]
            EhPreviewItem(item, index) { onItemClick(index) }
        }
    }
}

context(Context)
@Composable
private fun collectPreviewItems(detail: GalleryDetail, prefetchDistance: Int) = rememberInVM(detail) {
    val pageSize = detail.previewList.size
    val pages = detail.pages
    val previewPagesMap = detail.previewList.associateBy { it.position } as MutableMap
    Pager(
        PagingConfig(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance,
            initialLoadSize = pageSize,
            jumpThreshold = 2 * pageSize,
        ),
    ) {
        object : PagingSource<Int, GalleryPreview>() {
            override fun getRefreshKey(state: PagingState<Int, GalleryPreview>) = state.getClippedRefreshKey()
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryPreview> = withIOContext {
                val key = params.key ?: 0
                val up = getOffset(params, key, pages)
                val end = (up + getLimit(params, key) - 1).coerceAtMost(pages - 1)
                detail.runSuspendCatching {
                    (up..end).filterNot { it in previewPagesMap }.map { it / pageSize }.toSet()
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
