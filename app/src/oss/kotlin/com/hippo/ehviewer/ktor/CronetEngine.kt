@file:Suppress("NewApi")

package com.hippo.ehviewer.ktor

import android.net.http.HttpException
import android.net.http.UploadDataProvider
import android.net.http.UploadDataSink
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import com.hippo.ehviewer.cronet.cronetDispatcher
import com.hippo.ehviewer.cronet.cronetHttpClient
import com.hippo.ehviewer.cronet.cronetHttpClientExecutor
import com.hippo.ehviewer.cronet.pool
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headers
import io.ktor.util.date.GMTDate
import io.ktor.util.flattenForEach
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.pool.useInstance
import io.ktor.utils.io.writer
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

object CronetEngine : HttpClientEngineBase("Cronet") {
    override val config = HttpClientEngineConfig()
    override val dispatcher = cronetDispatcher

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = callContext()

        return executeHttpRequest(callContext, data)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun executeHttpRequest(
        callContext: CoroutineContext,
        data: HttpRequestData,
    ): HttpResponseData = suspendCancellableCoroutine { continuation ->
        val requestTime = GMTDate()

        val callback = object : UrlRequest.Callback {
            lateinit var chunkChan: Channel<ByteBuffer>
            override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                continuation.resume(
                    info.toHttpResponseData(
                        requestTime = requestTime,
                        callContext = callContext,
                    ),
                )
            }

            override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                val channel = GlobalScope.writer(callContext) {
                    pool.useInstance {
                        chunkChan = Channel(1)
                        request.read(it)
                        chunkChan.consumeEach { buffer ->
                            buffer.flip()
                            channel.writeFully(buffer)
                            buffer.clear()
                            request.read(buffer)
                        }
                    }
                }.channel
                continuation.resume(
                    info.toHttpResponseData(
                        requestTime = requestTime,
                        callContext = callContext,
                        responseBody = channel,
                    ),
                )
            }

            override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
                chunkChan.trySend(byteBuffer).getOrThrow()
            }

            override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                chunkChan.close()
            }

            override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: HttpException) {
                if (::chunkChan.isInitialized) {
                    chunkChan.close(error)
                } else {
                    continuation.resumeWithException(error)
                }
            }

            override fun onCanceled(p0: UrlRequest, p1: UrlResponseInfo?) {
                // No-op
            }
        }

        val request = cronetHttpClient.newUrlRequestBuilder(data.url.toString(), cronetHttpClientExecutor, callback).apply {
            setHttpMethod(data.method.value)
            data.headers.flattenForEach { key, value -> addHeader(key, value) }
            data.body.contentType?.let { addHeader(HttpHeaders.ContentType, it.toString()) }
            data.body.contentLength?.let { addHeader(HttpHeaders.ContentLength, it.toString()) }
            data.body.toUploadDataProvider(callContext)?.let { setUploadDataProvider(it, cronetHttpClientExecutor) }
        }.build()
        request.start()
        callContext[Job]!!.invokeOnCompletion { request.cancel() }
        continuation.invokeOnCancellation { request.cancel() }
    }
}

private fun UrlResponseInfo.toHttpResponseData(
    requestTime: GMTDate,
    callContext: CoroutineContext,
    responseBody: ByteReadChannel = ByteReadChannel.Empty,
): HttpResponseData {
    return HttpResponseData(
        statusCode = HttpStatusCode.fromValue(httpStatusCode),
        requestTime = requestTime,
        headers = headers {
            headers.asMap.forEach { (key, value) ->
                appendAll(key, value)
            }
        },
        version = when (negotiatedProtocol) {
            "h2" -> HttpProtocolVersion.HTTP_2_0
            "h3" -> HttpProtocolVersion.QUIC
            "quic/1+spdy/3" -> HttpProtocolVersion.SPDY_3
            else -> HttpProtocolVersion.HTTP_1_1
        },
        body = responseBody,
        callContext = callContext,
    )
}

@OptIn(DelicateCoroutinesApi::class)
private fun OutgoingContent.toUploadDataProvider(callContext: CoroutineContext): UploadDataProvider? = when (this) {
    is OutgoingContent.NoContent -> null
    is OutgoingContent.ByteArrayContent -> object : UploadDataProvider() {
        val buffer = ByteBuffer.wrap(bytes()).slice()
        override fun getLength() = buffer.limit().toLong()
        override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
            byteBuffer.put(buffer)
            uploadDataSink.onReadSucceeded(false)
        }
        override fun rewind(uploadDataSink: UploadDataSink) {
            buffer.position(0)
            uploadDataSink.onRewindSucceeded()
        }
    }
    is OutgoingContent.WriteChannelContent -> object : UploadDataProvider() {
        fun launch() = GlobalScope.writer(callContext) { writeTo(channel) }.channel
        var chan = launch()
        override fun getLength() = contentLength!!
        override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
            GlobalScope.launch(callContext) {
                chan.readAvailable(byteBuffer)
                uploadDataSink.onReadSucceeded(false)
            }
        }
        override fun rewind(uploadDataSink: UploadDataSink) {
            chan = launch()
            uploadDataSink.onRewindSucceeded()
        }
    }
    else -> error("UnsupportedContentType $this")
}
