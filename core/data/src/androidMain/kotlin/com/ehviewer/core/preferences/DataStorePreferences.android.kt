package com.ehviewer.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import splitties.init.appCtx

internal actual fun getDateStore(name: String?): DataStore<Preferences> {
    val context = appCtx
    val actualName = name ?: "${context.packageName}_preferences"
    return PreferenceDataStoreFactory.create(
        migrations = listOf(SharedPreferencesMigration(context = context, sharedPreferencesName = actualName)),
    ) {
        context.preferencesDataStoreFile(actualName)
    }
}
