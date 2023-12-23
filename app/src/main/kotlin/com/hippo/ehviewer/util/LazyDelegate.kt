@file:Suppress("NOTHING_TO_INLINE")

package com.hippo.ehviewer.util

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)
fun <T> resettableLazy(initializer: () -> T) = ResettableLazy(initializer)

interface Lazy<T> {
    var value: T
}

class ResettableLazy<T>(private val initializer: () -> T) {

    private var value: T? = null

    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        return value ?: initializer().apply { value = this }
    }

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        check(this.value == value) { "New values aren't accepted to reset this delegated property" }
        invalidate()
    }

    private fun invalidate() {
        value = null
    }
}

fun <T> lazyMut(init: () -> KMutableProperty0<T>) = object : Lazy<T> {
    private val lazy by lazy { init() }
    override var value
        get() = lazy.get()
        set(value) = lazy.set(value)
}

inline operator fun <T> Lazy<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value
inline operator fun <T> Lazy<T>.setValue(thisRef: Any?, prop: KProperty<*>?, newValue: T) {
    value = newValue
}
