/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.util

import android.os.Environment
import com.hippo.unifile.UniFile
import com.hippo.unifile.asUniFile
import java.io.File
import splitties.init.appCtx

fun File.ensureDirectory() = if (exists()) isDirectory else mkdirs()

object AppConfig {
    const val APP_DIRNAME = "EhViewer"
    private const val DOWNLOAD = "download"
    private const val TEMP = "temp"
    private const val PARSE_ERROR = "parse_error"
    private const val CRASH = "crash"

    private val externalAppDir: File?
        get() {
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
                return appCtx.getExternalFilesDir(null)?.takeIf { it.ensureDirectory() }
            }
            return null
        }

    private fun getDirInExternalAppDir(filename: String, create: Boolean = true) =
        externalAppDir?.run { File(this, filename).takeIf { if (create) it.ensureDirectory() else it.isDirectory } }

    val defaultDownloadDir: File?
        get() = getDirInExternalAppDir(DOWNLOAD, false)

    fun getTempDir(filename: String): UniFile? {
        return getDirInExternalAppDir(TEMP)?.run { File(this, filename).asUniFile() }
    }

    val externalTempDir: File?
        get() = appCtx.externalCacheDir?.run { File(this, TEMP).takeIf { it.ensureDirectory() } }

    val externalParseErrorDir: File?
        get() = getDirInExternalAppDir(PARSE_ERROR)
    val externalCrashDir: File?
        get() = getDirInExternalAppDir(CRASH)
    val tempDir: File?
        get() = appCtx.cacheDir.run { File(this, TEMP).takeIf { it.ensureDirectory() } }

    fun createTempFile(): File? {
        return FileUtils.createTempFile(tempDir, null)
    }

    fun getFilesDir(name: String) = File(appCtx.filesDir, name).takeIf { it.ensureDirectory() }
}
