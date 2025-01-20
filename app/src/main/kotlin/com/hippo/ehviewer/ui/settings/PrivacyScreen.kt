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
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.clear_all
import com.ehviewer.core.common.clear_search_history
import com.ehviewer.core.common.clear_search_history_confirm
import com.ehviewer.core.common.clear_search_history_summary
import com.ehviewer.core.common.search_history_cleared
import com.ehviewer.core.common.settings_privacy
import com.ehviewer.core.common.settings_privacy_require_unlock
import com.ehviewer.core.common.settings_privacy_require_unlock_delay
import com.ehviewer.core.common.settings_privacy_require_unlock_delay_summary
import com.ehviewer.core.common.settings_privacy_require_unlock_delay_summary_immediately
import com.ehviewer.core.common.settings_privacy_secure
import com.ehviewer.core.common.settings_privacy_secure_summary
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
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
import org.jetbrains.compose.resources.stringResource

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.PrivacyScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    fun launchSnackBar(content: String) = launch { snackbarHostState.showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.settings_privacy)) },
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
                title = stringResource(Res.string.settings_privacy_require_unlock),
                value = security.rememberedAccessor,
                enabled = isAuthenticationSupported(),
            )
            AnimatedVisibility(visible = security.value) {
                val securityDelay = Settings::securityDelay.observed
                val summary = if (securityDelay.value == 0) {
                    stringResource(Res.string.settings_privacy_require_unlock_delay_summary_immediately)
                } else {
                    stringResource(Res.string.settings_privacy_require_unlock_delay_summary, securityDelay.value)
                }
                IntSliderPreference(
                    maxValue = 30,
                    title = stringResource(Res.string.settings_privacy_require_unlock_delay),
                    summary = summary,
                    value = securityDelay.rememberedAccessor,
                    enabled = LocalContext.current.isAuthenticationSupported(),
                )
            }
            SwitchPreference(
                title = stringResource(Res.string.settings_privacy_secure),
                summary = stringResource(Res.string.settings_privacy_secure_summary),
                value = Settings::enabledSecurity,
            )
            val searchHistoryCleared = stringResource(Res.string.search_history_cleared)
            Preference(
                title = stringResource(Res.string.clear_search_history),
                summary = stringResource(Res.string.clear_search_history_summary),
            ) {
                launch {
                    awaitConfirmationOrCancel(
                        confirmText = Res.string.clear_all,
                        title = Res.string.clear_search_history_confirm,
                    )
                    searchDatabase.searchDao().clear()
                    launchSnackBar(searchHistoryCleared)
                }
            }
        }
    }
}
