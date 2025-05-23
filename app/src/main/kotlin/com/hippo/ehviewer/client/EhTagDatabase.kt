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
import arrow.core.Either
import com.hippo.ehviewer.EhApplication.Companion.ktorClient
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.TagNamespace
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.bodyAsUtf8Text
import com.hippo.ehviewer.util.ensureSuccess
import com.hippo.ehviewer.util.utf8
import com.hippo.files.metadataOrNull
import com.hippo.files.read
import com.hippo.files.write
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.client.request.prepareGet
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.Source
import kotlinx.io.writeString
import kotlinx.serialization.json.io.encodeToSink
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.tarsin.coroutines.runSuspendCatching
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

    context(ctx: Context)
    fun suggestion(rawKeyword: String, expectTranslate: Boolean): Sequence<Pair<String, String?>> {
        if (!initialized) return emptySequence()
        val translate = expectTranslate && translatable
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

    private fun metadata(context: Context): Array<String> = context.resources.getStringArray(R.array.tag_translation_metadata)

    context(ctx: Context)
    val translatable
        get() = ctx.resources.getBoolean(R.bool.tag_translatable)

    private suspend fun fetch(url: String) = ktorClient.prepareGet(url).executeSafely {
        it.status.ensureSuccess()
        it.bodyAsUtf8Text()
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
        val (sha1Name, sha1Url, dataName, dataUrl) = metadata(appCtx)
        val workdir = AppConfig.tagTranslationsDir
        val sha1File = workdir / sha1Name
        val dataFile = workdir / dataName

        if (!initialized) {
            Either.catch {
                tagGroups = dataFile.read(Source::parseAs)
                initialized = true

                // Perform only one automatic update (on app startup) per day
                val lastModified = Instant.fromEpochMilliseconds(dataFile.metadataOrNull()!!.lastModifiedAtMillis!!)
                if (Clock.System.now() - lastModified < 1.days) return
            }.onLeft {
                logcat(it)
            }
        }

        val currentSha1 = Either.catch {
            sha1File.utf8()
        }.getOrNull()
        val newSha1 = fetch(sha1Url)
        if (newSha1 == currentSha1) return

        val rawData = fetch(dataUrl)
        tagGroups = json.parseToJsonElement(rawData).jsonObject["data"]!!.jsonArray.associate { e ->
            val data = e.jsonObject
            val namespace = data["namespace"]!!.jsonPrimitive.content
            val prefix = if (namespace == "rows") NAMESPACE_PREFIX else TagNamespace.from(namespace)!!.prefix!!
            prefix to data["data"]!!.jsonObject.mapValues { it.value.jsonObject["name"]!!.jsonPrimitive.content }
        }
        initialized = true
        sha1File.write { writeString(newSha1) }
        dataFile.write { json.encodeToSink(tagGroups, this) }
    }
}
