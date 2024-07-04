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
    parent: TreeDocumentFile?,
    private var backingUri: Uri = Uri.EMPTY,
    private val givenName: String? = null,
    private val givenMimeType: String? = null,
) : FileNode<TreeDocumentFile>(parent) {

    override val uri: Uri
        get() = prepareUri().let { backingUri }

    private fun prepareUri() {
        if (backingUri != Uri.EMPTY) return
        val parent = parent ?: return
        val f = parent.findFile(name) ?: return
        backingUri = f.uri
    }

    override fun listFiles() = runCatching {
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
    }.getOrNull() ?: emptyList()

    private fun createFile(displayName: String, mimeType: String) = DocumentsContractApi21.createFile(uri, mimeType, displayName)?.let { result ->
        TreeDocumentFile(this, result, displayName, mimeType)
    }

    override fun createFile(displayName: String): UniFile? {
        if (!ensureDir()) return null
        val extension = displayName.substringAfterLast('.', "").ifEmpty { null }?.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        return createFile(displayName, mimeType)
    }

    override fun createDirectory(displayName: String): UniFile? {
        if (!ensureDir()) return null
        return createFile(displayName, Document.MIME_TYPE_DIR)
    }

    override val name
        get() = givenName ?: DocumentsContractApi19.getName(uri)!!

    private val mimeType
        get() = givenMimeType ?: DocumentsContractApi19.getRawType(uri)

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

    override fun resolve(displayName: String) = TreeDocumentFile(this, givenName = displayName)

    override fun delete() = DocumentsContractApi19.delete(uri)

    override fun exists() = DocumentsContractApi19.exists(uri)

    override fun renameTo(displayName: String): UniFile? = DocumentsContractApi21.renameTo(uri, displayName)?.let { result ->
        TreeDocumentFile(parent, result, displayName)
    }
}

private val projection = arrayOf(
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_MIME_TYPE,
)
