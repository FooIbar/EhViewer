package com.hippo.ehviewer.gallery

import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.resourceScope
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.obtainSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.releaseSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import okio.Path

suspend fun <T> useEhPageLoader(
    info: GalleryInfo,
    startPage: Int,
    block: suspend (PageLoader) -> T,
) = resourceScope {
    val queen = install(
        { obtainSpiderQueen(info, SpiderQueen.MODE_READ) },
        { queen, _ -> releaseSpiderQueen(queen, SpiderQueen.MODE_READ) },
    )
    check(queen.awaitReady())
    val loader = autoCloseable {
        object : PageLoader(info.gid, startPage) {
            val listener = object : OnSpiderListener {
                override fun onGetPages(pages: Int) = Unit

                override fun onGet509(index: Int) = Unit

                override fun onPageDownload(index: Int, contentLength: Long, receivedSize: Long, bytesRead: Int) {
                    if (contentLength > 0) {
                        notifyPagePercent(index, receivedSize.toFloat() / contentLength)
                    }
                }

                override fun onPageSuccess(index: Int, finished: Int, downloaded: Int, total: Int) = Unit

                override fun onPageFailure(index: Int, error: String?, finished: Int, downloaded: Int, total: Int) = notifyPageFailed(index, error)

                override fun onFinish(finished: Int, downloaded: Int, total: Int) = Unit

                override fun onGetImageSuccess(index: Int, image: Image?) = notifyPageSucceed(index, image!!)

                override fun onGetImageFailure(index: Int, error: String?) = notifyPageFailed(index, error)
            }.apply { queen.addOnSpiderListener(this) }

            override fun close() {
                super.close()
                queen.removeOnSpiderListener(listener)
            }

            override val title by lazy { EhUtils.getSuitableTitle(info) }

            override fun getImageExtension(index: Int) = queen.getExtension(index)

            override fun save(index: Int, file: Path) = queen.save(index, file)

            override val size = queen.size

            override fun prefetchPages(pages: List<Int>, bounds: Pair<Int, Int>) = queen.preloadPages(pages, bounds)

            override fun onRequest(index: Int) = queen.request(index)

            override fun onForceRequest(index: Int, orgImg: Boolean) = queen.forceRequest(index, orgImg)

            override fun onCancelRequest(index: Int) = queen.cancelRequest(index)
        }
    }
    loader.progressJob.join()
    block(loader)
}
