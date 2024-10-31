package com.hippo.ehviewer.gallery

import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.resourceScope
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.obtainSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.releaseSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import com.hippo.ehviewer.ui.screen.implicit
import com.hippo.ehviewer.util.hasAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import okio.Path

suspend fun <T> useEhPageLoader(
    info: GalleryInfo,
    startPage: Int,
    block: suspend (PageLoader) -> T,
) = coroutineScope {
    resourceScope {
        val queen = install(
            { obtainSpiderQueen(info, SpiderQueen.MODE_READ) },
            { queen, _ -> releaseSpiderQueen(queen, SpiderQueen.MODE_READ) },
        )
        queen.awaitReady()
        val loader = autoCloseable {
            object : PageLoader(info.gid, startPage, queen.size, info.hasAds, implicit<CoroutineScope>()) {
                override val title by lazy { EhUtils.getSuitableTitle(info) }

                override fun getImageExtension(index: Int) = queen.getExtension(index)

                override fun save(index: Int, file: Path) = queen.save(index, file)

                override fun openSource(index: Int) = queen.spiderDen.getImageSource(index)

                override fun prefetchPages(pages: List<Int>, bounds: IntRange) = queen.preloadPages(pages, bounds)

                override fun onRequest(index: Int, force: Boolean, orgImg: Boolean) = queen.request(index, force, orgImg)
            }
        }.apply {
            val listener = object : OnSpiderListener {
                override fun onPageDownload(index: Int, contentLength: Long, receivedSize: Long, bytesRead: Int) {
                    if (contentLength > 0) {
                        notifyPagePercent(index, receivedSize.toFloat() / contentLength)
                    }
                }

                override fun onPageSuccess(index: Int, finished: Int, downloaded: Int, total: Int) = notifySourceReady(index)

                override fun onPageFailure(index: Int, error: String?, finished: Int, downloaded: Int, total: Int) = notifyPageFailed(index, error)
            }
            install(
                { queen.addOnSpiderListener(listener) },
                { _, _ -> queen.removeOnSpiderListener(listener) },
            )
        }
        loader.progressJob.join()
        block(loader)
    }
}
