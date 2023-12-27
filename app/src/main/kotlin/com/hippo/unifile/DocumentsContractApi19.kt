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

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import splitties.init.appCtx

object DocumentsContractApi19 {

    fun getName(self: Uri) = Contracts.queryForString(self, DocumentsContract.Document.COLUMN_DISPLAY_NAME, null)

    private fun getRawType(self: Uri) = Contracts.queryForString(self, DocumentsContract.Document.COLUMN_MIME_TYPE, null)

    fun getType(self: Uri) = getRawType(self).takeUnless { it == DocumentsContract.Document.MIME_TYPE_DIR }

    fun isDirectory(self: Uri) = DocumentsContract.Document.MIME_TYPE_DIR == getRawType(self)

    fun isFile(self: Uri): Boolean {
        val type = getRawType(self)
        return !(DocumentsContract.Document.MIME_TYPE_DIR == type || type.isNullOrEmpty())
    }

    fun lastModified(self: Uri) = Contracts.queryForLong(self, DocumentsContract.Document.COLUMN_LAST_MODIFIED, -1L)

    fun length(self: Uri) = Contracts.queryForLong(self, DocumentsContract.Document.COLUMN_SIZE, -1L)

    fun canRead(self: Uri): Boolean {
        val granted = appCtx.checkCallingOrSelfUriPermission(self, Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED
        // Ignore if grant doesn't allow read
        if (!granted) return false

        // Ignore documents without MIME
        return !getRawType(self).isNullOrEmpty()
    }

    fun canWrite(self: Uri): Boolean {
        val granted = appCtx.checkCallingOrSelfUriPermission(self, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED
        // Ignore if grant doesn't allow write
        if (!granted) return false

        val type = getRawType(self)
        val flags = Contracts.queryForInt(self, DocumentsContract.Document.COLUMN_FLAGS, 0)
        // Ignore documents without MIME
        if (type.isNullOrEmpty()) return false

        // Deletable documents considered writable
        if (flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0) return true

        val dirSupportCreate = DocumentsContract.Document.MIME_TYPE_DIR == type && flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0
        // Directories that allow create considered writable
        if (dirSupportCreate) return true

        // Writable normal files considered writable
        return flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0
    }

    fun delete(self: Uri) = runCatching { DocumentsContract.deleteDocument(appCtx.contentResolver, self) }.getOrDefault(false)

    fun exists(self: Uri) = runCatching {
        appCtx.contentResolver.query(
            self,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null,
        ).use { null != it && it.count > 0 }
    }.getOrDefault(false)
}
