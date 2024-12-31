package com.hippo.ehviewer.ui.tools

import androidx.collection.MutableObjectIntMap
import androidx.collection.mutableObjectIntMapOf
import arrow.fx.coroutines.fixedRate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.map
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
                val window = passed.coerceAtMost(window)
                val cutoff = now - window
                received.removeIf { time, _ -> time < cutoff }
                received.fold(0) { total, _, v -> total + v } / (window / 1.seconds)
            } ?: 0f
        }
    }
}

inline fun <T, R> MutableObjectIntMap<T>.fold(initial: R, operation: (acc: R, T, Int) -> R): R {
    var accumulator = initial
    forEach { key, value -> accumulator = operation(accumulator, key, value) }
    return accumulator
}
