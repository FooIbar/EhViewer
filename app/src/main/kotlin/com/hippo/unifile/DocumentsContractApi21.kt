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

    fun createFile(self: Uri, mimeType: String, displayName: String) = runCatching {
        DocumentsContract.createDocument(resolver, self, mimeType, displayName)
    }.getOrNull()

    fun createDirectory(self: Uri, displayName: String) = createFile(self, DocumentsContract.Document.MIME_TYPE_DIR, displayName)

    fun prepareTreeUri(treeUri: Uri): Uri = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri),
    )

    // This is not a good approach as the document identifier is an opaque implementation detail of
    // the provider, but works for ExternalStorageProvider
    fun buildChildUri(uri: Uri, displayName: String): Uri = DocumentsContract.buildDocumentUriUsingTree(
        uri,
        DocumentsContract.getDocumentId(uri) + "/" + displayName,
    )

    fun renameTo(self: Uri, displayName: String) = runCatching {
        DocumentsContract.renameDocument(resolver, self, displayName)
    }.getOrNull()
}
