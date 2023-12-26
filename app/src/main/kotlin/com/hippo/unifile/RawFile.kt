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

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.util.Locale

class RawFile(parent: UniFile?, private var mFile: File) : UniFile(parent) {
    val parent = parent as? RawFile

    private val allChildren by lazy {
        val current = this
        mutableListOf<RawFile>().apply {
            val fs = mFile.listFiles()?.map { RawFile(current, it) }
            if (fs != null) addAll(fs)
        }
    }

    override fun createFile(displayName: String): UniFile? {
        val target = RawFile(this, File(mFile, displayName))
        if (target.exists()) {
            if (target.isFile) return target
        } else {
            if (target.ensureFile()) return target
        }
        return null
    }

    override fun createDirectory(displayName: String): UniFile? {
        val target = RawFile(this, File(mFile, displayName))
        if (target.ensureDir()) return target
        return null
    }

    override val uri: Uri
        get() = Uri.fromFile(mFile)
    override val name: String
        get() = mFile.name
    override val type: String?
        get() = if (mFile.isDirectory) {
            null
        } else {
            getTypeForName(mFile.name)
        }
    override val isDirectory: Boolean
        get() = mFile.isDirectory
    override val isFile: Boolean
        get() = mFile.isFile

    override fun lastModified() = mFile.lastModified()

    override fun length() = mFile.length()

    override fun canRead() = mFile.canRead()

    override fun canWrite() = mFile.canWrite()

    override fun ensureDir(): Boolean {
        if (mFile.isDirectory) return true
        if (mFile.mkdirs()) {
            parent?.allChildren?.add(this)
            return true
        }
        return false
    }

    override fun ensureFile(): Boolean {
        if (mFile.exists()) return mFile.isFile
        val success = mFile.createNewFile()
        if (success) parent?.allChildren?.add(this)
        return success
    }

    override fun subFile(displayName: String) = RawFile(this, File(mFile, displayName))

    override fun delete() = mFile.deleteRecursively()

    override fun exists() = mFile.exists()

    override fun listFiles() = allChildren

    override fun findFirst(filter: (String) -> Boolean) = allChildren.firstOrNull { filter(it.name) }

    override fun findFile(displayName: String) = subFile(displayName).takeIf { it.exists() }

    override fun renameTo(displayName: String): Boolean {
        val target = File(mFile.parentFile, displayName)
        if (mFile.renameTo(target)) {
            mFile = target
            return true
        }
        return false
    }

    override val imageSource: ImageDecoder.Source
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.createSource(mFile)
        } else {
            TODO("VERSION.SDK_INT < P")
        }

    override fun openFileDescriptor(mode: String): ParcelFileDescriptor {
        val md = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(mFile, md) ?: throw IOException("Can't open ParcelFileDescriptor")
    }
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
