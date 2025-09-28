package com.hippo.ehviewer.ui.screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.util.withIOContext
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TOPLIST
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.tarsin.coroutines.runSuspendCatching

class GalleryListViewModel(lub: ListUrlBuilder, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val mutex = Mutex()

    val urlBuilder by savedStateHandle.saved(MutableStateSerializer()) {
        mutableStateOf(lub)
    }

    // Use a smaller prefetch distance to avoid prepending an additional page after jumping
    val data = Pager(PagingConfig(25, prefetchDistance = 20)) {
        object : PagingSource<String, BaseGalleryInfo>() {
            override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
            override suspend fun load(params: LoadParams<String>) = withIOContext {
                val urlBuilder = urlBuilder.value
                if (urlBuilder.mode == MODE_TOPLIST) {
                    // TODO: Since we know total pages, let pager support jump
                    val key: Int
                    val url = mutex.withLock {
                        with(urlBuilder) {
                            params.key?.let { page = it.toInt() }
                            key = page
                            build()
                        }
                    }
                    val prev = (key - 1).takeIf { it > 0 }
                    val next = (key + 1).takeIf { it <= TOPLIST_PAGES }
                    runSuspendCatching {
                        EhEngine.getGalleryList(url)
                    }.foldToLoadResult { result ->
                        LoadResult.Page(result.galleryInfoList, prev?.toString(), next?.toString())
                    }
                } else {
                    val url = mutex.withLock {
                        with(urlBuilder) {
                            when (params) {
                                is LoadParams.Prepend -> setIndex(params.key, isNext = false)
                                is LoadParams.Append -> setIndex(params.key, isNext = true)
                                is LoadParams.Refresh -> params.key?.let { setIndex(it, false) }
                            }
                            build()
                        }
                    }
                    runSuspendCatching {
                        EhEngine.getGalleryList(url)
                    }.foldToLoadResult { result ->
                        LoadResult.Page(result.galleryInfoList, result.prev, result.next)
                    }
                }
            }
        }
    }.flow.cachedIn(viewModelScope)
}
