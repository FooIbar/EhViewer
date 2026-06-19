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
import com.ehviewer.core.util.toLocalDateTime
import com.ehviewer.core.util.withIOContext
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.client.parser.ParserUtils
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import kotlin.random.Random
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
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

    /**
     * Picks a random cloud favorite from the current view (category + keyword).
     *
     * Cloud favorites use opaque cursor pagination (GID seek positions, not
     * page numbers), so we can't `ORDER BY RANDOM()` or jump to "page N".
     * Instead we make at most three requests:
     *
     * 1. First page — get the newest favorite's date.
     * 2. Last page via `prev=1-0` — get the oldest favorite's date.
     * 3. Random date between them → `seek=<date>` — fetch any middle page.
     *
     * This reaches any page in the list in one request after establishing the
     * range. If the date-seek fails we fall back to 50/50 between first and
     * last page; if that also fails we return from the first page, so the
     * action always opens something on the first click.
     */
    suspend fun randomCloudFav(): BaseGalleryInfo? = withIOContext {
        val src = urlBuilder.value
        val first = EhEngine.getFavorites(src.copy(jumpTo = null, prev = null, next = null).build())
        val firstPage = first.galleryInfoList
        if (firstPage.isEmpty()) return@withIOContext null

        // Single page — nothing else to fetch.
        if (first.next == null) return@withIOContext firstPage.random()

        // Multiple pages: fetch the last page to establish the date range.
        val last = runSuspendCatching {
            EhEngine.getFavorites(src.copy(jumpTo = null, prev = "1-0", next = null).build())
        }.getOrNull()

        // Try to pick a random middle page via date seeking.
        // The posted field on favorites is in "YYYY-MM-DD HH:mm" (favorite time),
        // same format ParserUtils.parseDate handles.
        val middle = last?.let { lastPage ->
            runSuspendCatching {
                val lastItems = lastPage.galleryInfoList
                if (lastItems.isEmpty()) return@runSuspendCatching null
                val firstTime = firstPage.first().posted?.let(ParserUtils::parseDate) ?: return@runSuspendCatching null
                val lastTime = lastItems.last().posted?.let(ParserUtils::parseDate) ?: return@runSuspendCatching null
                val (lo, hi) = if (firstTime < lastTime) firstTime to lastTime else lastTime to firstTime
                val randomMillis = (lo..hi).random()
                val dateStr = randomMillis.toLocalDateTime(TimeZone.UTC).date.toString()
                EhEngine.getFavorites(src.copy(jumpTo = dateStr, prev = null, next = null).build())
            }.getOrNull()
        }

        // Pick from the middle page (best), first/last 50/50 (good), or first (fallback).
        if (middle != null && middle.galleryInfoList.isNotEmpty()) {
            middle.galleryInfoList.random()
        } else if (last != null && Random.nextBoolean()) {
            last.galleryInfoList.random()
        } else {
            firstPage.random()
        }
    }
}

private const val DEFAULT_PAGE_SIZE = 50
