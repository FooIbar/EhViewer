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
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.webkit.MimeTypeMap
import splitties.init.appCtx

class TreeDocumentFile(
    override val parent: TreeDocumentFile?,
    override val uri: Uri,
    name: String? = null,
    mimeType: String? = null,
) : CachingFile<TreeDocumentFile>() {
    override var cachePresent = false
    override val allChildren by lazy {
        cachePresent = true
        queryChildren() ?: mutableListOf()
    }

    private fun createFile(displayName: String, mimeType: String) =
        DocumentsContractApi21.createFile(uri, mimeType, displayName)?.let { result ->
            TreeDocumentFile(this, result, displayName, mimeType).also { popCacheIfPresent(it) }
        }

    override fun createFile(displayName: String): UniFile? {
        val child = findFile(displayName)
        return if (child != null) {
            child.takeIf { it.isFile }
        } else {
            val extension = displayName.substringAfterLast('.', "").ifEmpty { null }?.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
            createFile(displayName, mimeType)
        }
    }

    override fun createDirectory(displayName: String): UniFile? {
        val child = findFile(displayName)
        return if (child != null) {
            child.takeIf { it.isDirectory }
        } else {
            createFile(displayName, Document.MIME_TYPE_DIR)
        }
    }

    override val name by lazy {
        name ?: DocumentsContractApi19.getName(uri)!!
    }

    private val mimeType by lazy {
        mimeType ?: DocumentsContractApi19.getRawType(uri)
    }

    override val type: String?
        get() = mimeType.takeUnless { isDirectory }
    override val isDirectory: Boolean
        get() = mimeType == Document.MIME_TYPE_DIR
    override val isFile: Boolean
        get() = !type.isNullOrEmpty()

    override fun lastModified() = DocumentsContractApi19.lastModified(uri)

    override fun length() = DocumentsContractApi19.length(uri)

    override fun canRead() = DocumentsContractApi19.canRead(uri)

    override fun canWrite() = DocumentsContractApi19.canWrite(uri)

    override fun delete() = DocumentsContractApi19.delete(uri).also {
        if (it) parent?.evictCacheIfPresent(this)
    }

    override fun exists() = DocumentsContractApi19.exists(uri)

    override fun listFiles() = synchronized(allChildren) {
        allChildren.toList()
    }

    override fun findFirst(filter: (String) -> Boolean) = synchronized(allChildren) {
        allChildren.firstOrNull { filter(it.name) }
    }

    override fun renameTo(displayName: String): UniFile? =
        DocumentsContractApi21.renameTo(uri, displayName)?.let { result ->
            TreeDocumentFile(parent, result, displayName).also {
                parent?.replaceCacheIfPresent(this, it)
            }
        }
}

private val projection = arrayOf(
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_MIME_TYPE,
)

private fun TreeDocumentFile.queryChildren() = runCatching {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
    appCtx.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
        MutableList(c.count) {
            c.moveToNext()
            val documentId = c.getString(0)
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
            val displayName = c.getString(1)
            val mimeType = c.getString(2)
            TreeDocumentFile(this, documentUri, displayName, mimeType)
        }
    }
}.getOrNull()
