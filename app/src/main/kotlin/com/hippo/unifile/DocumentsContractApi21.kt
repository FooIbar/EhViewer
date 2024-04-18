/*
 * Copyright (C) 2014 The Android Open Source Project
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
import splitties.init.appCtx

object DocumentsContractApi21 {
    private val resolver = appCtx.contentResolver
    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
    )

    fun createFile(self: Uri, mimeType: String, displayName: String) = runCatching {
        DocumentsContract.createDocument(resolver, self, mimeType, displayName)
    }.getOrNull()

    fun createDirectory(self: Uri, displayName: String) = createFile(self, DocumentsContract.Document.MIME_TYPE_DIR, displayName)

    fun prepareTreeUri(treeUri: Uri): Uri {
        val documentId = if (treeUri.pathSegments.size == 2) {
            DocumentsContract.getTreeDocumentId(treeUri)
        } else {
            DocumentsContract.getDocumentId(treeUri)
        }
        return DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            documentId,
        )
    }

    fun buildChildUri(uri: Uri, displayName: String): Uri {
        return DocumentsContract.buildDocumentUriUsingTree(
            uri,
            DocumentsContract.getDocumentId(uri) + "/" + displayName,
        )
    }

    fun listFiles(self: Uri) = sequence {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(self, DocumentsContract.getDocumentId(self))
        resolver.query(childrenUri, projection, null, null, null, null)?.use {
            while (it.moveToNext()) {
                val documentId = it.getString(0)
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(self, documentId)
                val displayName = it.getString(1)
                val mimeType = it.getString(2)
                yield(Triple(documentUri, displayName, mimeType))
            }
        }
    }

    fun renameTo(self: Uri, displayName: String) = runCatching {
        DocumentsContract.renameDocument(resolver, self, displayName)
    }.getOrNull()
}
