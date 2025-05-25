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
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import moe.tarsin.coroutines.runSuspendCatching

class FavoritesViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
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
            }
        } else {
            Pager(PagingConfig(25)) {
                object : PagingSource<String, BaseGalleryInfo>() {
                    override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
                    override suspend fun load(params: LoadParams<String>) = withIOContext {
                        val urlBuilder = urlBuilder.value
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
                            EhEngine.getFavorites(urlBuilder.build())
                        }.foldToLoadResult { result ->
                            Settings.favCat = result.catArray.toTypedArray()
                            Settings.favCount = result.countArray.toIntArray()
                            Settings.favCloudCount = result.countArray.sum()
                            urlBuilder.jumpTo = null
                            LoadResult.Page(result.galleryInfoList, result.prev, result.next)
                        }
                    }
                }
            }
        }.flow.map { pagingData ->
            // https://github.com/FooIbar/EhViewer/issues/1190
            // Workaround for duplicate items when sorting by favorited time
            val gidSet = MutableLongSet(50)
            pagingData.filter { gidSet.add(it.gid) }
        }
    }.cachedIn(viewModelScope)
}
