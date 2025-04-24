/*
 * Copyright (C) 2015 Hippo Seven
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
package com.hippo.ehviewer.util

import com.hippo.files.write
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.rethrowCloseCauseIfNeeded
import java.io.File
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import okio.Path

object FileUtils {
    // Even though vfat allows 255 UCS-2 chars, we might eventually write to
    // ext4 through a FUSE layer, so use that limit.
    private const val MAX_FILENAME_BYTES = 255

    fun ensureDirectory(file: File?) = file?.let { if (it.exists()) it.isDirectory else it.mkdirs() } == true

    /**
     * Convert byte to human readable string.<br></br>
     * http://stackoverflow.com/questions/3758606/
     *
     * @param bytes the bytes to convert
     * @return the human readable string
     */
    fun humanReadableByteCount(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024f)).toInt()
        return "%.1f %sB".format(
            Locale.US,
            bytes / 1024.toDouble().pow(exp.toDouble()),
            ("KMGTPE")[exp - 1].toString() + "i",
        )
    }

    fun cleanupDirectory(dir: File?, maxFiles: Int = 10) {
        dir?.listFiles()?.let { files ->
            files.sortByDescending { it.lastModified() }
            files.forEachIndexed { index, file ->
                if (index >= maxFiles) file.delete()
            }
        }
    }

    private fun isValidFatFilenameChar(c: Char) = when (c.code) {
        in 0x00..0x1F,
        '"'.code,
        '*'.code,
        '/'.code,
        ':'.code,
        '<'.code,
        '>'.code,
        '?'.code,
        '\\'.code,
        '|'.code,
        0x7F,
        -> false
        else -> true
    }

    // From https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/os/FileUtils.java;l=1142;drc=7b647e4ea0e92f33c19b315eaed364ee067ba0aa
    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_".
     *
     * @param name the original filename
     */
    fun sanitizeFilename(name: String): String {
        if (name.isEmpty() || "." == name || ".." == name) {
            return "(invalid)"
        }
        return buildString(name.length) {
            name.forEach {
                if (isValidFatFilenameChar(it)) {
                    append(it)
                } else {
                    append('_')
                }
            }
            if (sizeInBytes > MAX_FILENAME_BYTES) {
                while (sizeInBytes > MAX_FILENAME_BYTES - 3) {
                    deleteCharAt(length / 2)
                }
                insert(length / 2, "...")
            }
        }
    }

    private val StringBuilder.sizeInBytes
        get() = toString().toByteArray().size

    /**
     * Get extension from filename
     *
     * @param filename the complete filename
     * @return null for can't find extension
     */
    fun getExtensionFromFilename(filename: String?) = filename?.substringAfterLast('.', "")?.ifEmpty { null }

    /**
     * Get name from filename
     *
     * @param filename the complete filename
     * @return null for start with . dot
     */
    fun getNameFromFilename(filename: String?) = filename?.substringBeforeLast('.')?.ifEmpty { null }
}

@OptIn(InternalAPI::class)
suspend fun ByteReadChannel.copyTo(file: Path) {
    file.write {
        while (!isClosedForRead) {
            transferFrom(readBuffer)
            awaitContent()
        }
        rethrowCloseCauseIfNeeded()
    }
}
