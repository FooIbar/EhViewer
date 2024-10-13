package com.hippo.ehviewer.ui.login

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.composing
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.WebViewSignInScreen(navigator: DestinationsNavigator) = composing(navigator) {
    val state = rememberWebViewState(url = EhUrl.URL_SIGN_IN)
    val client = remember {
        object : AccompanistWebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                if (EhCookieStore.hasSignedIn()) {
                    postLogin()
                    view.destroy()
                    launch { bgWork { awaitCancellation() } }
                }
            }
        }
    }
    SideEffect {
        EhUtils.signOut()
    }
    WebView(
        state = state,
        modifier = Modifier.fillMaxSize(),
        onCreated = { it.setDefaultSettings() },
        client = client,
    )
}
