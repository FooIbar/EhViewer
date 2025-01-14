package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.isAuthenticationSupported
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberedAccessor
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.PrivacyScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    fun launchSnackBar(content: String) = launch { snackbarHostState.showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_privacy)) },
                navigationIcon = {
                    IconButton(onClick = { popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        Column(modifier = Modifier.padding(it).nestedScroll(scrollBehavior.nestedScrollConnection)) {
            val security = Settings.security.asMutableState()
            SwitchPreference(
                title = stringResource(id = R.string.settings_privacy_require_unlock),
                value = security.rememberedAccessor,
                enabled = isAuthenticationSupported(),
            )
            AnimatedVisibility(visible = security.value) {
                val securityDelay = Settings::securityDelay.observed
                val summary = if (securityDelay.value == 0) {
                    stringResource(id = R.string.settings_privacy_require_unlock_delay_summary_immediately)
                } else {
                    stringResource(id = R.string.settings_privacy_require_unlock_delay_summary, securityDelay.value)
                }
                IntSliderPreference(
                    maxValue = 30,
                    title = stringResource(id = R.string.settings_privacy_require_unlock_delay),
                    summary = summary,
                    value = securityDelay.rememberedAccessor,
                    enabled = LocalContext.current.isAuthenticationSupported(),
                )
            }
            SwitchPreference(
                title = stringResource(id = R.string.settings_privacy_secure),
                summary = stringResource(id = R.string.settings_privacy_secure_summary),
                value = Settings::enabledSecurity,
            )
            val searchHistoryCleared = stringResource(id = R.string.search_history_cleared)
            Preference(
                title = stringResource(id = R.string.clear_search_history),
                summary = stringResource(id = R.string.clear_search_history_summary),
            ) {
                launch {
                    awaitConfirmationOrCancel(
                        confirmText = R.string.clear_all,
                        title = R.string.clear_search_history_confirm,
                    )
                    searchDatabase.searchDao().clear()
                    launchSnackBar(searchHistoryCleared)
                }
            }
        }
    }
}
