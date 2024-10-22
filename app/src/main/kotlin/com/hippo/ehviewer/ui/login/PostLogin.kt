package com.hippo.ehviewer.ui.login

import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

suspend fun CoroutineScope.refreshAccountInfo() = runSuspendCatching {
    with(EhEngine.getProfile()) {
        Settings.displayName.value = displayName
        Settings.avatar.value = avatar
    }
}.onFailure {
    logcat(it)
}

@OptIn(DelicateCoroutinesApi::class)
fun postLogin() = GlobalScope.async(Dispatchers.IO) {
    launch { refreshAccountInfo() }
    runCatching {
        // For the `star` cookie
        EhEngine.getNews(false)

        // Get cookies for image limits
        launch {
            runCatching {
                EhEngine.getUConfig(EhUrl.URL_UCONFIG_E)
                EhCookieStore.flush()
            }.onFailure {
                logcat(it)
            }
        }

        // Sad panda check
        EhEngine.getUConfig(EhUrl.URL_UCONFIG_EX)
        EhCookieStore.flush()
        Settings.gallerySite.value = EhUrl.SITE_EX
    }.onFailure {
        Settings.gallerySite.value = EhUrl.SITE_E
    }

    Settings.hasSignedIn.value = true
    Settings.needSignIn.value = false
}
