package com.ehviewer.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal expect fun getDateStore(name: String?): DataStore<Preferences>

abstract class DataStorePreferences(private val dataStore: DataStore<Preferences>) {
    constructor(name: String?) : this(getDateStore(name))

    val data: Flow<Snapshot>
        get() = dataStore.data.map { Snapshot(it) }

    suspend fun snapshot(): Snapshot = data.first()

    suspend fun withMutableSnapshot(block: (MutableSnapshot) -> Unit) {
        dataStore.edit {
            block(MutableSnapshot(it))
        }
    }

    protected fun boolPref(
        key: String,
        defaultValue: Boolean,
    ) = PrefDelegate(this, booleanPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun intPref(
        key: String,
        defaultValue: Int,
    ) = PrefDelegate(this, intPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun floatPref(
        key: String,
        defaultValue: Float,
    ) = PrefDelegate(this, floatPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun doublePref(
        key: String,
        defaultValue: Double,
    ) = PrefDelegate(this, doublePreferencesKey(name = key), defaultValue = defaultValue)

    protected fun longPref(
        key: String,
        defaultValue: Long,
    ) = PrefDelegate(this, longPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun stringPref(
        key: String,
        defaultValue: String,
    ) = PrefDelegate(this, stringPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun stringOrNullPref(
        key: String,
    ) = PrefDelegate(this, stringPreferencesKey(name = key), null)

    protected fun stringSetPref(
        key: String,
        defaultValue: Set<String> = emptySet(),
    ) = PrefDelegate(this, stringSetPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun stringSetOrNullPref(
        key: String,
    ) = PrefDelegate(this, stringSetPreferencesKey(name = key), null)

    protected fun byteArrayPref(
        key: String,
        defaultValue: ByteArray,
    ) = PrefDelegate(this, byteArrayPreferencesKey(name = key), defaultValue = defaultValue)
}
