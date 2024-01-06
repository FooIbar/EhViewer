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
import eu.kanade.tachiyomi.util.system.logcat
import java.io.File
import java.util.Locale

class RawFile(parent: UniFile?, private var file: File) : UniFile(parent) {
    private val parent = parent as? RawFile

    private var cachePresent = false
    private val allChildren by lazy {
        val current = this
        cachePresent = true
        logcat { "Directory lookup cache created for $name" }
        mutableListOf<RawFile>().apply {
            val fs = file.listFiles()?.map { RawFile(current, it) }
            if (fs != null) addAll(fs)
        }
    }

    private fun popCacheIfPresent(file: RawFile) = apply {
        if (cachePresent) {
            synchronized(allChildren) {
                allChildren.add(file)
            }
        }
    }

    override fun createFile(displayName: String): UniFile? {
        val target = RawFile(this, File(file, displayName))
        if (target.exists()) {
            if (target.isFile) return target
        } else {
            if (target.ensureFile()) return target
        }
        return null
    }

    override fun createDirectory(displayName: String): UniFile? {
        val target = RawFile(this, File(file, displayName))
        if (target.ensureDir()) return target
        return null
    }

    override val uri: Uri
        get() = Uri.fromFile(file)
    override val name: String
        get() = file.name
    override val type: String?
        get() = if (file.isDirectory) {
            null
        } else {
            getTypeForName(file.name)
        }
    override val isDirectory: Boolean
        get() = file.isDirectory
    override val isFile: Boolean
        get() = file.isFile

    override fun lastModified() = file.lastModified()

    override fun length() = file.length()

    override fun canRead() = file.canRead()

    override fun canWrite() = file.canWrite()

    override fun ensureDir(): Boolean {
        if (file.isDirectory) return true
        if (file.mkdirs()) {
            parent?.popCacheIfPresent(this)
            return true
        }
        return false
    }

    override fun ensureFile(): Boolean {
        if (file.exists()) return file.isFile
        parent?.ensureDir()
        val success = file.createNewFile()
        if (success) parent?.popCacheIfPresent(this)
        return success
    }

    override fun resolve(displayName: String) = RawFile(this, File(file, displayName))

    override fun delete() = file.deleteRecursively()

    override fun exists() = file.exists()

    override fun listFiles() = synchronized(allChildren) {
        allChildren.toList()
    }

    override fun findFirst(filter: (String) -> Boolean) = synchronized(allChildren) {
        allChildren.firstOrNull { filter(it.name) }
    }

    override fun renameTo(displayName: String): Boolean {
        val target = File(file.parentFile, displayName)
        if (file.renameTo(target)) {
            file = target
            return true
        }
        return false
    }

    override fun openFileDescriptor(mode: String): ParcelFileDescriptor = open(file, parseMode(mode))
}

private fun getTypeForName(name: String): String {
    val lastDot = name.lastIndexOf('.')
    if (lastDot >= 0) {
        val extension = name.substring(lastDot + 1).lowercase(Locale.getDefault())
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        if (mime != null) {
            return mime
        }
    }
    return "application/octet-stream"
}
