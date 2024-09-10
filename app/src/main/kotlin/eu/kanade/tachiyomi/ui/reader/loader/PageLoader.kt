package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import androidx.collection.SieveCache
import androidx.compose.runtime.toMutableStateList
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
        SieveCache<ReaderPage, Image>(
            maxSize = if (isAtLeastO) {
                (OSUtils.totalMemory / 16).toInt().coerceIn(MIN_CACHE_SIZE, MAX_CACHE_SIZE)
            } else {
                (OSUtils.appMaxMemory / 3 * 2).toInt()
            },
            sizeOf = { _, v -> v.size.toInt() },
            onEntryRemoved = { k, o, n, _ ->
                if (o.isRecyclable) {
                    n ?: notifyPageWait(k.index)
                    o.recycle()
                } else {
                    o.isRecyclable = true
                }
            },
        )
    }

    private val storage by lazy {
        check(internalSize > 0)
        (0 until internalSize).map { ReaderPage(it) }.let { pages ->
            val tracker = pages.associateBy { it.index }
            val stateList = pages.toMutableStateList()
            tracker to stateList
        }
    }

    private val trackMap
        get() = storage.first

    private val stateList
        get() = storage.second

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
        stateList.forEach(ReaderPage::reset)
    }

    protected abstract val internalSize: Int

    operator fun get(index: Int): ReaderPage = stateList[index]

    val size: Int
        get() = stateList.size

    private var lastRequestIndex = -1

    fun request(page: ReaderPage) {
        val index = page.index
        val image = cache[page]
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
        val pagesAbsent = prefetchRange.filter { stateList[it].status.value == Page.State.QUEUE }
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

    fun retryPage(page: ReaderPage, orgImg: Boolean = false) {
        val index = page.index
        notifyPageWait(index)
        onForceRequest(index, orgImg)
    }

    private val Int.page
        get() = trackMap[this]!!

    // Following accepts real index!!!

    protected abstract fun prefetchPages(pages: List<Int>, bounds: Pair<Int, Int>)

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int, orgImg: Boolean)

    fun notifyPageWait(index: Int) {
        index.page.reset()
    }

    fun notifyPagePercent(index: Int, percent: Float) {
        val page = index.page
        page.progress = (percent * 100).toInt()
        page.status.compareAndSet(Page.State.QUEUE, Page.State.DOWNLOAD_IMAGE)
    }

    fun notifyPageSucceed(index: Int, image: Image, replaceCache: Boolean = true) {
        val page = index.page
        if (image.hasQRCode) {
            stateList.remove(page)
        } else {
            if (replaceCache) {
                cache[page] = image
            }
            page.image = image
            page.status.value = Page.State.READY
        }
    }

    fun notifyPageFailed(index: Int, error: String?) {
        val page = index.page
        page.errorMsg = error
        page.status.value = Page.State.ERROR
    }
}

private fun ReaderPage.reset() {
    image = null
    errorMsg = null
    progress = 0
    status.value = Page.State.QUEUE
}
