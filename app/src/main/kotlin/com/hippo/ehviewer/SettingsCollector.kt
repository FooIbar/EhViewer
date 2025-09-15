package com.hippo.ehviewer

import android.app.UiModeManager
import androidx.appcompat.app.AppCompatDelegate
import arrow.core.Either.Companion.catch
import com.ehviewer.core.preferences.PrefDelegate
import com.ehviewer.core.util.withIOContext
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.dailycheck.updateDailyCheckWork
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import com.hippo.ehviewer.util.isAtLeastS
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import splitties.init.appCtx
import splitties.systemservices.uiModeManager

private const val TAG = "SettingsCollector"

private val collectScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
fun <T, R : PrefDelegate<T>> R.observed(func: suspend (T) -> Unit) = apply { collectScope.launch { valueFlow().drop(1).collectLatest(func) } }
fun <T, R : Settings.Delegate<T>> R.emitTo(flow: MutableSharedFlow<Unit>) = apply { collectScope.launch { flow.emitAll(changesFlow()) } }
fun <T, R : PrefDelegate<T>> R.emitTo(flow: MutableSharedFlow<Unit>) = apply { collectScope.launch { flow.emitAll(changesFlow()) } }

suspend fun updateWhenKeepMediaStatusChanges(mediaScan: Boolean) {
    withIOContext {
        catch {
            keepNoMediaFileStatus(mediaScan = mediaScan)
        }.onLeft {
            logcat(TAG, it)
        }
    }
}

suspend fun updateWhenThemeChanges(theme: Int) {
    delay(100) // Avoid recompose being cancelled
    if (isAtLeastS) {
        val mode = when (theme) {
            AppCompatDelegate.MODE_NIGHT_NO -> UiModeManager.MODE_NIGHT_NO
            AppCompatDelegate.MODE_NIGHT_YES -> UiModeManager.MODE_NIGHT_YES
            else -> UiModeManager.MODE_NIGHT_AUTO
        }
        uiModeManager.setApplicationNightMode(mode)
    }
    AppCompatDelegate.setDefaultNightMode(theme)
}

fun updateWhenRequestNewsChanges() {
    updateDailyCheckWork(appCtx)
}

suspend fun updateWhenGallerySiteChanges(gallerySite: Int) {
    if (Settings.hasSignedIn.value) {
        catch {
            EhEngine.getUConfig(gallerySite)
        }.onLeft {
            logcat(TAG, it)
        }
    }
}

fun updateWhenTagTranslationChanges(enabled: Boolean) {
    if (enabled) {
        EhTagDatabase.launchUpdate()
    }
}
