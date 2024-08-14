package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import androidx.collection.lruCache
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
                n ?: notifyPageWait(k)
                o.recycle()
            },
        )
    }

    val pages by lazy {
        check(size > 0)
        (0 until size).map { ReaderPage(it) }
    }

    abstract val preloadPageCount: Int

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
    }

    abstract val size: Int

    fun request(index: Int) {
        val image = cache[index]
        if (image != null) {
            notifyPageSucceed(index, image)
        } else {
            notifyPageWait(index)
            onRequest(index)
        }

        val preloadRange = (index - 3).coerceAtLeast(0) until (index + preloadPageCount).coerceAtMost(size)
        val pagesAbsent = preloadRange.mapNotNullTo(mutableListOf()) { it.takeIf { it != index && cache[it] == null } }
        // Load forward first, then load backward from the nearest index
        pagesAbsent.sortBy { (index - it).coerceAtLeast(0) }
        preloadPages(pagesAbsent, (preloadRange.first - 5).coerceAtLeast(0) to (preloadRange.last + 10).coerceAtMost(size))
    }

    fun retryPage(index: Int, orgImg: Boolean = false) {
        notifyPageWait(index)
        onForceRequest(index, orgImg)
    }

    protected abstract fun preloadPages(pages: List<Int>, bounds: Pair<Int, Int>)

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int, orgImg: Boolean)

    fun cancelRequest(index: Int) {
        onCancelRequest(index)
    }

    protected abstract fun onCancelRequest(index: Int)

    fun notifyPageWait(index: Int) {
        pages[index].status.value = Page.State.QUEUE
    }

    fun notifyPagePercent(index: Int, percent: Float) {
        pages[index].status.compareAndSet(Page.State.QUEUE, Page.State.DOWNLOAD_IMAGE)
        pages[index].progress = (percent * 100).toInt()
    }

    fun notifyPageSucceed(index: Int, image: Image) {
        if (cache[index] != image) {
            cache.put(index, image)
        }
        pages[index].image = image
        pages[index].status.value = Page.State.READY
    }

    fun notifyPageFailed(index: Int, error: String?) {
        pages[index].errorMsg = error
        pages[index].status.value = Page.State.ERROR
    }
}
