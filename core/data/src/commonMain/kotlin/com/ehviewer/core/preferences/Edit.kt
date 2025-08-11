package com.ehviewer.core.preferences

import com.ehviewer.core.mainthread.checkNotMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun <P : DataStorePreferences> P.edit(blocking: Boolean = false, block: P.(MutableSnapshot) -> Unit) {
    if (blocking) {
        checkNotMainThread()
        runBlocking {
            withMutableSnapshot { block(it) }
        }
    } else {
        scope.launch {
            withMutableSnapshot { block(it) }
        }
    }
}

private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
