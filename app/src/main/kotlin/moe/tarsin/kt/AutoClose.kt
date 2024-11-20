package moe.tarsin.kt

import arrow.AutoCloseScope

inline fun <A> AutoCloseScope.install(
    acquire: () -> A,
    crossinline release: (A, Throwable?) -> Unit,
): A = acquire().also { a -> onClose { release(a, it) } }
