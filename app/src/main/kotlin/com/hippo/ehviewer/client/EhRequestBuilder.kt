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
package com.hippo.ehviewer.client

import com.hippo.ehviewer.EhApplication.Companion.ktorClient
import com.hippo.ehviewer.EhApplication.Companion.noRedirectKtorClient
import com.hippo.ehviewer.Settings
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.ParametersBuilder
import io.ktor.http.content.TextContent
import io.ktor.http.userAgent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import okio.BufferedSource

inline fun JsonObjectBuilder.array(name: String, builder: JsonArrayBuilder.() -> Unit) = put(name, buildJsonArray(builder))

suspend inline fun <reified T> HttpStatement.executeAndParseAs() = execute { it.bodyAsText().parseAs<T>() }
inline fun <reified T> BufferedSource.parseAs(): T = json.decodeFromBufferedSource(this)
inline fun <reified T> String.parseAs(): T = json.decodeFromString(this)

val json = Json { ignoreUnknownKeys = true }

const val CHROME_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36"
const val CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
const val CHROME_ACCEPT_LANGUAGE = "en-US,en;q=0.5"

fun HttpRequestBuilder.applyEhConfig(referer: String?, origin: String?) {
    method = HttpMethod.Get
    header("Referer", referer)
    header("Origin", origin)
    userAgent(Settings.userAgent)
    header(HttpHeaders.Accept, CHROME_ACCEPT)
    header(HttpHeaders.AcceptLanguage, CHROME_ACCEPT_LANGUAGE)
}

suspend inline fun ehRequest(
    url: String,
    referer: String? = null,
    origin: String? = null,
    verbose: Boolean = true,
    builder: HttpRequestBuilder.() -> Unit = {},
) = ktorClient.prepareRequest(url) {
    applyEhConfig(referer, origin)
    apply(builder)
}.also { if (verbose) logcat("EhRequest") { url } }

suspend inline fun noRedirectEhRequest(
    url: String,
    referer: String? = null,
    origin: String? = null,
    builder: HttpRequestBuilder.() -> Unit = {},
) = noRedirectKtorClient.prepareRequest(url) {
    applyEhConfig(referer, origin)
    apply(builder)
}.also { logcat("EhRequest") { url } }

inline fun HttpRequestBuilder.formBody(builder: ParametersBuilder.() -> Unit) {
    method = HttpMethod.Post
    val parameters = ParametersBuilder().apply(builder).build()
    setBody(FormDataContent(parameters))
}

inline fun HttpRequestBuilder.jsonBody(builder: JsonObjectBuilder.() -> Unit) {
    method = HttpMethod.Post
    val json = buildJsonObject(builder).toString()
    setBody(TextContent(text = json, contentType = ContentType.Application.Json))
}

fun HttpRequestBuilder.multipartBody(builder: FormBuilder.() -> Unit) {
    method = HttpMethod.Post
    setBody(MultiPartFormDataContent(formData(builder)))
}
