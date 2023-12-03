@file:Suppress("NewApi")

package com.hippo.ehviewer.cronet

import android.net.http.HttpEngine
import android.net.http.HttpException
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
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
import splitties.init.appCtx

val cronetHttpClient: HttpEngine = HttpEngine.Builder(appCtx).apply {
    setEnableBrotli(true)
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

inline fun cronetRequest(url: String, referer: String? = null, origin: String? = null, conf: UrlRequest.Builder.() -> Unit = {}) = CronetRequest().apply {
    request = cronetHttpClient.newUrlRequestBuilder(url, cronetHttpClientExecutor, callback).apply {
        addHeader("Cookie", EhCookieStore.getCookieHeader(url))
        addHeader("Accept", CHROME_ACCEPT)
        addHeader("Accept-Language", CHROME_ACCEPT_LANGUAGE)
        referer?.let { addHeader("Referer", it) }
        origin?.let { addHeader("Origin", it) }
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
