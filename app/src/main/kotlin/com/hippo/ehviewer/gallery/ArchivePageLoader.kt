/*
 * Copyright 2023 Tarsin Norbin
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

import android.os.ParcelFileDescriptor
import com.hippo.ehviewer.Settings.archivePasswds
import com.hippo.ehviewer.image.ByteBufferSource
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.jni.closeArchive
import com.hippo.ehviewer.jni.extractToByteBuffer
import com.hippo.ehviewer.jni.extractToFd
import com.hippo.ehviewer.jni.getExtension
import com.hippo.ehviewer.jni.needPassword
import com.hippo.ehviewer.jni.openArchive
import com.hippo.ehviewer.jni.providePassword
import com.hippo.ehviewer.jni.releaseByteBuffer
import com.hippo.ehviewer.util.FileUtils
import com.hippo.unifile.UniFile
import com.hippo.unifile.displayPath
import eu.kanade.tachiyomi.util.system.logcat
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import moe.tarsin.coroutines.NamedMutex
import moe.tarsin.coroutines.withLock

typealias PasswdInvalidator = (String) -> Boolean
typealias PasswdProvider = suspend (PasswdInvalidator) -> String

class ArchivePageLoader(
    private val file: UniFile,
    gid: Long = 0,
    startPage: Int = 0,
    passwdProvider: PasswdProvider? = null,
) : PageLoader2(gid, startPage) {
    private lateinit var pfd: ParcelFileDescriptor
    private val hostJob = launch(start = CoroutineStart.LAZY) {
        logcat(DEBUG_TAG) { "Open archive ${file.uri.displayPath}" }
        pfd = file.openFileDescriptor("r")
        size = openArchive(pfd.fd, pfd.statSize)
        if (size == 0) {
            return@launch
        }
        if (passwdProvider != null && needPassword()) {
            archivePasswds?.forEach {
                it ?: return@forEach
                if (providePassword(it)) return@launch
            }
            val toAdd = passwdProvider { providePassword(it) }
            archivePasswds = archivePasswds?.toMutableSet()?.apply { add(toAdd) } ?: setOf(toAdd)
        }
    }

    override var size = 0
        private set

    override fun start() {
        super.start()
        hostJob.start()
    }

    override fun stop() {
        super.stop()
        closeArchive()
        pfd.close()
        logcat(DEBUG_TAG) { "Close archive ${file.uri.displayPath} successfully!" }
    }

    private val mJobMap = hashMapOf<Int, Job>()
    private val mWorkerMutex = NamedMutex<Int>()
    private val mSemaphore = Semaphore(4)

    override fun onRequest(index: Int) {
        synchronized(mJobMap) {
            val current = mJobMap[index]
            if (current?.isActive != true) {
                mJobMap[index] = launch {
                    mWorkerMutex.withLock(index) {
                        mSemaphore.withPermit {
                            doRealWork(index)
                        }
                    }
                }
            }
        }
    }

    private suspend fun doRealWork(index: Int) {
        val buffer = extractToByteBuffer(index)
        buffer ?: return
        check(buffer.isDirect)
        val src = object : ByteBufferSource {
            override val source: ByteBuffer = buffer

            override fun close() {
                releaseByteBuffer(buffer)
            }
        }
        runCatching {
            currentCoroutineContext().ensureActive()
        }.onFailure {
            src.close()
            throw it
        }
        val image = Image.decode(src) ?: return notifyPageFailed(index, null)
        runCatching {
            currentCoroutineContext().ensureActive()
        }.onFailure {
            image.recycle()
            throw it
        }
        notifyPageSucceed(index, image)
    }

    override fun onForceRequest(index: Int, orgImg: Boolean) {
        onRequest(index)
    }

    override suspend fun awaitReady(): Boolean {
        hostJob.join()
        return super.awaitReady() && size != 0
    }

    override val isReady: Boolean
        get() = size != 0

    override fun onCancelRequest(index: Int) {
        mJobMap[index]?.cancel()
    }

    override val title by lazy {
        FileUtils.getNameFromFilename(file.name)!!
    }

    override fun getImageExtension(index: Int): String {
        return getExtension(index)
    }

    override fun save(index: Int, file: UniFile): Boolean {
        return runCatching {
            file.openFileDescriptor("w").use {
                extractToFd(index, it.fd)
            }
        }.getOrElse {
            logcat(it)
            false
        }
    }

    override fun preloadPages(pages: List<Int>, pair: Pair<Int, Int>) {}
}

private const val DEBUG_TAG = "ArchivePageLoader"
