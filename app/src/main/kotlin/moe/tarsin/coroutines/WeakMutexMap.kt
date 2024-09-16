package moe.tarsin.coroutines

import java.util.WeakHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@JvmInline
value class WeakMutexMap<T>(val mutexes: WeakHashMap<T, Mutex> = WeakHashMap())

suspend inline fun <T, K> WeakMutexMap<K>.withLock(key: K, owner: Any? = null, action: () -> T) = synchronized(mutexes) {
    mutexes[key] ?: Mutex().also { mutexes[key] = it }
}.withLock(owner, action)
