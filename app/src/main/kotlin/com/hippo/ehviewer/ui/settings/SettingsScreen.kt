package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.settings
import com.ehviewer.core.common.settings_about
import com.ehviewer.core.common.settings_advanced
import com.ehviewer.core.common.settings_download
import com.ehviewer.core.common.settings_eh
import com.ehviewer.core.common.settings_privacy
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.SadPanda
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.AboutScreenDestination
import com.hippo.ehviewer.ui.destinations.AdvancedScreenDestination
import com.hippo.ehviewer.ui.destinations.DownloadScreenDestination
import com.hippo.ehviewer.ui.destinations.EhScreenDestination
import com.hippo.ehviewer.ui.destinations.PrivacyScreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.jetbrains.compose.resources.stringResource

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.SettingsScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) {
        Column(modifier = Modifier.padding(it).nestedScroll(scrollBehavior.nestedScrollConnection)) {
            PreferenceHeader(
                icon = EhIcons.Default.SadPanda,
                title = Res.string.settings_eh,
                childRoute = EhScreenDestination,
                navigator = navigator,
            )
            PreferenceHeader(
                icon = Icons.Default.Download,
                title = Res.string.settings_download,
                childRoute = DownloadScreenDestination,
                navigator = navigator,
            )
            PreferenceHeader(
                icon = Icons.Default.Security,
                title = Res.string.settings_privacy,
                childRoute = PrivacyScreenDestination,
                navigator = navigator,
            )
            PreferenceHeader(
                icon = Icons.Default.Adb,
                title = Res.string.settings_advanced,
                childRoute = AdvancedScreenDestination,
                navigator = navigator,
            )
            PreferenceHeader(
                icon = Icons.Default.Info,
                title = Res.string.settings_about,
                childRoute = AboutScreenDestination,
                navigator = navigator,
            )
        }
    }
}
