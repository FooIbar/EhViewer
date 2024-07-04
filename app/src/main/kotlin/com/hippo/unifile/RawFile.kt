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

class RawFile(parent: RawFile?, private val file: File) : FileNode<RawFile>(parent) {
    override fun listFiles() = file.listFiles()?.let { children ->
        MutableList(children.size) { RawFile(this, children[it]) }
    } ?: emptyList()

    private fun createFile(displayName: String, isFile: Boolean): UniFile? {
        val child = findFile(displayName)
        return if (child != null) {
            child.takeIf { if (isFile) it.isFile else it.isDirectory }
        } else {
            if (!ensureDir()) return null
            val target = File(file, displayName)
            val created = if (isFile) target.createNewFile() else target.mkdir()
            if (created) {
                RawFile(this, target)
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

    override fun resolve(displayName: String) = RawFile(this, File(file, displayName))

    override fun delete() = file.deleteRecursively()

    override fun exists() = file.exists()

    override fun renameTo(displayName: String): UniFile? {
        val old = name
        val target = File(file.parentFile, displayName)
        return if (file.renameTo(target)) {
            RawFile(parent, target)
        } else {
            null
        }
    }

    override fun openFileDescriptor(mode: String): ParcelFileDescriptor = open(file, parseMode(mode))
}
