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
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TOPLIST
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import eu.kanade.tachiyomi.util.lang.withIOContext
import moe.tarsin.coroutines.runSuspendCatching

class GalleryListViewModel(lub: ListUrlBuilder, savedStateHandle: SavedStateHandle) : ViewModel() {
    val urlBuilder by savedStateHandle.saved(MutableStateSerializer()) {
        mutableStateOf(lub)
    }

    val data = Pager(PagingConfig(25)) {
        object : PagingSource<String, BaseGalleryInfo>() {
            override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
            override suspend fun load(params: LoadParams<String>) = withIOContext {
                val urlBuilder = urlBuilder.value
                if (urlBuilder.mode == MODE_TOPLIST) {
                    // TODO: Since we know total pages, let pager support jump
                    val key = (params.key ?: urlBuilder.jumpTo)?.toInt() ?: 0
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
                                if (urlBuilder.jumpTo != null) {
                                    urlBuilder.next ?: urlBuilder.setIndex("2", true)
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
                        urlBuilder.jumpTo = null
                        LoadResult.Page(result.galleryInfoList, result.prev, result.next)
                    }
                }
            }
        }
    }.flow.cachedIn(viewModelScope)
}
