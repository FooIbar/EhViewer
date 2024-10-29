package com.hippo.ehviewer.gallery

import androidx.collection.SieveCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.OSUtils
import com.hippo.ehviewer.util.isAtLeastO
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path

private val progressScope = CoroutineScope(Dispatchers.IO)
private const val MAX_CACHE_SIZE = 512 * 1024 * 1024
private const val MIN_CACHE_SIZE = 128 * 1024 * 1024

abstract class PageLoader(val gid: Long, var startPage: Int) : AutoCloseable {
    private val cache = SieveCache<Int, Image>(
        maxSize = if (isAtLeastO) {
            (OSUtils.totalMemory / 16).toInt().coerceIn(MIN_CACHE_SIZE, MAX_CACHE_SIZE)
        } else {
            (OSUtils.appMaxMemory / 3 * 2).toInt()
        },
        sizeOf = { _, v -> v.allocationSize.toInt() },
        onEntryRemoved = { k, o, n, _ ->
            if (o.isRecyclable) {
                n ?: notifyPageWait(k)
                o.recycle()
            } else {
                o.isRecyclable = true
            }
        },
    )

    private val lock = ReentrantReadWriteLock()

    val pages by lazy {
        check(size > 0)
        (0 until size).map { Page(it) }
    }

    private val prefetchPageCount = Settings.preloadImage

    fun restart() {
        lock.write { cache.evictAll() }
        pages.forEach(Page::reset)
    }

    abstract val size: Int

    private var lastRequestIndex = -1

    fun request(index: Int) {
        val image = lock.read { cache[index] }
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
        notifyPageWait(index)
        onForceRequest(index, orgImg)
    }

    protected abstract fun prefetchPages(pages: List<Int>, bounds: Pair<Int, Int>)

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int, orgImg: Boolean)

    fun cancelRequest(index: Int) {
        onCancelRequest(index)
    }

    protected abstract fun onCancelRequest(index: Int)

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
            lock.write { cache[index] = image }
        }
        pages[index].statusFlow.update { if (image.hasQrCode) PageStatus.Blocked(image) else PageStatus.Ready(image) }
    }

    fun notifyPageFailed(index: Int, error: String?) {
        pages[index].statusFlow.update { PageStatus.Error(error) }
    }

    val progressJob = progressScope.launch {
        if (startPage == -1) {
            startPage = EhDB.getReadProgress(gid)
        }
    }

    override fun close() {
        lock.write { cache.evictAll() }
        if (gid != 0L) {
            progressScope.launch {
                EhDB.putReadProgress(gid, startPage)
            }
        }
    }

    protected abstract val title: String

    protected abstract fun getImageExtension(index: Int): String?

    fun getImageFilename(index: Int): String? = getImageExtension(index)?.let {
        FileUtils.sanitizeFilename("$title - ${index + 1}.${it.lowercase()}")
    }

    abstract fun save(index: Int, file: Path): Boolean
}
