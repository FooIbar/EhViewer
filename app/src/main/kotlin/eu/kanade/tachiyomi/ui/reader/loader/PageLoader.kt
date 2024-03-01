package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import androidx.collection.lruCache
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.util.OSUtils
import com.hippo.ehviewer.util.isAtLeastO
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

private const val MAX_CACHE_SIZE = 512 * 1024 * 1024
private const val MIN_CACHE_SIZE = 128 * 1024 * 1024

abstract class PageLoader {

    private val cache by lazy {
        lruCache<ReaderPage, Image>(
            maxSize = if (isAtLeastO) {
                (OSUtils.totalMemory / 16).toInt().coerceIn(MIN_CACHE_SIZE, MAX_CACHE_SIZE)
            } else {
                (OSUtils.appMaxMemory / 3 * 2).toInt()
            },
            sizeOf = { _, v -> v.size.toInt() },
            onEntryRemoved = { _, k, o, _ ->
                k.status.value = Page.State.QUEUE
                o.recycle()
            },
        )
    }
    val pages by lazy {
        check(size > 0)
        (0 until size).map { ReaderPage(it) }
    }

    private val preloads = com.hippo.ehviewer.Settings.preloadImage.coerceIn(0, 100)

    abstract suspend fun awaitReady(): Boolean
    abstract val isReady: Boolean

    @CallSuper
    open fun start() {
        cache
    }

    @CallSuper
    open fun stop() {
        cache.evictAll()
    }

    fun restart() {
        cache.evictAll()
    }

    abstract val size: Int

    fun request(index: Int) {
        val image = cache[pages[index]]
        if (image != null) {
            notifyPageSucceed(index, image)
        } else {
            notifyPageWait(index)
            onRequest(index)
        }

        val pagesAbsent = ((index - 5).coerceAtLeast(0) until (preloads + index).coerceAtMost(size)).mapNotNull { it.takeIf { cache[pages[it]] == null } }
        preloadPages(pagesAbsent, (index - 10).coerceAtLeast(0) to (preloads + index + 10).coerceAtMost(size))
    }

    fun retryPage(index: Int, orgImg: Boolean = false) {
        notifyPageWait(index)
        onForceRequest(index, orgImg)
    }

    protected abstract fun preloadPages(pages: List<Int>, pair: Pair<Int, Int>)

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
        if (cache[pages[index]] != image) {
            cache.put(pages[index], image)
        }
        pages[index].image = image
        pages[index].status.value = Page.State.READY
    }

    fun notifyPageFailed(index: Int, error: String?) {
        pages[index].errorMsg = error
        pages[index].status.value = Page.State.ERROR
    }
}
