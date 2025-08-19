package com.ehviewer.core.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

inline fun <T> Flow<T>.onEachLatest(
    crossinline action: suspend (T) -> Unit,
): Flow<T> = transformLatest { value ->
    action(value)
    return@transformLatest emit(value)
}
