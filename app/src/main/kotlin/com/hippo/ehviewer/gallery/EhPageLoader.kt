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
import eu.kanade.tachiyomi.ui.reader.loader.PageEvent
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import okio.Path

class EhPageLoader(private val mGalleryInfo: GalleryInfo, startPage: Int) : PageLoader2(mGalleryInfo.gid, startPage) {
    override lateinit var loaderEvent: SharedFlow<PageEvent>
    private lateinit var spiderQueen: SpiderQueen
    override fun start() {
        super.start()
        if (!::spiderQueen.isInitialized) {
            spiderQueen = obtainSpiderQueen(mGalleryInfo, SpiderQueen.MODE_READ)
            loaderEvent = callbackFlow {
                val listener = object : SpiderQueen.OnSpiderListener {
                    override suspend fun onPageDownload(index: Int, contentLength: Long, bytesReceived: Flow<Long>) {
                        if (contentLength > 0) {
                            send(PageEvent.Downloading(index, bytesReceived.map { it.toFloat() / contentLength }))
                        }
                    }

                    override suspend fun onPageFailure(index: Int, error: String?, finished: Int, downloaded: Int, total: Int) = send(PageEvent.Error(index, error))

                    override suspend fun onGetImageSuccess(index: Int, image: Image) = send(PageEvent.Success(index, image))

                    override suspend fun onGetImageFailure(index: Int, error: String) = send(PageEvent.Error(index, error))
                }
                spiderQueen.addOnSpiderListener(listener)
                awaitClose {
                    launch(NonCancellable) {
                        spiderQueen.removeOnSpiderListener(listener)
                    }
                }
            }.shareIn(this, SharingStarted.Eagerly)
        }
    }

    override fun stop() {
        super.stop()
        releaseSpiderQueen(spiderQueen, SpiderQueen.MODE_READ)
    }

    override val title by lazy {
        EhUtils.getSuitableTitle(mGalleryInfo)
    }

    override fun getImageExtension(index: Int): String? = spiderQueen.getExtension(index)

    override fun save(index: Int, file: Path): Boolean = spiderQueen.save(index, file)

    override val size: Int
        get() = spiderQueen.size

    override fun onRequest(index: Int) {
        spiderQueen.request(index)
    }

    override fun onForceRequest(index: Int, orgImg: Boolean) {
        spiderQueen.forceRequest(index, orgImg)
    }

    override suspend fun awaitReady(): Boolean = super.awaitReady() && spiderQueen.awaitReady()

    override val isReady: Boolean
        get() = ::spiderQueen.isInitialized && spiderQueen.isReady

    override fun prefetchPages(pages: List<Int>, bounds: Pair<Int, Int>) {
        spiderQueen.preloadPages(pages, bounds)
    }
}
