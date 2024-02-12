package eu.kanade.tachiyomi.util.system

import logcat.LogPriority
import logcat.asLog
import logcat.logcat as logcatImpl

inline fun logcat(tag: String, priority: LogPriority = LogPriority.DEBUG, message: () -> String) {
    logcatImpl(tag, priority, message)
}

inline fun Any.logcat(priority: LogPriority = LogPriority.DEBUG, message: () -> String) {
    logcatImpl(priority, message = message)
}

fun logcat(tag: String, throwable: Throwable) {
    logcatImpl(tag, LogPriority.ERROR) { throwable.asLog() }
}

fun Any.logcat(throwable: Throwable) {
    logcatImpl(LogPriority.ERROR) { throwable.asLog() }
}
