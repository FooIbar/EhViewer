package moe.tarsin.kt

inline infix fun (() -> Unit).andThen(crossinline block: () -> Unit): () -> Unit = {
    invoke()
    block()
}
