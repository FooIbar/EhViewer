package moe.tarsin.coroutines

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.sync.Mutex

suspend inline fun <T> Mutex.withLockNeedSuspend(action: () -> T): Pair<T, Boolean> {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val mustSuspend = !tryLock()
    if (mustSuspend) lock()
    return try {
        action() to mustSuspend
    } finally {
        unlock()
    }
}
