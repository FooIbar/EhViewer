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
import com.hippo.ehviewer.EhApplication.Companion.noRedirectOkHttpClient
import com.hippo.ehviewer.EhApplication.Companion.okHttpClient
import com.hippo.ehviewer.Settings
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.ParametersBuilder
import io.ktor.http.content.TextContent
import io.ktor.http.userAgent
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.Response
import okhttp3.executeAsync
import okio.BufferedSource

inline fun ehRequest(url: String, referer: String? = null, origin: String? = null, builder: Request.Builder.() -> Unit = {}) = Request.Builder().url(url).apply {
    addHeader("User-Agent", Settings.userAgent)
    addHeader("Accept", CHROME_ACCEPT)
    addHeader("Accept-Language", CHROME_ACCEPT_LANGUAGE)
    referer?.let { addHeader("Referer", it) }
    origin?.let { addHeader("Origin", it) }
}.apply(builder).build()

inline fun multipartBody(builder: MultipartBody.Builder.() -> Unit) = MultipartBody.Builder().apply(builder).build()

inline fun JsonObjectBuilder.array(name: String, builder: JsonArrayBuilder.() -> Unit) = put(name, buildJsonArray(builder))

suspend inline fun <R> Request.execute(block: Response.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return okHttpClient.newCall(this).executeAsync().use(block)
}

suspend inline fun <R> Request.executeNoRedirect(block: Response.() -> R) = noRedirectOkHttpClient.newCall(this).executeAsync().use(block)

suspend inline fun <reified T> Request.executeAndParseAs() = execute { parseAs<T>() }

inline fun <reified T> Response.parseAs(): T = body.source().parseAs()
inline fun <reified T> BufferedSource.parseAs(): T = json.decodeFromBufferedSource(this)
inline fun <reified T> String.parseAs(): T = json.decodeFromString(this)

val json = Json { ignoreUnknownKeys = true }

const val CHROME_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36"
const val CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
const val CHROME_ACCEPT_LANGUAGE = "en-US,en;q=0.5"

suspend inline fun statement(
    url: String,
    referer: String? = null,
    origin: String? = null,
    builder: HttpRequestBuilder.() -> Unit = {},
) = ktorClient.prepareRequest(url) {
    method = HttpMethod.Get
    header("Referer", referer)
    header("Origin", origin)
    userAgent(Settings.userAgent)
    header(HttpHeaders.Accept, CHROME_ACCEPT)
    header(HttpHeaders.AcceptLanguage, CHROME_ACCEPT_LANGUAGE)
    apply(builder)
}

fun HttpRequestBuilder.formBody(builder: ParametersBuilder.() -> Unit) {
    method = HttpMethod.Post
    val parameters = ParametersBuilder().apply(builder).build()
    setBody(FormDataContent(parameters))
}

fun HttpRequestBuilder.jsonBody(builder: JsonObjectBuilder.() -> Unit) {
    method = HttpMethod.Post
    val json = buildJsonObject(builder).toString()
    setBody(TextContent(text = json, contentType = ContentType.Application.Json))
}
