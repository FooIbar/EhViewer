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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = settingsPrivacy) },
                navigationIcon = {
                    IconButton(onClick = { popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) {
        Column(modifier = Modifier.padding(it).nestedScroll(scrollBehavior.nestedScrollConnection)) {
            val security = Settings.security.asMutableState()
            SwitchPreference(
                title = settingsPrivacyRequireUnlock,
                value = security.rememberedAccessor,
                enabled = isAuthenticationSupported(),
            )
            AnimatedVisibility(visible = security.value) {
                val securityDelay = Settings::securityDelay.observed
                val summary = if (securityDelay.value == 0) {
                    settingsPrivacyRequireUnlockDelaySummaryImmediately
                } else {
                    settingsPrivacyRequireUnlockDelaySummary(securityDelay.value)
                }
                IntSliderPreference(
                    maxValue = 30,
                    title = settingsPrivacyRequireUnlockDelay,
                    summary = summary,
                    value = securityDelay.rememberedAccessor,
                    enabled = LocalContext.current.isAuthenticationSupported(),
                )
            }
            SwitchPreference(
                title = settingsPrivacySecure,
                summary = settingsPrivacySecureSummary,
                value = Settings::enabledSecurity,
            )
            Preference(
                title = clearSearchHistory,
                summary = clearSearchHistorySummary,
            ) {
                launch {
                    awaitConfirmationOrCancel(
                        confirmText = R.string.clear_all,
                        title = R.string.clear_search_history_confirm,
                    )
                    searchDatabase.searchDao().clear()
                    launchSnackbar(searchHistoryCleared)
                }
            }
        }
    }
}
