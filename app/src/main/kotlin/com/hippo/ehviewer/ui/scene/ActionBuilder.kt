package com.hippo.ehviewer.ui.scene

fun interface ActionScope {
    infix fun String.thenDo(that: suspend () -> Unit)
}

inline fun buildAction(builder: ActionScope.() -> Unit) = buildList {
    builder { that -> add(this to that) }
}
