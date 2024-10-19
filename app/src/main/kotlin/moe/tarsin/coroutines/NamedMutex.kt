package moe.tarsin.coroutines

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import io.ktor.utils.io.pool.DefaultPool
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MutexTracker(mutex: Mutex = Mutex(), private var count: Int = 0) : Mutex by mutex {
    operator fun inc() = apply { count++ }
    operator fun dec() = apply { count-- }
    val isFree
        get() = count == 0
}

object MutexPool : DefaultPool<MutexTracker>(capacity = 32) {
    override fun produceInstance() = MutexTracker()
    override fun validateInstance(mutex: MutexTracker) {
        check(!mutex.isLocked)
        check(mutex.isFree)
    }
}

@JvmInline
value class NamedMutex<K>(val active: MutableScatterMap<K, MutexTracker> = mutableScatterMapOf())

suspend inline fun <K, R> NamedMutex<K>.withLock(key: K, owner: Any? = null, action: () -> R): R {
    val mutex = synchronized(active) { active.getOrPut(key) { MutexPool.borrow() }.inc() }
    return try {
        mutex.withLock(owner, action)
    } finally {
        synchronized(active) {
            mutex.dec()
            if (mutex.isFree) {
                active.remove(key)
                MutexPool.recycle(mutex)
            }
        }
    }
}
