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

    protected abstract val sourceFlow: SharedFlow<PageEvent>

    private val broadcast = MutableSharedFlow<Pair<Int, PageStatus>>()

    val pages by lazy {
        check(size > 0)
        (0 until size).map { index ->
            val flow = sourceFlow.filter { it.index == index }.map { event ->
                when (event) {
                    is PageEvent.Error -> PageStatus.Error(event.error)
                    is PageEvent.Progress -> PageStatus.Loading(event.progress.stateIn(this, SharingStarted.Eagerly, 0f))
                    is PageEvent.Success -> event.image.let {
                        cache.put(index, it)
                        if (it.hasQrCode) PageStatus.Blocked(it) else PageStatus.Ready(it)
                    }
                    is PageEvent.Wait -> PageStatus.Queued
                }
            }
            val overwrite = broadcast.filter { (i, _) -> index == i }.map { (_, e) -> e }
            Page(index, merge(flow, overwrite).stateIn(this, SharingStarted.Eagerly, PageStatus.Queued))
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
            launch { broadcast.emit(index to PageStatus.Ready(image)) }
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

    fun queuePage(index: Int) = launch { broadcast.emit(index to PageStatus.Queued) }

    fun unblockPage(page: Page) {
        launch {
            broadcast.emit(
                page.index to when (val status = page.status) {
                    is PageStatus.Blocked -> PageStatus.Ready(status.ad)
                    else -> error("Call unblock on page not blocked!!!")
                },
            )
        }
    }
}

sealed interface PageEvent {
    val index: Int

    data class Error(override val index: Int, val error: String?) : PageEvent
    data class Success(override val index: Int, val image: Image) : PageEvent
    data class Progress(override val index: Int, val progress: Flow<Float>) : PageEvent
    data class Wait(override val index: Int) : PageEvent
}
