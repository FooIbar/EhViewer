@file:RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)

package com.hippo.ehviewer.cronet

import android.net.http.HttpEngine
import android.net.http.HttpException
import android.net.http.UploadDataProvider
import android.net.http.UploadDataSink
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import com.hippo.ehviewer.client.CHROME_ACCEPT
import com.hippo.ehviewer.client.CHROME_ACCEPT_LANGUAGE
import com.hippo.ehviewer.client.CHROME_USER_AGENT
import com.hippo.ehviewer.client.EhCookieStore
import java.nio.ByteBuffer
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.RequestBody
import okio.Buffer
import okio.Path.Companion.toOkioPath
import splitties.init.appCtx

val cronetHttpClient: HttpEngine = HttpEngine.Builder(appCtx).apply {
    setEnableBrotli(true)
    val cache = (appCtx.cacheDir.toOkioPath() / "http_cache").toFile().apply { mkdirs() }
    setStoragePath(cache.absolutePath)
    setEnableHttpCache(HttpEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 100 * 1024)
    setUserAgent(CHROME_USER_AGENT)
}.build()

class CronetRequest {
    lateinit var consumer: (ByteBuffer) -> Unit
    lateinit var onResponse: CronetRequest.(UrlResponseInfo) -> Unit
    lateinit var request: UrlRequest
    lateinit var onError: (Throwable) -> Unit
    lateinit var readerCont: Continuation<Unit>
    val callback = object : UrlRequest.Callback {
        override fun onRedirectReceived(req: UrlRequest, info: UrlResponseInfo, url: String) {
            req.followRedirect()
        }

        override fun onResponseStarted(req: UrlRequest, info: UrlResponseInfo) {
            onResponse(info)
        }

        override fun onReadCompleted(req: UrlRequest, info: UrlResponseInfo, data: ByteBuffer) {
            data.flip()
            consumer(data)
        }

        override fun onSucceeded(req: UrlRequest, info: UrlResponseInfo) {
            readerCont.resume(Unit)
        }

        override fun onFailed(req: UrlRequest, info: UrlResponseInfo?, e: HttpException) {
            onError(e)
        }

        override fun onCanceled(req: UrlRequest, info: UrlResponseInfo?) {
            // No-op
        }
    }
}

inline fun cronetRequest(url: String, referer: String? = null, conf: UrlRequest.Builder.() -> Unit = {}) = CronetRequest().apply {
    request = cronetHttpClient.newUrlRequestBuilder(url, cronetHttpClientExecutor, callback).apply {
        addHeader("Cookie", EhCookieStore.getCookieHeader(url.toHttpUrl()))
        addHeader("Accept", CHROME_ACCEPT)
        addHeader("Accept-Language", CHROME_ACCEPT_LANGUAGE)
        referer?.let { addHeader("Referer", it) }
    }.apply(conf).build()
}

suspend inline fun <R> CronetRequest.execute(crossinline callback: suspend CronetRequest.(UrlResponseInfo) -> R): R {
    contract {
        callsInPlace(callback, InvocationKind.EXACTLY_ONCE)
    }
    return coroutineScope {
        suspendCancellableCoroutine { cont ->
            onResponse = { launch { cont.resume(callback(it)) } }
            cont.invokeOnCancellation { request.cancel() }
            onError = { cont.resumeWithException(it) }
            request.start()
        }
    }
}

fun UrlRequest.Builder.withRequestBody(body: RequestBody) {
    addHeader("Content-Type", body.contentType().toString())
    val buffer = Buffer().apply { body.writeTo(this) }
    val provider = object : UploadDataProvider() {
        override fun getLength() = body.contentLength()
        override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
            buffer.read(byteBuffer)
            uploadDataSink.onReadSucceeded(false)
        }
        override fun rewind(uploadDataSink: UploadDataSink) {
            error("OneShot!")
        }
    }
    setUploadDataProvider(provider, cronetHttpClientExecutor)
}

fun UrlRequest.Builder.noCache(): UrlRequest.Builder = setCacheDisabled(true)
fun UrlResponseInfo.getHeadersMap(): Map<String, List<String>> = headers.asMap
