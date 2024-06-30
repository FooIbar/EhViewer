/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.unifile

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.open
import android.os.ParcelFileDescriptor.parseMode
import android.webkit.MimeTypeMap
import java.io.File

class RawFile(override val parent: RawFile?, private val file: File) : CachingFile<RawFile>() {
    override var cachePresent = false
    override val allChildren by lazy {
        cachePresent = true
        file.listFiles().orEmpty().mapTo(mutableListOf()) { RawFile(this, it) }
    }

    private fun createFile(displayName: String, isFile: Boolean): UniFile? {
        val child = findFile(displayName)
        return if (child != null) {
            child.takeIf { if (isFile) it.isFile else it.isDirectory }
        } else {
            val target = File(file, displayName)
            val created = if (isFile) target.createNewFile() else target.mkdir()
            if (created) {
                RawFile(this, target).also { popCacheIfPresent(it) }
            } else {
                null
            }
        }
    }

    override fun createFile(displayName: String) = createFile(displayName, true)

    override fun createDirectory(displayName: String) = createFile(displayName, false)

    override val uri: Uri
        get() = Uri.fromFile(file)
    override val name: String
        get() = file.name
    override val type: String?
        get() = if (file.isDirectory) {
            null
        } else {
            val extension = file.extension.ifEmpty { null }?.lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
    override val isDirectory: Boolean
        get() = file.isDirectory
    override val isFile: Boolean
        get() = file.isFile

    override fun lastModified() = file.lastModified()

    override fun length() = file.length()

    override fun canRead() = file.canRead()

    override fun canWrite() = file.canWrite()

    override fun delete() = file.deleteRecursively().also {
        if (it) parent?.evictCacheIfPresent(this)
    }

    override fun exists() = file.exists()

    override fun listFiles() = synchronized(allChildren) {
        allChildren.toList()
    }

    override fun findFirst(filter: (String) -> Boolean) = synchronized(allChildren) {
        allChildren.firstOrNull { filter(it.name) }
    }

    override fun findFile(displayName: String): UniFile? = if (cachePresent) {
        super.findFile(displayName)
    } else {
        val target = File(file, displayName)
        if (target.exists()) {
            RawFile(this, target)
        } else {
            null
        }
    }

    override fun renameTo(displayName: String): UniFile? {
        val target = File(file.parentFile, displayName)
        return if (file.renameTo(target)) {
            RawFile(parent, target).also { parent?.replaceCacheIfPresent(this, it) }
        } else {
            null
        }
    }

    override fun openFileDescriptor(mode: String): ParcelFileDescriptor = open(file, parseMode(mode))
}
