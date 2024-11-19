package moe.tarsin.coroutines

import io.ktor.utils.io.pool.DefaultPool
import kotlinx.coroutines.sync.Semaphore

class SemaphoreWithCounter(semaphore: Semaphore) : Semaphore by semaphore, Counter by counter()

class SemaphorePool(val permits: Int) : DefaultPool<SemaphoreWithCounter>(capacity = 32) {
    override fun produceInstance() = SemaphoreWithCounter(semaphore = Semaphore(permits = permits))
    override fun validateInstance(semaphore: SemaphoreWithCounter) {
        check(semaphore.availablePermits == permits)
        check(semaphore.isFree)
    }
}

class NamedSemaphore<K>(val permits: Int) : LockTracker<SemaphoreWithCounter, K>() {
    val pool = SemaphorePool(permits = permits)
    override suspend fun SemaphoreWithCounter.lock() = acquire()
    override fun SemaphoreWithCounter.unlock() = release()
    override fun new() = pool.borrow()
    override fun free(e: SemaphoreWithCounter) = pool.recycle(e)
}
