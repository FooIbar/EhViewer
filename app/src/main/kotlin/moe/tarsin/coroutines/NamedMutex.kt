package moe.tarsin.coroutines

import io.ktor.utils.io.pool.DefaultPool
import kotlinx.coroutines.sync.Mutex

class MutexWithCounter() : Mutex by Mutex(), Counter by counter()

object MutexPool : DefaultPool<MutexWithCounter>(capacity = 32) {
    override fun produceInstance() = MutexWithCounter()
    override fun validateInstance(mutex: MutexWithCounter) {
        check(!mutex.isLocked)
        check(mutex.isFree)
    }
}

class NamedMutex<K>() : LockTracker<MutexWithCounter, K>() {
    override suspend fun MutexWithCounter.lock() = lock()
    override fun MutexWithCounter.unlock() = unlock()
    override fun new() = MutexPool.borrow()
    override fun free(e: MutexWithCounter) = MutexPool.recycle(e)
}
