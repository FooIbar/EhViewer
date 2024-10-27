package moe.tarsin.coroutines

import androidx.collection.mutableScatterMapOf
import io.ktor.utils.io.pool.DefaultPool
import kotlinx.coroutines.sync.Semaphore

class SemaphoreTracker(semaphore: Semaphore, private var count: Int = 0) : Semaphore by semaphore {
    operator fun inc() = apply { count++ }
    operator fun dec() = apply { count-- }
    val isFree
        get() = count == 0
}

class SemaphorePool(val permits: Int) : DefaultPool<SemaphoreTracker>(capacity = 32) {
    override fun produceInstance() = SemaphoreTracker(semaphore = Semaphore(permits = permits))
    override fun validateInstance(semaphore: SemaphoreTracker) {
        check(semaphore.availablePermits == permits)
        check(semaphore.isFree)
    }
}

class NamedSemaphore<K>(val permits: Int) : LockPool<SemaphoreTracker, K> {
    val pool = SemaphorePool(permits = permits)
    val active = mutableScatterMapOf<K, SemaphoreTracker>()
    override fun acquire(key: K) = synchronized(active) { active.getOrPut(key) { pool.borrow() }.inc() }
    override fun release(key: K, lock: SemaphoreTracker) = synchronized(active) {
        lock.dec()
        if (lock.isFree) {
            active.remove(key)
            pool.recycle(lock)
        }
    }
    override suspend fun SemaphoreTracker.lock() = acquire()
    override fun SemaphoreTracker.tryLock() = tryAcquire()
    override fun SemaphoreTracker.unlock() = release()
}
