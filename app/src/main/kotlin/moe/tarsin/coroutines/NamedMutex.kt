package moe.tarsin.coroutines

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import io.ktor.utils.io.pool.DefaultPool
import kotlinx.coroutines.sync.Mutex

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

class NamedMutex<K>(val active: MutableScatterMap<K, MutexTracker> = mutableScatterMapOf()) : LockPool<MutexTracker, K> {
    override fun acquire(key: K) = synchronized(active) { active.getOrPut(key) { MutexPool.borrow() }.inc() }
    override fun release(key: K, lock: MutexTracker) = synchronized(active) {
        lock.dec()
        if (lock.isFree) {
            active.remove(key)
            MutexPool.recycle(lock)
        }
    }
    override suspend fun MutexTracker.lock() = lock()
    override fun MutexTracker.tryLock() = tryLock()
    override fun MutexTracker.unlock() = unlock()
}
