package com.hippo.ehviewer.cronet

import com.hippo.ehviewer.EhApplication
import io.ktor.utils.io.pool.DirectByteBufferPool
import io.ktor.utils.io.pool.useInstance
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

val pool = DirectByteBufferPool(32)

val cronetHttpClientExecutor = EhApplication.baseOkHttpClient.dispatcher.executorService

@Suppress("NewApi")
suspend inline fun CronetRequest.awaitBodyFully(crossinline callback: (ByteBuffer) -> Unit) {
    return pool.useInstance { buffer ->
        suspendCancellableCoroutine { cont ->
            consumer = {
                check(it === buffer)
                buffer.flip()
                callback(it)
                buffer.clear()
                request.read(buffer)
            }
            onError = { readerCont.resumeWithException(it) }
            readerCont = cont
            request.read(buffer)
        }
    }
}

@Suppress("NewApi")
suspend inline fun CronetRequest.awaitBodyFully(buffer: ByteBuffer) {
    return suspendCancellableCoroutine { cont ->
        consumer = {
            request.read(buffer)
            check(buffer.hasRemaining())
        }
        onError = { readerCont.resumeWithException(it) }
        readerCont = cont
        request.read(buffer)
    }
}

suspend inline fun CronetRequest.copyToChannel(chan: FileChannel, crossinline listener: ((Int) -> Unit) = {}) = awaitBodyFully {
    val bytes = chan.write(it)
    listener(bytes)
}
