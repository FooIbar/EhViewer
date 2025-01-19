package com.hippo.ehviewer.ui.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.LicenseScreenDestination
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.updater.Release
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.displayString
import com.hippo.ehviewer.util.installPackage
import com.hippo.files.delete
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withUIContext
import moe.tarsin.coroutines.runSuspendCatching

private const val REPO_URL = "https://github.com/${BuildConfig.REPO_NAME}"
private const val RELEASE_URL = "$REPO_URL/releases"

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.AboutScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = settingsAbout) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState()).padding(paddingValues)) {
            Preference(
                title = settingsAboutDeclaration,
                summary = settingsAboutDeclarationSummary,
            )
            HtmlPreference(
                title = settingsAboutAuthor,
                summary = settingsAboutAuthorSummary,
            )
            UrlPreference(
                title = settingsAboutLatestRelease,
                url = RELEASE_URL,
            )
            UrlPreference(
                title = settingsAboutSource,
                url = REPO_URL,
            )
            Preference(title = license) {
                navigator.navigate(LicenseScreenDestination)
            }
            Preference(
                title = settingsAboutVersion,
                summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_SHA})\n" + settingsAboutCommitTime(AppConfig.commitTime),
            )
            SwitchPreference(
                title = backupBeforeUpdate,
                value = Settings::backupBeforeUpdate,
            )
            SwitchPreference(
                title = useCiUpdateChannel,
                value = Settings::useCIUpdateChannel,
            )
            SimpleMenuPreferenceInt(
                title = autoUpdates,
                entry = R.array.update_frequency,
                entryValueRes = R.array.update_frequency_values,
                value = Settings::updateIntervalDays.observed,
            )
            WorkPreference(title = settingsAboutCheckForUpdates) {
                runSuspendCatching {
                    AppUpdater.checkForUpdate(true)?.let {
                        showNewVersion(context, it)
                    } ?: launchSnackbar(alreadyLatestVersion)
                }.onFailure {
                    launchSnackbar(updateFailed(it.displayString()))
                }
            }
        }
    }
}

suspend fun DialogState.showNewVersion(context: Context, release: Release) {
    awaitConfirmationOrCancel(
        confirmText = R.string.download,
        title = R.string.new_version_available,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = release.version,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = release.changelog)
        }
    }
    if (Settings.backupBeforeUpdate) {
        val time = ReadableTime.getFilenamableTime()
        EhDB.exportDB(context, (downloadLocation / "$time.db"))
    }
    // TODO: Download in the background and show progress in notification
    val path = AppConfig.tempDir / "update.apk"
    AppUpdater.downloadUpdate(release.downloadLink, path.apply { delete() })
    withUIContext { context.installPackage(path.toFile()) }
}
