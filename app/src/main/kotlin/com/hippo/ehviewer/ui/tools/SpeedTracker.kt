package com.hippo.ehviewer.ui.tools

import androidx.collection.MutableObjectIntMap
import androidx.collection.mutableObjectIntMapOf
import arrow.fx.coroutines.fixedRate
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.executeSafely
import com.hippo.ehviewer.util.LowSpeedException
import com.hippo.ehviewer.util.ensureSuccess
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.request
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SpeedTracker(val window: Duration = 1.seconds) {
    private val mutex = Mutex()
    private val received = mutableObjectIntMapOf<TimeSource.Monotonic.ValueTimeMark>()
    private var start: TimeSource.Monotonic.ValueTimeMark? = null

    suspend fun reset() = mutex.withLock {
        received.clear()
        start = null
    }

    suspend fun track(bytes: Int) = TimeSource.Monotonic.markNow().let { now ->
        mutex.withLock {
            start = start ?: now
            received[now] = bytes
        }
    }

    fun speedFlow(sample: Duration = 1.seconds) = fixedRate(sample).map {
        mutex.withLock {
            start?.let { start ->
                val now = TimeSource.Monotonic.markNow()
                val passed = now - start
                val window = passed.coerceIn(10.milliseconds, window)
                val cutoff = now - window
                received.removeIf { time, _ -> time < cutoff }
                received.fold(0) { total, _, v -> total + v } / (window / 1.seconds)
            } ?: 0.0
        }
    }
}

suspend inline fun <R> timeoutBySpeed(
    crossinline req: suspend (HttpRequestBuilder.() -> Unit) -> HttpStatement,
    crossinline l: suspend (Long, Long, Int) -> Unit,
    crossinline f: suspend (HttpResponse) -> R,
) = with(SpeedTracker(2.seconds)) {
    var prev = 0L
    req {
        onDownload { done, total ->
            val bytesRead = (done - prev).toInt()
            track(bytesRead)
            l(total!!, done, bytesRead)
            prev = done
        }
        timeout { connectTimeoutMillis = Settings.connTimeout * 1000L }
    }.executeSafely { resp ->
        resp.status.ensureSuccess()
        val timeoutSpeed = Settings.timeoutSpeed * 1024.0
        coroutineScope {
            val work = async { f(resp) }
            val job = launch {
                delay(2.seconds) // Tolerant 4 secs for receiving
                speedFlow(1.seconds).collect { speed ->
                    if (timeoutSpeed != 0.0 && speed < timeoutSpeed) {
                        throw LowSpeedException(
                            resp.request.url.toString(),
                            speed.toLong(),
                        )
                    }
                }
            }
            try {
                work.await()
            } finally {
                job.cancel()
            }
        }
    }
}

inline fun <T, R> MutableObjectIntMap<T>.fold(initial: R, operation: (acc: R, T, Int) -> R): R {
    var accumulator = initial
    forEach { key, value -> accumulator = operation(accumulator, key, value) }
    return accumulator
}
