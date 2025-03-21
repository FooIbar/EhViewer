/*
 * Copyright 2023-2024 Tarsin Norbin
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

import arrow.autoCloseScope
import com.hippo.ehviewer.Settings.archivePasswds
import com.hippo.ehviewer.image.ImageSource
import com.hippo.ehviewer.image.byteBufferSource
import com.hippo.ehviewer.jni.closeArchive
import com.hippo.ehviewer.jni.extractToByteBuffer
import com.hippo.ehviewer.jni.extractToFd
import com.hippo.ehviewer.jni.getExtension
import com.hippo.ehviewer.jni.needPassword
import com.hippo.ehviewer.jni.openArchive
import com.hippo.ehviewer.jni.providePassword
import com.hippo.ehviewer.jni.releaseByteBuffer
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.displayName
import com.hippo.files.openFileDescriptor
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.coroutineScope
import moe.tarsin.kt.install
import okio.Path

typealias PasswdInvalidator = (String) -> Boolean
typealias PasswdProvider = suspend (PasswdInvalidator) -> String

suspend inline fun <T> useArchivePageLoader(
    file: Path,
    gid: Long = 0,
    startPage: Int = 0,
    hasAds: Boolean = false,
    crossinline passwdProvider: PasswdProvider,
    crossinline block: suspend (PageLoader) -> T,
) = autoCloseScope {
    coroutineScope {
        val pfd = install(file.openFileDescriptor("r"))
        val size = install(
            { openArchive(pfd.fd, pfd.statSize, gid == 0L || file.name.endsWith(".zip")) },
            { _, _ -> closeArchive() },
        )
        check(size > 0) { "Archive have no content!" }
        if (needPassword() && archivePasswds.filterNotNull().none(::providePassword)) {
            archivePasswds += passwdProvider(::providePassword)
        }
        val loader = install(
            object : PageLoader(this, gid, startPage, size, hasAds) {
                override val title by lazy { FileUtils.getNameFromFilename(file.displayName)!! }

                override fun getImageExtension(index: Int) = getExtension(index)

                override fun save(index: Int, file: Path) = runCatching {
                    file.openFileDescriptor("w").use {
                        extractToFd(index, it.fd)
                    }
                }.getOrElse {
                    logcat(it)
                    false
                }

                override fun openSource(index: Int): ImageSource {
                    val buffer = extractToByteBuffer(index)
                    checkNotNull(buffer) { "Extract archive content $index failed!" }
                    check(buffer.isDirect)
                    return byteBufferSource(buffer) { releaseByteBuffer(buffer) }
                }

                override fun prefetchPages(pages: List<Int>, bounds: IntRange) = pages.forEach(::notifySourceReady)

                override fun onRequest(index: Int, force: Boolean, orgImg: Boolean) = notifySourceReady(index)
            },
        )
        block(loader)
    }
}
