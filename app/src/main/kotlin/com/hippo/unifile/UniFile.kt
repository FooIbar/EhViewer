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

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toFile
import androidx.core.provider.DocumentsContractCompat
import java.io.File
import splitties.init.appCtx

/**
 * In Android files can be accessed via [java.io.File] and [android.net.Uri].
 * The UniFile is designed to emulate File interface for both File and Uri.
 */
sealed interface UniFile {
    /**
     * Create a new file as a direct child of this directory.
     *
     * @param displayName name of new file
     * @return file representing newly created document, or null if failed
     * @see android.provider.DocumentsContract.createDocument
     */
    fun createFile(displayName: String): UniFile?

    /**
     * Create a new directory as a direct child of this directory.
     *
     * @param displayName name of new directory
     * @return file representing newly created directory, or null if failed
     * @see android.provider.DocumentsContract.createDocument
     */
    fun createDirectory(displayName: String): UniFile?

    /**
     * Return a Uri for the underlying document represented by this file. This
     * can be used with other platform APIs to manipulate or share the
     * underlying content. You can use [.isTreeUri] to
     * test if the returned Uri is backed by a
     * [android.provider.DocumentsProvider].
     *
     * @return uri of the file
     * @see Intent.setData
     * @see Intent.setClipData
     * @see ContentResolver.openInputStream
     * @see ContentResolver.openOutputStream
     * @see ContentResolver.openFileDescriptor
     */
    val uri: Uri

    /**
     * Return the display name of this file.
     *
     * @return name of the file, or null if failed
     * @see android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
     */
    val name: String?

    /**
     * Return the MIME type of this file.
     *
     * @return MIME type of the file, or null if failed
     * @see android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
     */
    val type: String?

    /**
     * Return the parent file of this file. Only defined inside of the
     * user-selected tree; you can never escape above the top of the tree.
     *
     *
     * The underlying [android.provider.DocumentsProvider] only defines a
     * forward mapping from parent to child, so the reverse mapping of child to
     * parent offered here is purely a convenience method, and it may be
     * incorrect if the underlying tree structure changes.
     *
     * @return parent of the file, or null if it is the top of the file tree
     */
    val parent: UniFile?

    /**
     * Indicates if this file represents a *directory*.
     *
     * @return `true` if this file is a directory, `false`
     * otherwise.
     * @see android.provider.DocumentsContract.Document.MIME_TYPE_DIR
     */
    val isDirectory: Boolean

    /**
     * Indicates if this file represents a *file*.
     *
     * @return `true` if this file is a file, `false` otherwise.
     * @see android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
     */
    val isFile: Boolean

    /**
     * Returns the time when this file was last modified, measured in
     * milliseconds since January 1st, 1970, midnight. Returns -1 if the file
     * does not exist, or if the modified time is unknown.
     *
     * @return the time when this file was last modified, `-1L` if can't get it
     * @see android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
     */
    fun lastModified(): Long

    /**
     * Returns the length of this file in bytes. Returns -1 if the file does not
     * exist, or if the length is unknown. The result for a directory is not
     * defined.
     *
     * @return the number of bytes in this file, `-1L` if can't get it
     * @see android.provider.DocumentsContract.Document.COLUMN_SIZE
     */
    fun length(): Long

    /**
     * Indicates whether the current context is allowed to read from this file.
     *
     * @return `true` if this file can be read, `false` otherwise.
     */
    fun canRead(): Boolean

    /**
     * Indicates whether the current context is allowed to write to this file.
     *
     * @return `true` if this file can be written, `false`
     * otherwise.
     * @see android.provider.DocumentsContract.Document.COLUMN_FLAGS
     *
     * @see android.provider.DocumentsContract.Document.FLAG_SUPPORTS_DELETE
     *
     * @see android.provider.DocumentsContract.Document.FLAG_SUPPORTS_WRITE
     *
     * @see android.provider.DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
     */
    fun canWrite(): Boolean

    /**
     * It works like mkdirs, but it will return true if the UniFile is directory
     *
     * @return `true` if the directory was created
     * or if the directory already existed.
     */
    fun ensureDir(): Boolean

    /**
     * Make sure the UniFile is file
     *
     * @return `true` if the file can be created
     * or if the file already existed.
     */
    fun ensureFile(): Boolean

    /**
     * Get child file of this directory, the child might not exist.
     *
     * @return the child file
     */
    fun resolve(displayName: String): UniFile

    operator fun div(name: String) = resolve(name)

    /**
     * Deletes this file.
     *
     *
     * Note that this method does *not* throw `IOException` on
     * failure. Callers must check the return value.
     *
     * @return `true` if this file was deleted, `false` otherwise.
     * @see android.provider.DocumentsContract.deleteDocument
     */
    fun delete(): Boolean

    /**
     * Returns a boolean indicating whether this file can be found.
     *
     * @return `true` if this file exists, `false` otherwise.
     */
    fun exists(): Boolean

    fun listFiles(): List<UniFile>

    fun openFileDescriptor(mode: String) = appCtx.contentResolver.openFileDescriptor(uri, mode) ?: error("Can't open ParcelFileDescriptor")

    /**
     * Test there is a file with the display name in the directory.
     *
     * @return the file if found it, or `null`.
     */
    fun findFile(displayName: String): UniFile?

    /**
     * Renames this file to `displayName`.
     *
     *
     * Note that this method does *not* throw `IOException` on
     * failure. Callers must check the return value.
     *
     *
     * Some providers may need to create a new file to reflect the rename,
     * potentially with a different MIME type, so [uri] and
     * [type] may change to reflect the rename.
     *
     *
     * When renaming a directory, children previously enumerated through
     * [listFiles] may no longer be valid.
     *
     * @param displayName the new display name.
     * @return the new file after the rename, or `null` if failed.
     * @see android.provider.DocumentsContract.renameDocument
     */
    fun renameTo(displayName: String): UniFile?

    companion object {
        fun fromFile(file: File) = RawFile(null, file)

        fun fromUri(uri: Uri) = when {
            isFileUri(uri) -> fromFile(uri.toFile())
            isTreeUri(uri) -> {
                if (isDocumentUri(uri)) {
                    TreeDocumentFile(null, uri)
                } else {
                    TreeDocumentFile(null, DocumentsContractApi21.prepareTreeUri(uri))
                }
            }

            isDocumentUri(uri) -> SingleDocumentFile(uri)
            isMediaUri(uri) -> MediaFile(uri)
            else -> null
        }

        fun isFileUri(uri: Uri) = ContentResolver.SCHEME_FILE == uri.scheme

        fun isDocumentUri(uri: Uri) = DocumentsContract.isDocumentUri(appCtx, uri)

        private fun isTreeUri(uri: Uri) = DocumentsContractCompat.isTreeUri(uri)

        private fun isMediaUri(uri: Uri) = null != MediaContract.getName(uri)

        val Stub = fromFile(File(""))
    }
}
