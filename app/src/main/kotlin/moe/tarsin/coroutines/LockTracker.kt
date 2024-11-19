package moe.tarsin.coroutines

import androidx.collection.mutableScatterMapOf
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface Counter {
    var ref: Int
    val isFree get() = ref == 0
}

fun counter() = object : Counter {
    override var ref = 0
}

abstract class Tracker<T : Counter, K> {
    private val active = mutableScatterMapOf<K, T>()
    abstract fun new(): T
    abstract fun free(e: T)
    fun acquire(key: K) = synchronized(active) { active.getOrPut(key, ::new).apply { ref++ } }
    fun release(key: K, e: T) = synchronized(active) {
        e.ref--
        if (e.isFree) {
            free(e)
            active.remove(key)
        }
    }
}

inline fun <T : Counter, K, R> Tracker<T, K>.use(key: K, block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val inst = acquire(key)
    try {
        return block(inst)
    } finally {
        release(key, inst)
    }
}

abstract class LockTracker<Lock : Counter, K> : Tracker<Lock, K>() {
    abstract suspend fun Lock.lock()
    abstract fun Lock.unlock()
}

suspend inline fun <Lock : Counter, K, R> LockTracker<Lock, K>.withLock(key: K, action: () -> R): R {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    use(key) {
        lock()
        try {
            return action()
        } finally {
            unlock()
        }
    }
}
