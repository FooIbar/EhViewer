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
import com.hippo.ehviewer.util.copyTo
import com.hippo.ehviewer.util.ensureSuccess
import com.hippo.ehviewer.util.sha1
import com.hippo.ehviewer.util.utf8
import com.hippo.files.delete
import com.hippo.files.moveTo
import com.hippo.files.read
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Source
import moe.tarsin.coroutines.runSuspendCatching
import okio.Path
import splitties.init.appCtx

object EhTagDatabase : CoroutineScope {
    override val coroutineContext = Dispatchers.IO + Job()
    private const val NAMESPACE_PREFIX = "n"
    private val PREFIXES = arrayOf(
        "-",
        "~",
        "tag:",
        "weak:",
    )
    private lateinit var tagGroups: Map<String, Map<String, String>>
    private val updateLock = Mutex()
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

    private fun metadata(context: Context): Array<String>? = context.resources.getStringArray(R.array.tag_translation_metadata)
        .takeIf { it.size == 4 }

    fun isTranslatable(context: Context): Boolean = context.resources.getBoolean(R.bool.tag_translatable)

    private suspend fun fetch(client: HttpClient, url: String, file: Path) {
        runCatching {
            client.prepareGet(url).executeSafely {
                it.status.ensureSuccess()
                it.bodyAsChannel().copyTo(file)
            }
        }.onFailure {
            file.delete()
        }.getOrThrow()
    }

    fun launchUpdate() = launch {
        updateLock.withLock {
            runSuspendCatching {
                atomicallyUpdate()
            }.onFailure {
                logcat(it)
            }
        }
    }

    private suspend fun atomicallyUpdate() {
        val (sha1Name, sha1Url, dataName, dataUrl) = metadata(appCtx) ?: return
        val workdir = AppConfig.tagTranslationsDir
        val sha1File = workdir / sha1Name
        val dataFile = workdir / dataName

        fun updateInMemoryData() {
            tagGroups = dataFile.read(Source::parseAs)
            initialized = true
        }

        // Check current sha1 and current data
        val currentSha1 = runSuspendCatching {
            dataFile.sha1().also {
                check(sha1File.utf8() == it)
                if (!initialized) updateInMemoryData()
            }
        }.onFailure {
            sha1File.delete()
            dataFile.delete()
        }.getOrNull()

        // Save new sha1
        val tempSha1File = workdir / "$sha1Name.tmp"
        fetch(ktorClient, sha1Url, tempSha1File)
        val tempSha1 = tempSha1File.utf8()

        // Check new sha1 and current sha1
        if (tempSha1 == currentSha1) {
            // The data is the same
            tempSha1File.delete()
            return
        }

        // Save new data
        val tempDataFile = workdir / "$dataName.tmp"
        fetch(ktorClient, dataUrl, tempDataFile)

        // Check new sha1 and new data
        runSuspendCatching {
            check(tempDataFile.sha1() == tempSha1)
        }.onFailure {
            tempSha1File.delete()
            tempDataFile.delete()
            throw it
        }.onSuccess {
            // Replace current sha1 and current data with new sha1 and new data
            sha1File.delete()
            dataFile.delete()
            tempSha1File moveTo sha1File
            tempDataFile moveTo dataFile

            // Read new EhTagDatabase
            updateInMemoryData()
        }
    }
}
