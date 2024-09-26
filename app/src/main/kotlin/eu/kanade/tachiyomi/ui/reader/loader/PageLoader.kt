package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import androidx.collection.lruCache
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.gallery.Page
import com.hippo.ehviewer.gallery.PageStatus
import com.hippo.ehviewer.gallery.status
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.util.OSUtils
import com.hippo.ehviewer.util.isAtLeastO
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MAX_CACHE_SIZE = 512 * 1024 * 1024
private const val MIN_CACHE_SIZE = 128 * 1024 * 1024

abstract class PageLoader : CoroutineScope {
    private val cache by lazy {
        lruCache<Int, Image>(
            maxSize = if (isAtLeastO) {
                (OSUtils.totalMemory / 16).toInt().coerceIn(MIN_CACHE_SIZE, MAX_CACHE_SIZE)
            } else {
                (OSUtils.appMaxMemory / 3 * 2).toInt()
            },
            sizeOf = { _, v -> v.allocationSize.toInt() },
            onEntryRemoved = { _, k, o, n ->
                if (o.isRecyclable) {
                    n ?: queuePage(k)
                    o.recycle()
                } else {
                    o.isRecyclable = true
                }
            },
        )
    }

    protected abstract val loaderEvent: SharedFlow<PageEvent>

    private val broadcast = MutableSharedFlow<PageEvent>()

    val pages by lazy {
        check(size > 0)
        launch {
            loaderEvent.collect {
                if (it is PageEvent.Success) {
                    cache.put(it.index, it.image)
                }
            }
        }
        (0 until size).map { index ->
            Page(
                index,
                merge(loaderEvent, broadcast).filter { it.index == index }.map { event ->
                    when (event) {
                        is PageEvent.Error -> PageStatus.Error(event.error)
                        is PageEvent.Downloading -> PageStatus.Loading(event.progress.stateIn(this, SharingStarted.Eagerly, 0f))
                        is PageEvent.Success -> with(event.image) { if (hasQrCode) PageStatus.Blocked(this) else PageStatus.Ready(this) }
                        is PageEvent.Queued -> PageStatus.Queued
                    }
                }.stateIn(this, SharingStarted.Eagerly, PageStatus.Queued),
            )
        }
    }

    private val prefetchPageCount = Settings.preloadImage

    @CallSuper
    open suspend fun awaitReady(): Boolean {
        withIOContext { cache }
        return true
    }

    abstract val isReady: Boolean

    @CallSuper
    abstract fun start()

    @CallSuper
    open fun stop() {
        cache.evictAll()
    }

    fun restart() {
        cache.evictAll()
        pages.forEach { queuePage(it.index) }
    }

    abstract val size: Int

    private var lastRequestIndex = -1

    fun request(index: Int) {
        val image = cache[index]
        if (image != null) {
            launch(Dispatchers.Unconfined) { broadcast.emit(PageEvent.Success(index, image)) }
        } else {
            queuePage(index)
            onRequest(index)
        }

        // Prefetch to disk
        val prefetchRange = if (index >= lastRequestIndex) {
            index + 1..(index + prefetchPageCount).coerceAtMost(size - 1)
        } else {
            index - 1 downTo (index - prefetchPageCount).coerceAtLeast(0)
        }
        val pagesAbsent = prefetchRange.filter {
            when (pages[it].status) {
                PageStatus.Queued, is PageStatus.Error -> true
                else -> false
            }
        }
        val start = if (prefetchRange.step > 0) prefetchRange.first else prefetchRange.last
        val end = if (prefetchRange.step > 0) prefetchRange.last else prefetchRange.first
        prefetchPages(pagesAbsent, start - 5 to end + 5)

        // Prefetch to memory
        val range = index - 3..index + 3
        pagesAbsent.forEach {
            if (it in range) onRequest(it)
        }

        lastRequestIndex = index
    }

    fun retryPage(index: Int, orgImg: Boolean = false) {
        queuePage(index)
        onForceRequest(index, orgImg)
    }

    protected abstract fun prefetchPages(pages: List<Int>, bounds: Pair<Int, Int>)

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int, orgImg: Boolean)

    fun queuePage(index: Int) = launch(Dispatchers.Unconfined) { broadcast.emit(PageEvent.Queued(index)) }

    fun unblockPage(page: Page) {
        launch {
            broadcast.emit(
                PageEvent.Success(
                    page.index,
                    when (val status = page.status) {
                        is PageStatus.Blocked -> status.ad.apply { hasQrCode = false }
                        else -> error("Call unblock on page not blocked!!!")
                    },
                ),
            )
        }
    }
}

sealed interface PageEvent {
    val index: Int

    data class Error(override val index: Int, val error: String?) : PageEvent
    data class Success(override val index: Int, val image: Image) : PageEvent
    data class Downloading(override val index: Int, val progress: Flow<Float>) : PageEvent
    data class Queued(override val index: Int) : PageEvent
}
