package com.hippo.ehviewer.cronet

import io.ktor.utils.io.pool.DirectByteBufferPool
import io.ktor.utils.io.pool.useInstance
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

val pool = DirectByteBufferPool(32)

// Limit thread to 1 since we are async & non-blocking
val cronetDispatcher = Dispatchers.IO.limitedParallelism(1)
val cronetHttpClientExecutor = cronetDispatcher.asExecutor()

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

suspend inline fun CronetRequest.copyToChannel(chan: FileChannel, crossinline listener: ((Int) -> Unit) = {}) = awaitBodyFully {
    val bytes = chan.write(it)
    listener(bytes)
}
