package moe.tarsin.coroutines

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface Pool<T, K> {
    fun acquire(key: K): T
    fun release(key: K, lock: T)
}

interface LockPool<Lock, K> : Pool<Lock, K> {
    suspend fun Lock.lock()
    fun Lock.unlock()
}

suspend inline fun <Lock, K, R> LockPool<Lock, K>.withLock(key: K, action: () -> R): R {
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
