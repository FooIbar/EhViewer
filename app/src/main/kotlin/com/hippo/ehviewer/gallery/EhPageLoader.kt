package com.hippo.ehviewer.gallery

import arrow.autoCloseScope
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.obtainSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.releaseSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import com.hippo.ehviewer.util.hasAds
import kotlinx.coroutines.coroutineScope
import moe.tarsin.kt.install
import okio.Path

suspend inline fun <T> useEhPageLoader(
    info: GalleryInfo,
    startPage: Int,
    crossinline block: suspend (PageLoader) -> T,
) = autoCloseScope {
    coroutineScope {
        val queen = install(
            { obtainSpiderQueen(info, SpiderQueen.MODE_READ) },
            { queen, _ -> releaseSpiderQueen(queen, SpiderQueen.MODE_READ) },
        )
        queen.awaitReady()
        val loader = install(
            object : PageLoader(this, info.gid, startPage, queen.size, info.hasAds) {
                override val title by lazy { EhUtils.getSuitableTitle(info) }

                override fun getImageExtension(index: Int) = queen.getExtension(index)

                override fun save(index: Int, file: Path) = queen.save(index, file)

                override fun openSource(index: Int) = queen.spiderDen.getImageSource(index)

                override fun prefetchPages(pages: List<Int>, bounds: IntRange) = queen.preloadPages(pages, bounds)

                override fun onRequest(index: Int, force: Boolean, orgImg: Boolean) = queen.request(index, force, orgImg)
            },
        ).apply {
            val listener = object : OnSpiderListener {
                override fun onPageDownload(index: Int, contentLength: Long, receivedSize: Long, bytesRead: Int) {
                    if (contentLength > 0) {
                        notifyPagePercent(index, receivedSize.toFloat() / contentLength)
                    }
                }

                override fun onPageReady(index: Int) = notifySourceReady(index)

                override fun onPageFailure(index: Int, error: String?, finished: Int, downloaded: Int, total: Int) = notifyPageFailed(index, error)
            }
            install(
                { queen.addOnSpiderListener(listener) },
                { _, _ -> queen.removeOnSpiderListener(listener) },
            )
        }
        block(loader)
    }
}
