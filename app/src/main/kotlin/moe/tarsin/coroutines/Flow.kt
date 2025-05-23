package moe.tarsin.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest

inline fun <T> Flow<T>.onEachLatest(
    crossinline action: suspend (T) -> Unit,
): Flow<T> = transformLatest { value ->
    action(value)
    return@transformLatest emit(value)
}

inline fun <T, R> Flow<T>.flatMapLatestScoped(crossinline transform: suspend CoroutineScope.(value: T) -> Flow<R>) = transformLatest {
    coroutineScope { emitAll(transform(it)) }
}
