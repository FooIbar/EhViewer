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

import androidx.annotation.CallSuper
import com.hippo.ehviewer.EhDB
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

abstract class PageLoader2(private val gid: Long, var startPage: Int) : PageLoader(), CoroutineScope {
    override val coroutineContext = Dispatchers.IO + Job()
    private val progressJob = launch(start = CoroutineStart.LAZY) {
        if (startPage == -1) {
            startPage = EhDB.getReadProgress(gid)
        }
    }

    override fun start() {
        super.start()
        progressJob.start()
    }

    @CallSuper
    override suspend fun awaitReady(): Boolean {
        progressJob.join()
        return startPage != -1
    }

    override fun stop() {
        super.stop()
        if (gid != 0L) {
            launch {
                EhDB.putReadProgress(gid, startPage)
                this@PageLoader2.cancel()
            }
        } else {
            cancel()
        }
    }

    protected abstract val title: String

    protected abstract fun getImageExtension(index: Int): String

    fun getImageFilename(index: Int): String {
        return "$title - ${index + 1}.${getImageExtension(index)}"
    }

    abstract fun save(index: Int, file: UniFile): Boolean

    fun saveToDir(index: Int, dir: UniFile): UniFile? {
        val filename = getImageFilename(index)
        return (dir / filename).takeIf { save(index, it) }
    }
}
