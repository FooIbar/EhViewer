package com.hippo.ehviewer.spider

import androidx.collection.MutableObjectIntMap
import androidx.collection.mutableObjectIntMapOf
import arrow.fx.coroutines.fixedRate
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.executeSafely
import com.hippo.ehviewer.ktor.reset
import com.hippo.ehviewer.util.LowSpeedException
import com.hippo.ehviewer.util.ensureSuccess
import io.ktor.client.plugins.ConnectTimeoutException
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import kotlin.concurrent.atomics.AtomicReference
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

class SpeedTracker(val window: Duration = 1.seconds) {
    private val mutex = Mutex()
    private val received = mutableObjectIntMapOf<Instant>()
    private var start: Instant? = null

    suspend fun reset() = mutex.withLock {
        received.clear()
        start = null
    }

    suspend fun track(bytes: Int) = Clock.System.now().let { now ->
        mutex.withLock {
            start = start ?: now
            received[now] = bytes
        }
    }

    fun speedFlow(sample: Duration = 1.seconds) = fixedRate(sample).mapNotNull {
        mutex.withLock {
            start?.let { start ->
                val now = Clock.System.now()
                val passed = now - start
                val window = passed.coerceIn(10.milliseconds, window)
                val cutoff = now - window
                received.removeIf { time, _ -> time < cutoff }
                (received.fold(0) { total, _, v -> total + v } / (window / 1.seconds)).toLong()
            }
        }
    }
}

private typealias OnTimeout = (IOException) -> Unit

suspend inline fun <R> timeoutBySpeed(
    url: String,
    crossinline request: suspend (HttpRequestBuilder.() -> Unit) -> HttpStatement,
    crossinline l: suspend (Long, Long, Int) -> Unit,
    crossinline f: suspend (HttpResponse) -> R,
    noinline t: OnTimeout = { throw it },
) = coroutineScope {
    val onTimeout = AtomicReference<OnTimeout?>(t).let { { e: IOException -> it.exchange(null)?.invoke(e) } }
    val watchdog = launch {
        val timeout = 10.seconds
        delay(timeout)
        onTimeout(ConnectTimeoutException(url, timeout.inWholeMilliseconds))
    }
    val tracker = SpeedTracker(2.seconds)
    request {
        var prev = 0L
        onDownload { done, total ->
            val bytesRead = (done - prev).toInt()
            if (done == total!!) {
                tracker.reset()
            } else {
                tracker.track(bytesRead)
            }
            l(total, done, bytesRead)
            prev = done
        }
        timeout { reset() }
    }.executeSafely { resp ->
        watchdog.cancel()
        resp.status.ensureSuccess()
        val speedWatchdog = launch {
            val timeoutSpeed = speedLevelToSpeed(Settings.timeoutSpeed.value) * 1024L
            tracker.speedFlow().collect { speed ->
                if (speed < timeoutSpeed) {
                    onTimeout(LowSpeedException(url, speed))
                }
            }
        }
        try {
            f(resp)
        } finally {
            speedWatchdog.cancel()
        }
    }
}

const val MIN_SPEED_LEVEL = 3

fun speedLevelToSpeed(level: Int) = if (level == MIN_SPEED_LEVEL) 0 else 2f.pow(level).roundToInt()

inline fun <T, R> MutableObjectIntMap<T>.fold(initial: R, operation: (acc: R, T, Int) -> R): R {
    var accumulator = initial
    forEach { key, value -> accumulator = operation(accumulator, key, value) }
    return accumulator
}
