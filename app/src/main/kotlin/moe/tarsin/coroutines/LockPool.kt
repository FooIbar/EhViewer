package moe.tarsin.coroutines

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface LockPool<Lock, K> {
    fun acquire(key: K): Lock
    fun release(key: K, lock: Lock)
    suspend fun Lock.lock()
    fun Lock.tryLock(): Boolean
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

suspend inline fun <Lock, K, R> LockPool<Lock, K>.withLockNeedSuspend(key: K, action: () -> R): Pair<R, Boolean> {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val lock = acquire(key)
    return try {
        val mustSuspend = !lock.tryLock()
        if (mustSuspend) lock.lock()
        return try {
            action() to mustSuspend
        } finally {
            lock.unlock()
        }
    } finally {
        release(key, lock)
    }
}
