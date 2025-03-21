package com.hippo.ehviewer.util

import kotlin.concurrent.atomics.AtomicInt
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun AtomicInt.loop(action: (Int) -> Unit): Nothing {
    contract { callsInPlace(action, InvocationKind.AT_LEAST_ONCE) }
    do {
        action(load())
    } while (true)
}

inline fun <R> AtomicInt.update(function: (Int) -> Int, transform: (old: Int, new: Int) -> R): R {
    contract {
        callsInPlace(function, InvocationKind.AT_LEAST_ONCE)
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    loop { cur ->
        val upd = function(cur)
        if (compareAndSet(cur, upd)) return transform(cur, upd)
    }
}

inline fun AtomicInt.updateAndGet(function: (Int) -> Int): Int {
    contract { callsInPlace(function, InvocationKind.AT_LEAST_ONCE) }
    return update(function) { _, new -> new }
}
