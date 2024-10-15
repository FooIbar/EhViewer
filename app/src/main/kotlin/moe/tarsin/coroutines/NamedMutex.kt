package moe.tarsin.coroutines

import androidx.collection.mutableScatterMapOf
import io.ktor.utils.io.pool.DefaultPool
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MutexTracker(
    val mutex: Mutex = Mutex(),
    var count: Int = 0,
)

val MutexTracker.free
    get() = count == 0

operator fun MutexTracker.inc() = apply { count++ }
operator fun MutexTracker.dec() = apply { count-- }

class MutexPool(capacity: Int = 16) : DefaultPool<MutexTracker>(capacity) {
    override fun produceInstance() = MutexTracker()
    override fun validateInstance(mutex: MutexTracker) {
        check(!mutex.mutex.isLocked)
        check(mutex.free)
    }
}

class NamedMutex<K> {
    val pool = MutexPool()
    val active = mutableScatterMapOf<K, MutexTracker>()
    val lock = Any()
}

suspend inline fun <K, R> NamedMutex<K>.withLock(key: K, owner: Any? = null, action: () -> R): R {
    val mutex = synchronized(lock) { active.getOrPut(key) { pool.borrow() }.inc() }
    return try {
        mutex.mutex.withLock(owner, action)
    } finally {
        synchronized(lock) {
            mutex.dec()
            if (mutex.free) {
                active.remove(key)
                pool.recycle(mutex)
            }
        }
    }
}
