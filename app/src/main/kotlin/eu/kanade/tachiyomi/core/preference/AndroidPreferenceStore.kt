package eu.kanade.tachiyomi.core.preference

import android.content.SharedPreferences
import com.hippo.ehviewer.Settings
import eu.kanade.tachiyomi.core.preference.AndroidPreference.BooleanPrimitive
import eu.kanade.tachiyomi.core.preference.AndroidPreference.FloatPrimitive
import eu.kanade.tachiyomi.core.preference.AndroidPreference.IntPrimitive
import eu.kanade.tachiyomi.core.preference.AndroidPreference.LongPrimitive
import eu.kanade.tachiyomi.core.preference.AndroidPreference.Object
import eu.kanade.tachiyomi.core.preference.AndroidPreference.StringPrimitive
import eu.kanade.tachiyomi.core.preference.AndroidPreference.StringSetPrimitive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

object AndroidPreferenceStore : PreferenceStore {

    private val sharedPreferences = Settings.prefs

    private val keyFlow = sharedPreferences.keyFlow

    override fun getString(key: String, defaultValue: String): Preference<String> = StringPrimitive(sharedPreferences, keyFlow, key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> = LongPrimitive(sharedPreferences, keyFlow, key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> = IntPrimitive(sharedPreferences, keyFlow, key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> = FloatPrimitive(sharedPreferences, keyFlow, key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> = BooleanPrimitive(sharedPreferences, keyFlow, key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> = StringSetPrimitive(sharedPreferences, keyFlow, key, defaultValue)

    override fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = Object(
        preferences = sharedPreferences,
        keyFlow = keyFlow,
        key = key,
        defaultValue = defaultValue,
        serializer = serializer,
        deserializer = deserializer,
    )
}

private val SharedPreferences.keyFlow
    get() = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key: String? -> trySend(key) }
        registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
