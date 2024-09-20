package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import androidx.collection.lruCache
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.gallery.Page
import com.hippo.ehviewer.gallery.PageStatus
import com.hippo.ehviewer.gallery.reset
import com.hippo.ehviewer.gallery.status
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.util.OSUtils
import com.hippo.ehviewer.util.isAtLeastO
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val MAX_CACHE_SIZE = 512 * 1024 * 1024
private const val MIN_CACHE_SIZE = 128 * 1024 * 1024

abstract class PageLoader {
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
                    n ?: notifyPageWait(k)
                    o.recycle()
                } else {
                    o.isRecyclable = true
                }
            },
        )
    }

    val pages by lazy {
        check(size > 0)
        (0 until size).map { Page(it) }
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
        pages.forEach(Page::reset)
    }

    abstract val size: Int

    private var lastRequestIndex = -1

    fun request(index: Int) {
        val image = cache[index]
        if (image != null) {
            notifyPageSucceed(index, image, false)
        } else {
            notifyPageWait(index)
            onRequest(index)
        }

        // Prefetch to disk
        val prefetchRange = if (index >= lastRequestIndex) {
            index + 1..(index + prefetchPageCount).coerceAtMost(size - 1)
        } else {
            index - 1 downTo (index - prefetchPageCount).coerceAtLeast(0)
        }
        val pagesAbsent = prefetchRange.filter { pages[it].status == PageStatus.Queued }
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
        notifyPageWait(index)
        onForceRequest(index, orgImg)
    }

    protected abstract fun prefetchPages(pages: List<Int>, bounds: Pair<Int, Int>)

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int, orgImg: Boolean)

    fun notifyPageWait(index: Int) {
        pages[index].reset()
    }

    fun notifyPagePercent(index: Int, percent: Float) {
        pages[index].statusFlow.update {
            when (it) {
                is PageStatus.Loading -> it.apply { progress.update { percent } }
                else -> PageStatus.Loading(MutableStateFlow(percent))
            }
        }
    }

    fun notifyPageSucceed(index: Int, image: Image, replaceCache: Boolean = true) {
        if (replaceCache) {
            cache.put(index, image)
        }
        pages[index].statusFlow.update { if (image.hasQrCode) PageStatus.Blocked(image) else PageStatus.Ready(image) }
    }

    fun notifyPageFailed(index: Int, error: String?) {
        pages[index].statusFlow.update { PageStatus.Error(error) }
    }
}
