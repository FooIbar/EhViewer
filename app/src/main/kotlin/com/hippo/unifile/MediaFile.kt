/*
 * Copyright 2015 Hippo Seven
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
package com.hippo.unifile

import android.graphics.ImageDecoder
import android.net.Uri
import java.io.IOException
import splitties.init.appCtx

class MediaFile(override val uri: Uri) : UniFile(null) {
    override fun createFile(displayName: String) = null

    override fun createDirectory(displayName: String) = null

    override val name: String?
        get() = MediaContract.getName(appCtx, uri)
    override val type: String?
        get() = MediaContract.getType(appCtx, uri)
    override val isDirectory = false
    override val isFile: Boolean
        get() = DocumentsContractApi19.isFile(appCtx, uri)

    override fun lastModified() = MediaContract.lastModified(appCtx, uri)

    override fun length() = MediaContract.length(appCtx, uri)

    override fun canRead() = isFile

    override fun canWrite(): Boolean {
        try {
            val fd = openFileDescriptor("w")
            fd.close()
        } catch (e: IOException) {
            return false
        }
        return true
    }

    override fun ensureDir() = false

    override fun ensureFile() = isFile

    override fun subFile(displayName: String) = error("MediaFile never have a children")

    override fun delete() = false

    override fun exists() = isFile

    override fun listFiles() = emptyList<MediaFile>()

    override fun findFirst(filter: (String) -> Boolean) = null

    override fun renameTo(displayName: String) = false

    override val imageSource: ImageDecoder.Source
        get() = Contracts.getImageSource(appCtx, uri)

    override fun openFileDescriptor(mode: String) = Contracts.openFileDescriptor(appCtx, uri, mode)

    companion object {
        fun isMediaUri(uri: Uri): Boolean {
            return null != MediaContract.getName(appCtx, uri)
        }
    }
}
