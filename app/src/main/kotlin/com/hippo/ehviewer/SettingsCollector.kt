package com.hippo.ehviewer

import android.app.UiModeManager
import androidx.appcompat.app.AppCompatDelegate
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.dailycheck.updateDailyCheckWork
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import com.hippo.ehviewer.util.isAtLeastS
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import splitties.init.appCtx
import splitties.preferences.PrefDelegate
import splitties.systemservices.uiModeManager

private val collectScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
fun <T, R : PrefDelegate<T>> R.observed(func: (Unit) -> Unit) = apply { collectScope.launch { changesFlow().collect(func) } }
fun <T, R : Settings.Delegate<T>> R.observed(func: (Unit) -> Unit) = apply { collectScope.launch { flowGetter().collect(func) } }
fun <T, R : Settings.Delegate<T>> R.emitTo(flow: MutableSharedFlow<Unit>) = apply { collectScope.launch { flowGetter().collect { flow.emit(Unit) } } }
fun <T, R : PrefDelegate<T>> R.emitTo(flow: MutableSharedFlow<Unit>) = apply { collectScope.launch { changesFlow().collect { flow.emit(Unit) } } }

fun updateWhenKeepMediaStatusChanges() {
    collectScope.launchIO {
        runCatching {
            keepNoMediaFileStatus()
        }
    }
}

fun updateWhenThemeChanges() {
    collectScope.launch {
        delay(100) // Avoid recompose being cancelled
        if (isAtLeastS) {
            val mode = when (Settings.theme) {
                AppCompatDelegate.MODE_NIGHT_NO -> UiModeManager.MODE_NIGHT_NO
                AppCompatDelegate.MODE_NIGHT_YES -> UiModeManager.MODE_NIGHT_YES
                else -> UiModeManager.MODE_NIGHT_AUTO
            }
            uiModeManager.setApplicationNightMode(mode)
        }
        AppCompatDelegate.setDefaultNightMode(Settings.theme)
    }
}

fun updateWhenRequestNewsChanges() {
    updateDailyCheckWork(appCtx)
}

fun updateWhenGallerySiteChanges() {
    if (Settings.hasSignedIn.value) {
        collectScope.launchIO {
            runCatching {
                EhEngine.getUConfig()
            }.onFailure {
                logcat(it)
            }
        }
    }
}

fun updateWhenTagTranslationChanges() {
    collectScope.launchIO {
        runCatching {
            EhTagDatabase.update()
        }.onFailure {
            logcat(it)
        }
    }
}
