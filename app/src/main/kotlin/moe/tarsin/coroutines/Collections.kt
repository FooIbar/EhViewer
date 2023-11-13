package moe.tarsin.coroutines

fun <T> List<T>.replace(toRemove: T, toAdd: T) = map {
    if (it === toRemove) {
        toAdd
    } else {
        it
    }
}
