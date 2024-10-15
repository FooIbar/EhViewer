package moe.tarsin.kt

inline fun <T : Any, R> T.sync(block: T.() -> R) = synchronized(this) { block(this) }
