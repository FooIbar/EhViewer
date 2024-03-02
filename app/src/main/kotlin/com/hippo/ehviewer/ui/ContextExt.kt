package com.hippo.ehviewer.ui

import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

// Fuck U, Kotlin compiler!!!
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <T1, T2, R> with(
    t1: T1,
    t2: T2,
    block: context(T1, T2)
    () -> R,
): R {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }
    return block(t1, t2)
}
