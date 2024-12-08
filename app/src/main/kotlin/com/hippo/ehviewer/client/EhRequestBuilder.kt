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
import com.hippo.ehviewer.util.bodyAsUtf8Text
import com.hippo.ehviewer.util.ensureSuccess
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.ParametersBuilder
import io.ktor.http.content.TextContent
import kotlinx.coroutines.cancel
import kotlinx.io.Source
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.io.decodeFromSource

inline fun JsonObjectBuilder.array(name: String, builder: JsonArrayBuilder.() -> Unit) = put(name, buildJsonArray(builder))

suspend inline fun <reified T> HttpStatement.executeAndParseAs() = executeSafely {
    it.status.ensureSuccess()
    it.bodyAsUtf8Text().parseAs<T>()
}

inline fun <reified T> Source.parseAs(): T = json.decodeFromSource(this)
inline fun <reified T> String.parseAs(): T = json.decodeFromString(this)

val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun HttpRequestBuilder.applyEhConfig(referer: String?, origin: String?) {
    header("Referer", referer)
    header("Origin", origin)
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

suspend inline fun <T> HttpStatement.executeSafely(
    crossinline block: suspend (response: HttpResponse) -> T,
) = execute { resp ->
    try {
        block(resp)
    } finally {
        resp.coroutineContext.cancel()
    }
}

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
