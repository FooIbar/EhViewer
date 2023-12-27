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

import android.net.Uri
import java.io.IOException

class MediaFile(override val uri: Uri) : UniFile(null) {
    override fun createFile(displayName: String) = null

    override fun createDirectory(displayName: String) = null

    override val name: String?
        get() = MediaContract.getName(uri)
    override val type: String?
        get() = MediaContract.getType(uri)
    override val isDirectory = false
    override val isFile: Boolean
        get() = DocumentsContractApi19.isFile(uri)

    override fun lastModified() = MediaContract.lastModified(uri)

    override fun length() = MediaContract.length(uri)

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

    override fun resolve(displayName: String) = error("MediaFile never have a children")

    override fun delete() = false

    override fun exists() = isFile

    override fun listFiles() = emptyList<MediaFile>()

    override fun findFirst(filter: (String) -> Boolean) = null

    override fun renameTo(displayName: String) = false
}
