/*
 * Copyright 2019 Hippo Seven
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
package com.hippo.ehviewer.client

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hippo.ehviewer.EhApplication.Companion.ktorClient
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.TagNamespace
import com.hippo.ehviewer.ui.screen.implicit
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.copyTo
import com.hippo.ehviewer.util.ensureSuccess
import com.hippo.ehviewer.util.sha1
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.tarsin.coroutines.runSuspendCatching
import okio.BufferedSource
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import splitties.init.appCtx

object EhTagDatabase : CoroutineScope {
    private const val NAMESPACE_PREFIX = "n"
    private val PREFIXES = arrayOf(
        "-",
        "~",
        "tag:",
        "weak:",
    )
    private lateinit var tagGroups: Map<String, Map<String, String>>
    private val updateLock = Mutex()
    override val coroutineContext = Dispatchers.IO + Job()

    var initialized by mutableStateOf(false)

    fun getTranslation(prefix: String? = NAMESPACE_PREFIX, tag: String?): String? = tagGroups[prefix]?.get(tag)?.trim()?.ifEmpty { null }

    context(Context)
    fun suggestion(rawKeyword: String, expectTranslate: Boolean): Sequence<Pair<String, String?>> {
        if (!initialized) return emptySequence()
        val translate = expectTranslate && isTranslatable(implicit<Context>())
        val keyword = PREFIXES.fold(rawKeyword) { kwd, pfx -> kwd.removePrefix(pfx) }
        val prefix = rawKeyword.dropLast(keyword.length)
        val ns = keyword.substringBefore(':')
        val tag = keyword.drop(ns.length + 1)
        val nsPrefix = TagNamespace.from(ns)?.prefix ?: ns
        val tags = tagGroups[nsPrefix.takeIf { tag.isNotEmpty() && it != NAMESPACE_PREFIX }]
        fun suggestOnce(exactly: Boolean) = let {
            fun lookup(tags: Map<String, String>, keyword: String) = when {
                exactly -> tags[keyword]?.let { t -> sequenceOf(keyword to t.takeIf { translate }) } ?: emptySequence()
                translate -> tags.asSequence().filter { (tag, hint) ->
                    tag != keyword && (tag.containsIgnoreSpace(keyword) || hint.containsIgnoreSpace(keyword))
                }.map { (tag, hint) -> tag to hint }
                else -> tags.keys.asSequence().filter { tag ->
                    tag != keyword && tag.containsIgnoreSpace(keyword)
                }.map { tag -> tag to null }
            }
            if (tags != null) {
                lookup(tags, tag).map { (tag, hint) -> "$prefix$nsPrefix:$tag" to hint }
            } else {
                tagGroups.asSequence().flatMap { (nsPrefix, tags) ->
                    if (nsPrefix != NAMESPACE_PREFIX) {
                        lookup(tags, keyword).map { (tag, hint) -> "$prefix$nsPrefix:$tag" to hint }
                    } else {
                        lookup(tags, keyword).map { (ns, hint) -> "$prefix$ns:" to hint }
                    }
                }
            }
        }
        return suggestOnce(true) + suggestOnce(false)
    }

    private fun String.removeSpace(): String = replace(" ", "")

    private fun String.containsIgnoreSpace(other: String, ignoreCase: Boolean = true): Boolean = removeSpace().contains(other.removeSpace(), ignoreCase)

    private fun getMetadata(context: Context): Array<String>? = context.resources.getStringArray(R.array.tag_translation_metadata)
        .takeIf { it.size == 4 }

    fun isTranslatable(context: Context): Boolean = context.resources.getBoolean(R.bool.tag_translatable)

    private fun getFileContent(file: File): String? = runCatching {
        file.source().buffer().use { it.readUtf8() }
    }.getOrNull()

    private fun checkData(sha1: String?, data: File): Boolean = sha1 != null && sha1 == data.toOkioPath().sha1()

    private suspend fun save(client: HttpClient, url: String, file: File) {
        runCatching {
            client.prepareGet(url).executeSafely {
                it.status.ensureSuccess()
                it.bodyAsChannel().copyTo(file)
            }
        }.onFailure {
            file.delete()
        }.getOrThrow()
    }

    suspend fun update() {
        updateLock.withLock {
            updateInternal()
        }
    }

    private fun issueUpdateInMemoryData(file: File? = null) {
        val dataFile = file ?: getMetadata(appCtx)?.let { metadata ->
            val dataName = metadata[2]
            val dir = AppConfig.getFilesDir("tag-translations")
            File(dir, dataName).takeIf { it.exists() }
        } ?: return
        tagGroups = dataFile.source().buffer().use(BufferedSource::parseAs)
        initialized = true
    }

    init {
        launch {
            updateLock.withLock {
                runSuspendCatching {
                    issueUpdateInMemoryData()
                }.onFailure {
                    logcat(it)
                }
                updateInternal()
            }
        }
    }

    private suspend fun updateInternal() {
        getMetadata(appCtx)?.let { metadata ->
            val sha1Name = metadata[0]
            val sha1Url = metadata[1]
            val dataName = metadata[2]
            val dataUrl = metadata[3]

            val dir = AppConfig.getFilesDir("tag-translations")
            checkNotNull(dir)
            val sha1File = File(dir, sha1Name)
            val dataFile = File(dir, dataName)

            runSuspendCatching {
                // Check current sha1 and current data
                val sha1 = getFileContent(sha1File)
                if (!checkData(sha1, dataFile)) {
                    FileUtils.delete(sha1File)
                    FileUtils.delete(dataFile)
                }

                // Save new sha1
                val tempSha1File = File(dir, "$sha1Name.tmp")
                save(ktorClient, sha1Url, tempSha1File)
                val tempSha1 = getFileContent(tempSha1File)

                // Check new sha1 and current sha1
                if (tempSha1 == sha1) {
                    // The data is the same
                    FileUtils.delete(tempSha1File)
                    return
                }

                // Save new data
                val tempDataFile = File(dir, "$dataName.tmp")
                save(ktorClient, dataUrl, tempDataFile)

                // Check new sha1 and new data
                if (!checkData(tempSha1, tempDataFile)) {
                    FileUtils.delete(tempSha1File)
                    FileUtils.delete(tempDataFile)
                    return
                }

                // Replace current sha1 and current data with new sha1 and new data
                FileUtils.delete(sha1File)
                FileUtils.delete(dataFile)
                tempSha1File.renameTo(sha1File)
                tempDataFile.renameTo(dataFile)

                // Read new EhTagDatabase
                issueUpdateInMemoryData(dataFile)
            }.onFailure {
                logcat(it)
            }
        }
    }
}
