package com.hippo.ehviewer.ui.screen

import androidx.collection.MutableLongSet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.util.withIOContext
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.tarsin.coroutines.runSuspendCatching

class FavoritesViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val mutex = Mutex()

    val urlBuilder by savedStateHandle.saved(MutableStateSerializer()) {
        mutableStateOf(FavListUrlBuilder(favCat = Settings.recentFavCat))
    }

    val localFavCount = EhDB.localFavCount

    val data = snapshotFlow { urlBuilder.value.isLocal }.flatMapLatest { isLocalFav ->
        if (isLocalFav) {
            Pager(PagingConfig(20, jumpThreshold = 40)) {
                val keywordNow = urlBuilder.value.keyword.orEmpty()
                if (keywordNow.isBlank()) {
                    EhDB.localFavLazyList
                } else {
                    EhDB.searchLocalFav(keywordNow)
                }
            }.flow.map { data -> data.map<_, BaseGalleryInfo> { it } }
        } else {
            Pager(PagingConfig(DEFAULT_PAGE_SIZE, prefetchDistance = 20)) {
                object : PagingSource<String, BaseGalleryInfo>() {
                    override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
                    override suspend fun load(params: LoadParams<String>) = withIOContext {
                        val url = mutex.withLock {
                            with(urlBuilder.value) {
                                when (params) {
                                    is LoadParams.Prepend -> setIndex(params.key, isNext = false)
                                    is LoadParams.Append -> setIndex(params.key, isNext = true)
                                    is LoadParams.Refresh -> params.key?.let { setIndex(it, false) }
                                }
                                build()
                            }
                        }
                        runSuspendCatching {
                            EhEngine.getFavorites(url)
                        }.foldToLoadResult { result ->
                            Settings.favCat = result.catArray.toTypedArray()
                            Settings.favCount = result.countArray.toIntArray()
                            Settings.favCloudCount = result.countArray.sum()
                            LoadResult.Page(result.galleryInfoList, result.prev, result.next)
                        }
                    }
                }
            }.flow.map { data ->
                // https://github.com/FooIbar/EhViewer/issues/1190
                // Workaround for duplicate items when sorting by favorited time
                val gidSet = MutableLongSet(DEFAULT_PAGE_SIZE)
                data.filter { gidSet.add(it.gid) }
            }
        }
    }.cachedIn(viewModelScope)
}

private const val DEFAULT_PAGE_SIZE = 50
