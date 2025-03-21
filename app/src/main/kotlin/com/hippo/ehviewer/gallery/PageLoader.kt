package com.hippo.ehviewer.gallery

import androidx.collection.SieveCache
import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.bracketCase
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.image.ImageSource
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.OSUtils
import com.hippo.ehviewer.util.detectAds
import com.hippo.ehviewer.util.displayString
import com.hippo.ehviewer.util.isAtLeastO
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import moe.tarsin.coroutines.NamedMutex
import moe.tarsin.coroutines.withLock
import okio.Path

private val progressScope = CoroutineScope(Dispatchers.IO)
private const val MAX_CACHE_SIZE = 512 * 1024 * 1024
private const val MIN_CACHE_SIZE = 256 * 1024 * 1024

abstract class PageLoader(val scope: CoroutineScope, val gid: Long, startPage: Int, val size: Int, val hasAds: Boolean = false) : AutoCloseable {
    var startPage = startPage.coerceIn(0, size - 1)

    private val mutex = NamedMutex<Int>()
    private val semaphore = Semaphore(4)

    private val cache = SieveCache<Int, Image>(
        maxSize = if (isAtLeastO) {
            (OSUtils.totalMemory / 8).toInt().coerceIn(MIN_CACHE_SIZE, MAX_CACHE_SIZE)
        } else {
            (OSUtils.appMaxMemory / 3 * 2).toInt()
        },
        sizeOf = { _, v -> v.allocationSize.toInt() },
        onEntryRemoved = { k, o, n, _ -> if (o.unpin()) n ?: notifyPageWait(k) },
    )

    fun decodePreloadRange(index: Int) = index - 3..index + 3

    fun needDecode(index: Int) = index in decodePreloadRange(prevIndex.load()) &&
        lock.read { index !in cache } &&
        pages[index].status !is PageStatus.Ready &&
        pages[index].status !is PageStatus.Blocked

    suspend fun atomicallyDecodeAndUpdate(index: Int) {
        if (!needDecode(index)) return
        try {
            bracketCase(
                { openSource(index) },
                { src -> notifyPageSucceed(index, Image.decode(src, hasAds && detectAds(index, size))) },
                { src, case -> if (case !is ExitCase.Completed) src.close() },
            )
        } catch (e: Throwable) {
            notifyPageFailed(index, e.displayString())
        }
    }

    private val lock = ReentrantReadWriteLock()

    val pages = (0 until size).map { Page(it) }

    private val prefetchPageCount = Settings.preloadImage

    fun restart() {
        lock.write { cache.evictAll() }
        pages.forEach(Page::reset)
    }

    private val prevIndex = AtomicInt(-1)

    fun retryPage(index: Int, orgImg: Boolean = false) {
        notifyPageWait(index)
        lock.write { cache.remove(index) }
        onRequest(index, true, orgImg)
    }

    protected abstract fun prefetchPages(pages: List<Int>, bounds: IntRange)

    protected abstract fun onRequest(index: Int, force: Boolean = false, orgImg: Boolean = false)

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
        if (replaceCache) lock.write { cache[index] = image }
        pages[index].statusFlow.update { if (image.hasQrCode) PageStatus.Blocked(image) else PageStatus.Ready(image) }
    }

    fun notifyPageFailed(index: Int, error: String?) {
        pages[index].statusFlow.update { PageStatus.Error(error) }
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

    fun request(index: Int) {
        val prefetchRange = if (index >= prevIndex.load()) {
            index + 1..(index + prefetchPageCount).coerceAtMost(size - 1)
        } else {
            index - 1 downTo (index - prefetchPageCount).coerceAtLeast(0)
        }
        prevIndex.store(index)
        val image = lock.read { cache[index] }
        if (image != null) {
            notifyPageSucceed(index, image, false)
        } else {
            notifyPageWait(index)
            onRequest(index)
        }

        // Prefetch to disk
        val pagesAbsent = prefetchRange.filter {
            when (pages[it].status) {
                PageStatus.Queued, is PageStatus.Error -> true
                else -> false
            }
        }
        val start = if (prefetchRange.step > 0) prefetchRange.first else prefetchRange.last
        val end = if (prefetchRange.step > 0) prefetchRange.last else prefetchRange.first
        prefetchPages(pagesAbsent, start - 5..end + 5)
    }

    abstract fun save(index: Int, file: Path): Boolean

    fun notifySourceReady(index: Int) {
        if (needDecode(index)) {
            scope.launch {
                mutex.withLock(index) {
                    semaphore.withPermit {
                        atomicallyDecodeAndUpdate(index)
                    }
                }
            }
        }
    }

    abstract fun openSource(index: Int): ImageSource
}
