@file:RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)

package com.hippo.ehviewer.ktor

import android.net.http.HttpEngine
import android.net.http.HttpException
import android.net.http.UploadDataProvider
import android.net.http.UploadDataSink
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import com.hippo.ehviewer.Settings
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.HttpTimeoutCapability
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
import io.ktor.utils.io.pool.DirectByteBufferPool
import io.ktor.utils.io.pool.useInstance
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.job
import kotlinx.coroutines.suspendCancellableCoroutine

class CronetEngine(override val config: CronetConfig) : HttpClientEngineBase("Cronet") {
    // Limit thread to 1 since we are async & non-blocking
    override val dispatcher = Dispatchers.Default.limitedParallelism(1)

    override val supportedCapabilities = setOf(HttpTimeoutCapability)

    private val executor = dispatcher.asExecutor()
    private val pool = DirectByteBufferPool(32)
    private val client by lazy {
        with(config) { HttpEngine.Builder(context).apply(config).build() }
    }

    @InternalAPI
    override suspend fun execute(data: HttpRequestData) = executeHttpRequest(callContext(), data)

    private suspend fun executeHttpRequest(
        callContext: CoroutineContext,
        data: HttpRequestData,
    ) = suspendCancellableCoroutine { continuation ->
        val requestTime = GMTDate()

        val callback = object : UrlRequest.Callback {
            val chunkChan = Channel<ByteBuffer>()
            override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                continuation.resume(
                    info.toHttpResponseData(
                        requestTime = requestTime,
                        callContext = callContext,
                    ),
                )
            }

            override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                continuation.resume(
                    info.toHttpResponseData(
                        requestTime = requestTime,
                        callContext = callContext,
                        responseBody = writer(callContext.job) {
                            pool.useInstance {
                                request.read(it)
                                chunkChan.consumeEach { buffer ->
                                    buffer.flip()
                                    channel.writeFully(buffer)
                                    buffer.clear()
                                    request.read(buffer)
                                }
                            }
                        }.channel,
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
                // Cronet may crash on some devices, fuck xiaomi
                // https://github.com/FooIbar/EhViewer/issues/1826
                if (error.message?.contains("ERR_FILE_NOT_FOUND") == true) {
                    Settings.enableCronet = false
                }
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                } else {
                    chunkChan.close(error)
                }
            }

            override fun onCanceled(request: UrlRequest, info: UrlResponseInfo?) = Unit
        }

        client.newUrlRequestBuilder(data.url.toString(), executor, callback).apply {
            setHttpMethod(data.method.value)
            data.headers.flattenForEach { key, value -> addHeader(key, value) }
            data.body.contentType?.let { addHeader(HttpHeaders.ContentType, it.toString()) }
            data.body.contentLength?.let { addHeader(HttpHeaders.ContentLength, it.toString()) }
            data.body.toUploadDataProvider()?.let { setUploadDataProvider(it, executor) }
        }.build().apply {
            start()
            callContext.job.invokeOnCompletion { cancel() }
        }
    }
}

private fun UrlResponseInfo.toHttpResponseData(
    requestTime: GMTDate,
    callContext: CoroutineContext,
    responseBody: ByteReadChannel = ByteReadChannel.Empty,
) = HttpResponseData(
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

private fun OutgoingContent.toUploadDataProvider() = when (this) {
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
    else -> error("UnsupportedContentType $this")
}
