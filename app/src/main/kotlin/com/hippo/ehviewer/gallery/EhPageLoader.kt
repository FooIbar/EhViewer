/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.gallery

import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.obtainSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.releaseSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import okio.Path

class EhPageLoader(private val mGalleryInfo: GalleryInfo, startPage: Int) :
    PageLoader2(mGalleryInfo.gid, startPage),
    OnSpiderListener {
    private lateinit var mSpiderQueen: SpiderQueen
    override fun start() {
        super.start()
        if (!::mSpiderQueen.isInitialized) {
            mSpiderQueen = obtainSpiderQueen(mGalleryInfo, SpiderQueen.MODE_READ)
            mSpiderQueen.addOnSpiderListener(this)
        }
    }

    override fun stop() {
        super.stop()
        mSpiderQueen.removeOnSpiderListener(this)
        releaseSpiderQueen(mSpiderQueen, SpiderQueen.MODE_READ)
    }

    override val title by lazy {
        EhUtils.getSuitableTitle(mGalleryInfo)
    }

    override fun getImageExtension(index: Int): String? = mSpiderQueen.getExtension(index)

    override fun save(index: Int, file: Path): Boolean = mSpiderQueen.save(index, file)

    override val size: Int
        get() = mSpiderQueen.size

    override fun onRequest(index: Int) {
        mSpiderQueen.request(index)
    }

    override fun onForceRequest(index: Int, orgImg: Boolean) {
        mSpiderQueen.forceRequest(index, orgImg)
    }

    override suspend fun awaitReady(): Boolean = super.awaitReady() && mSpiderQueen.awaitReady()

    override val isReady: Boolean
        get() = ::mSpiderQueen.isInitialized && mSpiderQueen.isReady

    override fun onCancelRequest(index: Int) {
        mSpiderQueen.cancelRequest(index)
    }

    override fun onGetPages(pages: Int) {}

    override fun onGet509(index: Int) {}

    override fun onPageDownload(
        index: Int,
        contentLength: Long,
        receivedSize: Long,
        bytesRead: Int,
    ) {
        if (contentLength > 0) {
            notifyPagePercent(index, receivedSize.toFloat() / contentLength)
        }
    }

    override fun onPageSuccess(index: Int, finished: Int, downloaded: Int, total: Int) {}
    override fun onPageFailure(
        index: Int,
        error: String?,
        finished: Int,
        downloaded: Int,
        total: Int,
    ) {
        notifyPageFailed(index, error)
    }

    override fun onFinish(finished: Int, downloaded: Int, total: Int) {}
    override fun onGetImageSuccess(index: Int, image: Image?) {
        notifyPageSucceed(index, image!!)
    }

    override fun onGetImageFailure(index: Int, error: String?) {
        notifyPageFailed(index, error)
    }

    override fun preloadPages(pages: List<Int>, pair: Pair<Int, Int>) {
        mSpiderQueen.preloadPages(pages, pair)
    }
}
