package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ehviewer.core.i18n.R
import com.ehviewer.core.util.launch
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import moe.tarsin.snackbar

private const val APPLY_JS = "javascript:(function(){var apply = document.getElementById(\"apply\").children[0];apply.click();})();"

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.UConfigScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val url = EhUrl.getUConfigUrl()
    val wvNavigator = rememberWebViewNavigator()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.u_config)) },
                navigationIcon = { NavigationIcon() },
                actions = {
                    IconButton(
                        onClick = {
                            wvNavigator.loadUrl(APPLY_JS)
                            navigator.popBackStack()
                        },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        val state = rememberWebViewState(url = url)
        WebView(
            state = state,
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            navigator = wvNavigator,
            onCreated = { it.setDefaultSettings() },
        )
        val applyTip = stringResource(id = R.string.apply_tip)
        DisposableEffect(Unit) {
            launch { snackbar(applyTip) }
            onDispose {
                EhCookieStore.flush()
            }
        }
    }
}
