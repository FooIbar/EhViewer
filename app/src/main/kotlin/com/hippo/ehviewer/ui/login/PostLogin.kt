package com.hippo.ehviewer.ui.login

import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun postLogin() = coroutineScope {
    launch {
        runCatching {
            EhEngine.getProfile().run {
                Settings.displayName.value = displayName
            }
        }.onFailure {
            logcat(it)
        }
    }
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
        Settings.gallerySite = EhUrl.SITE_EX
        // Explicitly use ex url since https://github.com/Ehviewer-Overhauled/Ehviewer/issues/1239#issuecomment-1632584525
        EhEngine.getUConfig(EhUrl.URL_UCONFIG_EX)
        EhCookieStore.flush()
    }.onFailure {
        Settings.gallerySite = EhUrl.SITE_E
    }
    Settings.hasSignedIn.value = true
    Settings.needSignIn = false
}
