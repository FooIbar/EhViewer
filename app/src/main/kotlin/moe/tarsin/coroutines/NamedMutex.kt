package moe.tarsin.coroutines

import arrow.core.memoize
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.sync.Mutex

class NamedMutex<T> {
    val innerMutexGetter = { _: T -> Mutex() }.memoize()
}

suspend inline fun <T, K> NamedMutex<K>.withLock(key: K, owner: Any? = null, action: () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    val mutex = synchronized(innerMutexGetter) {
        innerMutexGetter(key)
    }

    mutex.run {
        lock(owner)
        try {
            return action()
        } finally {
            unlock(owner)
        }
    }
}
