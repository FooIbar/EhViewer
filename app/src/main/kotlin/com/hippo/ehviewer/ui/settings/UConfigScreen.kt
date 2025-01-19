package com.hippo.ehviewer.ui.settings

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import arrow.atomic.Atomic
import arrow.atomic.value
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.apply_tip
import com.ehviewer.core.common.u_config
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.jetbrains.compose.resources.stringResource

private const val APPLY_JS = "javascript:(function(){var apply = document.getElementById(\"apply\").children[0];apply.click();})();"

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.UConfigScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val url = EhUrl.uConfigUrl
    var webview by remember { Atomic<WebView?>(null)::value }
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.u_config)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            webview?.loadUrl(APPLY_JS)
                            navigator.popBackStack()
                        },
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val state = rememberWebViewState(url = url)
        WebView(
            state = state,
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            onCreated = { it.setDefaultSettings() },
            factory = { WebView(it).apply { webview = this } },
        )
        val applyTip = stringResource(Res.string.apply_tip)
        LaunchedEffect(Unit) { snackbarHostState.showSnackbar(applyTip) }
        DisposableEffect(Unit) {
            onDispose {
                EhCookieStore.flush()
            }
        }
    }
}
