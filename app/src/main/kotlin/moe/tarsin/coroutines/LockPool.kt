package moe.tarsin.coroutines

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface Counter {
    var ref: Int
    val isFree get() = ref == 0
}

fun counter() = object : Counter {
    override var ref = 0
}

interface Pool<T : Counter, K> {
    fun T.inc() = apply { ref++ }
    fun T.dec() = apply { ref-- }
    fun acquire(key: K): T
    fun release(key: K, lock: T)
}

inline fun <T : Counter, K, R> Pool<T, K>.use(key: K, block: T.() -> R): R {
    val inst = acquire(key)
    try {
        return block(inst)
    } finally {
        release(key, inst)
    }
}

interface LockPool<Lock : Counter, K> : Pool<Lock, K> {
    suspend fun Lock.lock()
    fun Lock.unlock()
}

suspend inline fun <Lock : Counter, K, R> LockPool<Lock, K>.withLock(key: K, action: () -> R): R {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val lock = acquire(key)
    return try {
        lock.lock()
        return try {
            action()
        } finally {
            lock.unlock()
        }
    } finally {
        release(key, lock)
    }
}
