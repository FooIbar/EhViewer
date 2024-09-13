package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import androidx.collection.lruCache
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.util.OSUtils
import com.hippo.ehviewer.util.isAtLeastO
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.lang.withIOContext

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
            sizeOf = { _, v -> v.size.toInt() },
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
        (0 until size).map { ReaderPage(it) }
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
        pages.forEach(ReaderPage::reset)
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
        val pagesAbsent = prefetchRange.filter { pages[it].status.value == Page.State.QUEUE }
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
        pages[index].progress = (percent * 100).toInt()
        pages[index].status.compareAndSet(Page.State.QUEUE, Page.State.DOWNLOAD_IMAGE)
    }

    fun notifyPageSucceed(index: Int, image: Image, replaceCache: Boolean = true) {
        if (replaceCache) {
            cache.put(index, image)
        }
        pages[index].image = image
        pages[index].status.value = if (image.hasQrCode) Page.State.BLOCKED else Page.State.READY
    }

    fun notifyPageFailed(index: Int, error: String?) {
        pages[index].errorMsg = error
        pages[index].status.value = Page.State.ERROR
    }
}

private fun ReaderPage.reset() {
    image = null
    errorMsg = null
    progress = 0
    status.value = Page.State.QUEUE
}
