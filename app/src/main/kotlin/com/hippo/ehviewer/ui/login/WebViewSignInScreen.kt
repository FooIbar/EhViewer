package com.hippo.ehviewer.ui.login

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.awaitCancellation

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.WebViewSignInScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val state = rememberWebViewState(url = EhUrl.URL_SIGN_IN)
    LaunchedEffect(state) {
        snapshotFlow { !state.isLoading }.collect { hasFinished ->
            if (hasFinished) {
                if (EhCookieStore.isCloudflareBypassed()) {
                    Settings.desktopSite.value = false
                }
                if (EhCookieStore.hasSignedIn()) {
                    postLogin()
                    state.webView?.destroy()
                    bgWork { awaitCancellation() }
                }
            }
        }
    }
    WebView(
        state = state,
        modifier = Modifier.fillMaxSize(),
        onCreated = {
            EhUtils.signOut()
            it.setDefaultSettings()
        },
    )
}
