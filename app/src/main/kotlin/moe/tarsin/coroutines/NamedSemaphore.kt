package moe.tarsin.coroutines

import androidx.collection.mutableScatterMapOf
import io.ktor.utils.io.pool.DefaultPool
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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

class NamedSemaphore<K>(val permits: Int) {
    val pool = SemaphorePool(permits = permits)
    val active = mutableScatterMapOf<K, SemaphoreTracker>()
}

suspend inline fun <K, R> NamedSemaphore<K>.withPermit(key: K, action: () -> R): R {
    val mutex = synchronized(active) { active.getOrPut(key) { pool.borrow() }.inc() }
    return try {
        mutex.withPermit(action)
    } finally {
        synchronized(active) {
            mutex.dec()
            if (mutex.isFree) {
                active.remove(key)
                pool.recycle(mutex)
            }
        }
    }
}
