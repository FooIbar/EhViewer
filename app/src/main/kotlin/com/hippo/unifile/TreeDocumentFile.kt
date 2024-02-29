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
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.webkit.MimeTypeMap
import eu.kanade.tachiyomi.util.system.logcat
import splitties.init.appCtx

class TreeDocumentFile(
    parent: UniFile?,
    override var uri: Uri,
    override var name: String = getFilenameForUri(uri),
    mimeType: String? = null,
) : UniFile(parent) {
    private var cachePresent = false
    private val allChildren by lazy {
        cachePresent = true
        logcat { "Directory lookup cache created for $name" }
        DocumentsContractApi21.listFiles(uri).mapTo(mutableListOf()) { (uri, name, mimeType) ->
            TreeDocumentFile(this, uri, name, mimeType)
        }
    }

    private fun popCacheIfPresent(file: TreeDocumentFile) = apply {
        if (cachePresent) {
            synchronized(allChildren) {
                allChildren.add(file)
            }
        }
    }

    override fun createFile(displayName: String): UniFile? {
        val child = findFile(displayName)
        if (child != null) {
            if (child.isFile) return child
        } else {
            val extension = displayName.substringAfterLast('.', "")
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
            val result = DocumentsContractApi21.createFile(uri, mimeType, displayName)
            if (result != null) {
                val f = TreeDocumentFile(this, result, displayName, mimeType)
                popCacheIfPresent(f)
                return f
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
                val d = TreeDocumentFile(this, result, displayName, MIME_TYPE_DIR)
                popCacheIfPresent(d)
                return d
            }
        }
        return null
    }

    private val rawType by lazy {
        mimeType ?: DocumentsContractApi19.getRawType(uri)
    }

    override val type: String?
        get() = rawType.takeUnless { isDirectory }
    override val isDirectory: Boolean
        get() = rawType == MIME_TYPE_DIR
    override val isFile: Boolean
        get() = !type.isNullOrEmpty()

    override fun lastModified() = DocumentsContractApi19.lastModified(uri)

    override fun length() = DocumentsContractApi19.length(uri)

    override fun canRead() = DocumentsContractApi19.canRead(uri)

    override fun canWrite() = DocumentsContractApi19.canWrite(uri)

    override fun ensureDir(): Boolean {
        if (isDirectory) return true
        if (isFile) return false
        val parent = parentFile
        return if (parent != null && parent.ensureDir()) {
            parent.createDirectory(name) != null
        } else {
            false
        }
    }

    override fun ensureFile(): Boolean {
        if (isFile) return true
        if (isDirectory) return false
        val parent = parentFile
        return if (parent != null && parent.ensureDir()) {
            parent.createFile(name) != null
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
        allChildren.firstOrNull { filter(it.name) }
    }

    override fun renameTo(displayName: String): Boolean {
        val result = DocumentsContractApi21.renameTo(appCtx, uri, displayName)
        if (result != null) {
            uri = result
            name = displayName
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
