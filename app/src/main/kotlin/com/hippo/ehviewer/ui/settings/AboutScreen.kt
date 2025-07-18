package com.hippo.ehviewer.ui.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.LicenseScreenDestination
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
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
import moe.tarsin.launch
import moe.tarsin.navigate
import moe.tarsin.snackbar
import moe.tarsin.string

private const val REPO_URL = "https://github.com/${BuildConfig.REPO_NAME}"
private const val RELEASE_URL = "$REPO_URL/releases"

@Composable
@Stable
private fun versionCode() = "${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_SHA})\n" + stringResource(R.string.settings_about_commit_time, AppConfig.commitTime)

@Composable
@Stable
private fun author() = AnnotatedString.fromHtml(stringResource(R.string.settings_about_author_summary).replace('$', '@'))

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.AboutScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    fun launchSnackbar(message: String) = launch { snackbar(message) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_about)) },
                navigationIcon = { NavigationIcon() },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState()).padding(paddingValues)) {
            Preference(
                title = stringResource(id = R.string.settings_about_declaration),
                summary = stringResource(id = R.string.settings_about_declaration_summary),
            )
            HtmlPreference(
                title = stringResource(id = R.string.settings_about_author),
                summary = author(),
            )
            UrlPreference(
                title = stringResource(id = R.string.settings_about_latest_release),
                url = RELEASE_URL,
            )
            UrlPreference(
                title = stringResource(id = R.string.settings_about_source),
                url = REPO_URL,
            )
            Preference(title = stringResource(id = R.string.license)) {
                navigate(LicenseScreenDestination)
            }
            Preference(
                title = stringResource(id = R.string.settings_about_version),
                summary = versionCode(),
            )
            SwitchPreference(
                title = stringResource(id = R.string.backup_before_update),
                state = Settings.backupBeforeUpdate.asMutableState(),
            )
            SwitchPreference(
                title = stringResource(id = R.string.use_ci_update_channel),
                state = Settings.useCIUpdateChannel.asMutableState(),
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.auto_updates),
                entry = R.array.update_frequency,
                entryValueRes = R.array.update_frequency_values,
                state = Settings.updateIntervalDays.asMutableState(),
            )
            WorkPreference(title = stringResource(id = R.string.settings_about_check_for_updates)) {
                runSuspendCatching {
                    AppUpdater.checkForUpdate(true)?.let { showNewVersion(it) } ?: launchSnackbar(string(R.string.already_latest_version))
                }.onFailure {
                    launchSnackbar(string(R.string.update_failed, it.displayString()))
                }
            }
        }
    }
}

context(ctx: Context, _: DialogState)
suspend fun showNewVersion(release: Release) {
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
    if (Settings.backupBeforeUpdate.value) {
        val time = ReadableTime.getFilenamableTime()
        EhDB.exportDB(ctx, (downloadLocation / "$time.db"))
    }
    // TODO: Download in the background and show progress in notification
    val path = AppConfig.tempDir / "update.apk"
    AppUpdater.downloadUpdate(release.downloadLink, path.apply { delete() })
    withUIContext { installPackage(path.toFile()) }
}
