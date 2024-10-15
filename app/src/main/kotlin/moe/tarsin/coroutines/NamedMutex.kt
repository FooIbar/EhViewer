package moe.tarsin.coroutines

import androidx.collection.mutableScatterMapOf
import io.ktor.utils.io.pool.DefaultPool
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MutexTracker(val mutex: Mutex = Mutex(), var count: Int = 0) {
    operator fun inc() = apply { count++ }
    operator fun dec() = apply { count-- }
}

val MutexTracker.isFree
    get() = count == 0

class MutexPool(capacity: Int) : DefaultPool<MutexTracker>(capacity) {
    override fun produceInstance() = MutexTracker()
    override fun validateInstance(mutex: MutexTracker) {
        check(!mutex.mutex.isLocked)
        check(mutex.isFree)
    }
}

class NamedMutex<K>(capacity: Int) {
    val pool = MutexPool(capacity)
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
            if (mutex.isFree) {
                active.remove(key)
                pool.recycle(mutex)
            }
        }
    }
}
