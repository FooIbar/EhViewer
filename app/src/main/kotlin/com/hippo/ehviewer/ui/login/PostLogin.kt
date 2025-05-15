package com.hippo.ehviewer.ui.login

import arrow.core.Either.Companion.catch
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

suspend fun CoroutineScope.refreshAccountInfo() = catch {
    with(EhEngine.getProfile()) {
        Settings.displayName.value = displayName
        Settings.avatar.value = avatar
    }
}.onLeft {
    logcat(it)
}

@OptIn(DelicateCoroutinesApi::class)
fun postLogin() = GlobalScope.async(Dispatchers.IO) {
    launch { refreshAccountInfo() }

    // For the `star` cookie
    catch {
        EhEngine.getNews(false)
    }.onLeft {
        logcat(it)
    }

    // Get cookies for image limits
    launch {
        catch {
            EhEngine.getUConfig(EhUrl.URL_UCONFIG_E)
            EhCookieStore.flush()
        }.onLeft {
            logcat(it)
        }
    }

    // Sad panda check
    catch {
        EhEngine.getUConfig(EhUrl.URL_UCONFIG_EX)
        EhCookieStore.flush()
        Settings.gallerySite.value = EhUrl.SITE_EX
    }.onLeft {
        Settings.gallerySite.value = EhUrl.SITE_E
    }

    Settings.hasSignedIn.value = true
    Settings.needSignIn.value = false
}
