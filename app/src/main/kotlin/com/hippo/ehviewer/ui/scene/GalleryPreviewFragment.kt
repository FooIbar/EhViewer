package com.hippo.ehviewer.ui.scene

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.room.paging.util.getClippedRefreshKey
import androidx.room.paging.util.getLimit
import androidx.room.paging.util.getOffset
import arrow.fx.coroutines.parMap
import coil.imageLoader
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.coil.justDownload
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.main.EhPreviewItem
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalGrid
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

@Destination
@Composable
fun GalleryPreviewScreen(galleryDetail: GalleryDetail, toNextPageArg: Boolean, navigator: NavController) {
    LockDrawer(true)
    val context = LocalContext.current
    fun onPreviewClick(index: Int) = context.navToReader(galleryDetail.galleryInfo, index)
    val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()
    val state = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val pages = galleryDetail.pages
    val pgSize = galleryDetail.previewList.size
    var toNextPage by rememberSaveable { mutableStateOf(toNextPageArg) }

    LaunchedEffect(Unit) {
        if (toNextPage) state.scrollToItem(pgSize)
        toNextPage = false
    }

    suspend fun getPreviewListByPage(page: Int) = galleryDetail.run {
        val url = EhUrl.getGalleryDetailUrl(gid, token, page, false)
        val result = EhEngine.getPreviewList(url)
        if (Settings.preloadThumbAggressively) {
            coroutineScope.launch {
                context.run { result.first.first.forEach { imageLoader.enqueue(imageRequest(it) { justDownload() }) } }
            }
        }
        result.first.first
    }

    val data = rememberInVM {
        val previewPagesMap = galleryDetail.previewList.associateBy { it.position } as MutableMap
        Pager(PagingConfig(pageSize = pgSize, initialLoadSize = pgSize, jumpThreshold = 2 * pgSize)) {
            object : PagingSource<Int, GalleryPreview>() {
                override fun getRefreshKey(state: PagingState<Int, GalleryPreview>) = state.getClippedRefreshKey()
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryPreview> {
                    val key = params.key ?: 0
                    val up = getOffset(params, key, pages)
                    val end = (up + getLimit(params, key) - 1).coerceAtMost(pages - 1)
                    runSuspendCatching {
                        (up..end).filterNot { it in previewPagesMap }.map { it / pgSize }.toSet()
                            .parMap(Dispatchers.IO, Settings.multiThreadDownload) { getPreviewListByPage(it) }
                            .forEach { previews -> previews.forEach { previewPagesMap[it.position] = it } }
                    }.onFailure {
                        return LoadResult.Error(it)
                    }
                    val r = (up..end).map { requireNotNull(previewPagesMap[it]) }
                    val prevK = if (up <= 0 || r.isEmpty()) null else up
                    val nextK = if (end == pages - 1) null else end + 1
                    return LoadResult.Page(r, prevK, nextK, up, pages - end - 1)
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
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehaviour,
            )
        },
    ) { paddingValues ->
        val thumbColumns by Settings.thumbColumns.collectAsState()
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
                EhPreviewItem(item, index) {
                    onPreviewClick(index)
                }
            }
        }
    }
}
