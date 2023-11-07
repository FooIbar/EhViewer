package com.hippo.ehviewer.ui.scene

interface ActionScope {
    infix fun String.thenDo(that: suspend () -> Unit)
}

inline fun buildAction(builder: ActionScope.() -> Unit) = buildList {
    builder(object : ActionScope {
        override fun String.thenDo(that: suspend () -> Unit) {
            add(this to that)
        }
    })
}
