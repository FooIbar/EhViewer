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
import android.webkit.MimeTypeMap
import eu.kanade.tachiyomi.util.system.logcat
import splitties.init.appCtx

class TreeDocumentFile(
    parent: UniFile?,
    override var uri: Uri,
    val filename: String,
) : UniFile(parent) {
    private var cachePresent = false
    private val allChildren by lazy {
        cachePresent = true
        logcat { "Directory lookup cache created for $name" }
        DocumentsContractApi21.listFiles(uri).mapTo(mutableListOf()) {
            val name = getFilenameForUri(it)
            TreeDocumentFile(this, it, name)
        }
    }

    private fun popCacheIfPresent(file: TreeDocumentFile) = apply {
        if (cachePresent) {
            synchronized(allChildren) {
                allChildren.add(file)
            }
        }
    }

    constructor(parent: UniFile?, uri: Uri) : this(parent, uri, getFilenameForUri(uri))

    override fun createFile(displayName: String): UniFile? {
        val child = findFile(displayName)
        if (child != null) {
            if (child.isFile) return child
        } else {
            val index = displayName.lastIndexOf('.')
            if (index > 0) {
                val name = displayName.substring(0, index)
                val extension = displayName.substring(index + 1)
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (!mimeType.isNullOrEmpty()) {
                    val result = DocumentsContractApi21.createFile(uri, mimeType, name)
                    if (result != null) {
                        val f = TreeDocumentFile(this, result, displayName)
                        popCacheIfPresent(f)
                        return f
                    }
                }
            } else {
                // Not dot in displayName or dot is the first char or can't get MimeType
                val result = DocumentsContractApi21.createFile(uri, "application/octet-stream", displayName)
                if (result != null) {
                    val f = TreeDocumentFile(this, result, displayName)
                    popCacheIfPresent(f)
                    return f
                }
            }
        }
        return null
    }

    override fun createDirectory(displayName: String): UniFile? {
        val child = findFile(displayName)
        if (child != null) {
            if (child.isDirectory) return null
        } else {
            val result = DocumentsContractApi21.createDirectory(uri, displayName)
            if (result != null) {
                val d = TreeDocumentFile(this, result, displayName)
                popCacheIfPresent(d)
                return d
            }
        }
        return null
    }

    override val name: String?
        get() = DocumentsContractApi19.getName(uri)
    override val type: String?
        get() = DocumentsContractApi19.getType(uri)
    override val isDirectory: Boolean
        get() = DocumentsContractApi19.isDirectory(uri)
    override val isFile: Boolean
        get() = DocumentsContractApi19.isFile(uri)

    override fun lastModified() = DocumentsContractApi19.lastModified(uri)

    override fun length() = DocumentsContractApi19.length(uri)

    override fun canRead() = DocumentsContractApi19.canRead(uri)

    override fun canWrite() = DocumentsContractApi19.canWrite(uri)

    override fun ensureDir(): Boolean {
        if (isDirectory) return true
        if (isFile) return false
        val parent = parentFile
        return if (parent != null && parent.ensureDir()) {
            parent.createDirectory(filename) != null
        } else {
            false
        }
    }

    override fun ensureFile(): Boolean {
        if (isFile) return true
        if (isDirectory) return false
        val parent = parentFile
        return if (parent != null && parent.ensureDir()) {
            parent.createFile(filename) != null
        } else {
            false
        }
    }

    override fun resolve(displayName: String): UniFile {
        val childUri = DocumentsContractApi21.buildChildUri(uri, displayName)
        return TreeDocumentFile(this, childUri, displayName)
    }

    override fun delete() = DocumentsContractApi19.delete(uri)

    override fun exists() = DocumentsContractApi19.exists(uri)

    override fun listFiles() = synchronized(allChildren) {
        allChildren.toList()
    }

    override fun findFirst(filter: (String) -> Boolean) = synchronized(allChildren) {
        allChildren.firstOrNull { filter(it.filename) }
    }

    override fun renameTo(displayName: String): Boolean {
        val result = DocumentsContractApi21.renameTo(appCtx, uri, displayName)
        if (result != null) {
            uri = result
            return true
        }
        return false
    }
}

private fun getFilenameForUri(uri: Uri): String {
    val path = requireNotNull(uri.path)
    val index = path.lastIndexOf('/')
    return if (index >= 0) {
        path.substring(index + 1)
    } else {
        return path
    }
}
