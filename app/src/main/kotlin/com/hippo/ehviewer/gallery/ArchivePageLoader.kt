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

import androidx.collection.mutableIntObjectMapOf
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
import com.hippo.ehviewer.util.detectAds
import com.hippo.ehviewer.util.displayPath
import com.hippo.files.openFileDescriptor
import com.hippo.files.toUri
import eu.kanade.tachiyomi.util.system.logcat
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import moe.tarsin.coroutines.NamedMutex
import moe.tarsin.coroutines.withLock
import okio.Path

typealias PasswdInvalidator = (String) -> Boolean
typealias PasswdProvider = suspend (PasswdInvalidator) -> String

suspend fun CoroutineScope.newArchivePageLoader(
    file: Path,
    gid: Long = 0,
    startPage: Int = 0,
    hasAds: Boolean = false,
    passwdProvider: PasswdProvider? = null,
): PageLoader2 {
    logcat(DEBUG_TAG) { "Open archive ${file.toUri().displayPath}" }
    val pfd = file.openFileDescriptor("r")
    val size = openArchive(pfd.fd, pfd.statSize, gid == 0L || file.name.endsWith(".zip"))
    if (size != 0 && passwdProvider != null && needPassword()) {
        if (!archivePasswds.filterNotNull().any { providePassword(it) }) {
            val toAdd = passwdProvider { providePassword(it) }
            archivePasswds += toAdd
        }
    }
    return object : PageLoader2(gid, startPage) {
        private val jobs = mutableIntObjectMapOf<Job>()
        private val mutex = NamedMutex<Int>()
        private val semaphore = Semaphore(4)

        override val title by lazy { FileUtils.getNameFromFilename(file.name)!! }

        override fun getImageExtension(index: Int) = getExtension(index)

        override fun save(index: Int, file: Path) = runCatching {
            file.openFileDescriptor("w").use {
                extractToFd(index, it.fd)
            }
        }.getOrElse {
            logcat(it)
            false
        }

        override fun close() {
            super.close()
            closeArchive()
            pfd.close()
            logcat(DEBUG_TAG) { "Close archive ${file.toUri().displayPath} successfully!" }
        }

        override val size = size

        override fun prefetchPages(pages: List<Int>, bounds: Pair<Int, Int>) = Unit

        override fun onRequest(index: Int) = synchronized(jobs) {
            val current = jobs[index]
            if (current?.isActive != true) {
                jobs[index] = launch {
                    semaphore.withPermit {
                        mutex.withLock(index) {
                            doRealWork(index)
                        }
                    }
                }
            }
        }

        suspend fun doRealWork(index: Int) {
            val buffer = extractToByteBuffer(index) ?: return notifyPageFailed(index, null)
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
            val image = Image.decode(src, hasAds && detectAds(index, size)) ?: return notifyPageFailed(index, null)
            runCatching {
                currentCoroutineContext().ensureActive()
            }.onFailure {
                image.recycle()
                throw it
            }
            notifyPageSucceed(index, image)
        }

        override fun onForceRequest(index: Int, orgImg: Boolean) = onRequest(index)

        override fun onCancelRequest(index: Int) {
            jobs[index]?.cancel()
        }
    }
}

private const val DEBUG_TAG = "ArchivePageLoader"
