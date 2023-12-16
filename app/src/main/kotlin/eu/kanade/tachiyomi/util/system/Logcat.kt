package eu.kanade.tachiyomi.util.system

import android.util.Log

inline fun logcat(tag: String, priority: Int = Log.DEBUG, message: () -> String) {
    Log.println(priority, tag, message())
}

inline fun Any.logcat(priority: Int = Log.DEBUG, message: () -> String) {
    Log.println(priority, javaClass.simpleName, message())
}
