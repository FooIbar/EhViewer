package com.hippo.ehviewer.ui.settings

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
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.SadPanda
import com.hippo.ehviewer.ui.destinations.AboutScreenDestination
import com.hippo.ehviewer.ui.destinations.AdvancedScreenDestination
import com.hippo.ehviewer.ui.destinations.DownloadScreenDestination
import com.hippo.ehviewer.ui.destinations.EhScreenDestination
import com.hippo.ehviewer.ui.destinations.FinishDestination
import com.hippo.ehviewer.ui.destinations.PrivacyScreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@RootNavGraph(start = true)
@Destination
@Composable
fun BaseScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigate(FinishDestination) }) {
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
                title = R.string.settings_eh,
                childRoute = EhScreenDestination,
                navigator = navigator,
            )
            PreferenceHeader(
                icon = Icons.Default.Download,
                title = R.string.settings_download,
                childRoute = DownloadScreenDestination,
                navigator = navigator,
            )
            PreferenceHeader(
                icon = Icons.Default.Security,
                title = R.string.settings_privacy,
                childRoute = PrivacyScreenDestination,
                navigator = navigator,
            )
            PreferenceHeader(
                icon = Icons.Default.Adb,
                title = R.string.settings_advanced,
                childRoute = AdvancedScreenDestination,
                navigator = navigator,
            )
            PreferenceHeader(
                icon = Icons.Default.Info,
                title = R.string.settings_about,
                childRoute = AboutScreenDestination,
                navigator = navigator,
            )
        }
    }
}
