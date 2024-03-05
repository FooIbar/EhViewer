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
package com.hippo.ehviewer.spider

import android.webkit.MimeTypeMap
import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.closeable
import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.resourceScope
import com.hippo.ehviewer.EhApplication.Companion.imageCache as sCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUtils.getSuitableTitle
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.ehRequest
import com.hippo.ehviewer.client.getImageKey
import com.hippo.ehviewer.coil.read
import com.hippo.ehviewer.coil.suspendEdit
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.image.UniFileSource
import com.hippo.ehviewer.jni.archiveFdBatch
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.sendTo
import com.hippo.unifile.UniFile
import com.hippo.unifile.asUniFile
import com.hippo.unifile.openOutputStream
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.client.plugins.onDownload
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.nio.copyTo
import kotlin.io.path.readText
import kotlinx.coroutines.CancellationException

class SpiderDen(val info: GalleryInfo) {
    private val gid = info.gid
    var downloadDir: UniFile? = null
        private set

    private var tempDownloadDir: UniFile? = null
    private val saveAsCbz = Settings.saveAsCbz
    private val archiveName = "$gid.cbz"

    private val imageDir
        get() = tempDownloadDir.takeIf { saveAsCbz } ?: downloadDir

    constructor(info: GalleryInfo, dirname: String) : this(info) {
        downloadDir = downloadLocation / dirname
    }

    @Volatile
    @SpiderQueen.Mode
    var mode = SpiderQueen.MODE_READ
        private set

    suspend fun setMode(value: Int) {
        mode = value
        if (mode == SpiderQueen.MODE_DOWNLOAD) {
            if (downloadDir == null) {
                downloadDir = getGalleryDownloadDir(gid)?.takeIf { it.ensureDir() }
            }
            if (saveAsCbz && tempDownloadDir == null) {
                tempDownloadDir = AppConfig.getTempDir("$gid")?.takeIf { it.ensureDir() }
            }
        }
    }

    private fun containInCache(index: Int): Boolean {
        val key = getImageKey(gid, index)
        return sCache.read(key) { true } ?: false
    }

    // Search in both directories to maintain compatibility
    private fun findImageFile(index: Int): UniFile? {
        return tempDownloadDir?.findImageFile(index) ?: downloadDir?.findImageFile(index)
    }

    private fun containInDownloadDir(index: Int): Boolean {
        return findImageFile(index) != null
    }

    private fun copyFromCacheToDownloadDir(index: Int): Boolean {
        val dir = imageDir ?: return false
        val key = getImageKey(gid, index)
        return runCatching {
            sCache.read(key) {
                val extension = metadata.toFile().readText()
                val file = dir.createFile(perFilename(index, extension)) ?: return false
                data.asUniFile() sendTo file
                true
            } ?: false
        }.onFailure {
            logcat(it)
        }.getOrDefault(false)
    }

    operator fun contains(index: Int): Boolean {
        return when (mode) {
            SpiderQueen.MODE_READ -> {
                containInCache(index) || containInDownloadDir(index)
            }

            SpiderQueen.MODE_DOWNLOAD -> {
                containInDownloadDir(index) || copyFromCacheToDownloadDir(index)
            }

            else -> {
                false
            }
        }
    }

    private fun removeFromCache(index: Int): Boolean {
        val key = getImageKey(gid, index)
        return sCache.remove(key)
    }

    private fun removeFromDownloadDir(index: Int): Boolean {
        return findImageFile(index)?.delete() ?: false
    }

    fun remove(index: Int): Boolean {
        return removeFromCache(index) or removeFromDownloadDir(index)
    }

    private fun findDownloadFileForIndex(index: Int, extension: String): UniFile? {
        return imageDir?.createFile(perFilename(index, extension))
    }

    suspend fun makeHttpCallAndSaveImage(
        index: Int,
        url: String,
        referer: String?,
        notifyProgress: (Long, Long, Int) -> Unit,
    ) = ehRequest(url, referer) {
        var prev = 0L
        onDownload { done, total ->
            notifyProgress(total!!, done, (done - prev).toInt())
            prev = done
        }
    }.execute {
        if (it.status.isSuccess()) {
            saveFromHttpResponse(index, it)
        } else {
            false
        }
    }

    private suspend inline fun saveResponseMeta(
        index: Int,
        ext: String,
        crossinline fops: suspend (UniFile) -> Unit,
    ): Boolean {
        findDownloadFileForIndex(index, ext)?.run {
            fops(this)
            return true
        }

        // Read Mode, allow save to cache
        if (mode == SpiderQueen.MODE_READ) {
            val key = getImageKey(gid, index)
            return sCache.suspendEdit(key) {
                metadata.toFile().writeText(ext)
                fops(data.asUniFile())
            }
        }
        return false
    }

    private suspend fun saveFromHttpResponse(index: Int, response: HttpResponse): Boolean {
        val url = response.request.url.toString()
        val extension = MimeTypeMap.getFileExtensionFromUrl(url).takeUnless { it.isEmpty() } ?: "jpg"
        return saveResponseMeta(index, extension) { outFile ->
            outFile.openOutputStream().use {
                val chan = it.channel.apply { truncate(0) }
                response.bodyAsChannel().copyTo(chan)
            }
        }
    }

    fun saveToUniFile(index: Int, file: UniFile): Boolean {
        val key = getImageKey(gid, index)

        // Read from diskCache first
        sCache.read(key) {
            runCatching {
                data.asUniFile() sendTo file
                return true
            }.onFailure {
                logcat(it)
                return false
            }
        }

        // Read from download dir
        runCatching {
            requireNotNull(findImageFile(index)) sendTo file
        }.onFailure {
            logcat(it)
            return false
        }.onSuccess {
            return true
        }
        return false
    }

    fun getExtension(index: Int): String? {
        val key = getImageKey(gid, index)
        return sCache.read(key) { metadata.toNioPath().readText() }
            ?: findImageFile(index)?.name.let { FileUtils.getExtensionFromFilename(it) }
    }

    fun getImageSource(index: Int): UniFileSource? {
        if (mode == SpiderQueen.MODE_READ) {
            val key = getImageKey(gid, index)
            val snapshot = sCache.openSnapshot(key)
            if (snapshot != null) {
                return object : UniFileSource, AutoCloseable by snapshot {
                    override val source = snapshot.data.asUniFile()
                    override val type by lazy {
                        snapshot.metadata.toNioPath().readText()
                    }
                }
            }
        }
        val source = findImageFile(index) ?: return null
        return object : UniFileSource {
            override val source = source
            override val type by lazy {
                FileUtils.getExtensionFromFilename(source.name)!!
            }

            override fun close() {}
        }
    }

    suspend fun archive() {
        if (saveAsCbz) {
            downloadDir?.run {
                findFile(archiveName) ?: createFile(archiveName)?.let { file ->
                    runCatching {
                        archiveTo(file)
                    }.onFailure {
                        file.delete()
                        if (it is CancellationException) throw it
                        logcat(it)
                    }
                }
            }
        }
    }

    suspend fun postArchive(): Boolean {
        val dir = downloadDir
        val archived = saveAsCbz && dir?.findFile(archiveName) != null
        if (archived) {
            dir.listFiles().parMap(concurrency = 10) {
                if (it.name?.matches(filenamePattern) == true) {
                    it.delete()
                }
            }
            dir.findFile(SpiderQueen.SPIDER_INFO_FILENAME)?.delete()
            tempDownloadDir?.delete()
        }
        return archived
    }

    suspend fun exportAsCbz(file: UniFile) =
        downloadDir!!.findFile(archiveName)?.sendTo(file) ?: archiveTo(file)

    private suspend fun archiveTo(file: UniFile) = resourceScope {
        val comicInfo = closeable {
            val f = downloadDir!! / COMIC_INFO_FILE
            if (!f.exists()) {
                writeComicInfo()
            } else if (info.pages == 0) {
                info.pages = readComicInfo(f)!!.pageCount
            }
            f.openFileDescriptor("r")
        }
        val pages = info.pages
        val (fdBatch, names) = (0 until pages).parMap { idx ->
            val f = autoCloseable { getImageSource(idx) ?: throw CancellationException("Image #$idx not found") }
            closeable { f.source.openFileDescriptor("r") }.fd to perFilename(idx, f.type)
        }.run { plus(comicInfo.fd to COMIC_INFO_FILE) }.unzip()
        val arcFd = closeable { file.openFileDescriptor("rw") }
        archiveFdBatch(fdBatch.toIntArray(), names.toTypedArray(), arcFd.fd, pages + 1)
    }

    suspend fun initDownloadDirIfExist() {
        downloadDir = getGalleryDownloadDir(gid)?.takeIf { it.isDirectory }
        tempDownloadDir = AppConfig.getTempDir("$gid")?.takeIf { it.isDirectory }
    }

    suspend fun initDownloadDir() {
        downloadDir = getGalleryDownloadDir(gid) ?: (downloadLocation / info.putToDownloadDir())
        check(downloadDir!!.ensureDir())
    }

    suspend fun writeComicInfo(fetchMetadata: Boolean = true) {
        downloadDir?.run {
            createFile(COMIC_INFO_FILE)?.also {
                runCatching {
                    if (info !is GalleryDetail && fetchMetadata) {
                        withNonCancellableContext {
                            EhEngine.fillGalleryListByApi(listOf(info))
                        }
                    }
                    info.getComicInfo().write(it)
                }.onFailure {
                    logcat(it)
                }
            }
        }
    }
}

private val filenamePattern = Regex("^\\d{8}\\.\\w{3,4}")

fun perFilename(index: Int, extension: String = ""): String {
    return "%08d.%s".format(index + 1, extension)
}

private fun UniFile.findImageFile(index: Int): UniFile? {
    val head = perFilename(index)
    return findFirst { name -> name.startsWith(head) }
}

suspend fun GalleryInfo.putToDownloadDir(): String {
    val title = getSuitableTitle(this)
    val dirname = FileUtils.sanitizeFilename("$gid-$title")
    EhDB.putDownloadDirname(gid, dirname)
    return dirname
}

suspend fun getGalleryDownloadDir(gid: Long): UniFile? {
    val dirname = EhDB.getDownloadDirname(gid) ?: return null
    return downloadLocation / dirname
}
