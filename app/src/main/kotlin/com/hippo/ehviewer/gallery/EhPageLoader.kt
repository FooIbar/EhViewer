package com.hippo.ehviewer.gallery

import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.obtainSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.releaseSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import okio.Path

suspend fun newEhPageLoader(
    info: GalleryInfo,
    startPage: Int,
): PageLoader2 {
    val spiderQueen = obtainSpiderQueen(info, SpiderQueen.Companion.MODE_READ)
    check(spiderQueen.awaitReady())
    return object : PageLoader2(info.gid, startPage) {
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
        }.apply {
            spiderQueen.addOnSpiderListener(this)
        }

        override fun close() {
            super.close()
            spiderQueen.removeOnSpiderListener(listener)
            releaseSpiderQueen(spiderQueen, SpiderQueen.MODE_READ)
        }

        override val title by lazy { EhUtils.getSuitableTitle(info) }

        override fun getImageExtension(index: Int) = spiderQueen.getExtension(index)

        override fun save(index: Int, file: Path) = spiderQueen.save(index, file)

        override val size = spiderQueen.size

        override fun prefetchPages(pages: List<Int>, bounds: Pair<Int, Int>) = spiderQueen.preloadPages(pages, bounds)

        override fun onRequest(index: Int) = spiderQueen.request(index)

        override fun onForceRequest(index: Int, orgImg: Boolean) = spiderQueen.forceRequest(index, orgImg)

        override fun onCancelRequest(index: Int) = spiderQueen.cancelRequest(index)
    }
}
