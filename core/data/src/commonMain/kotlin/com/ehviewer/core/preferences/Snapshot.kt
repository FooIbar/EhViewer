package com.ehviewer.core.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

open class Snapshot internal constructor(private val delegate: Preferences) {
    operator fun <T> contains(pref: PrefDelegate<T>): Boolean = pref.key in delegate
    operator fun <T> get(pref: PrefDelegate<T>): T = delegate[pref.key] ?: pref.defaultValue
}

class MutableSnapshot internal constructor(private val delegate: MutablePreferences) : Snapshot(delegate) {
    operator fun <T> set(pref: PrefDelegate<T>, value: T) {
        if (value == null) {
            delegate.remove(pref.key)
        } else {
            delegate[pref.key] = value
        }
    }
}
