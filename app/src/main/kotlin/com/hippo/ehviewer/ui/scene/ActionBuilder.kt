package com.hippo.ehviewer.ui.scene

fun interface ActionScope {
    fun onSelect(action: String, that: suspend () -> Unit)
}

inline fun buildAction(builder: ActionScope.() -> Unit) = buildList {
    builder { action, that -> add(action to that) }
}
