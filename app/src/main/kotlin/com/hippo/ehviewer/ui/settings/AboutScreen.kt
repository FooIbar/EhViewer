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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.already_latest_version
import com.ehviewer.core.common.auto_updates
import com.ehviewer.core.common.backup_before_update
import com.ehviewer.core.common.download
import com.ehviewer.core.common.license
import com.ehviewer.core.common.new_version_available
import com.ehviewer.core.common.settings_about
import com.ehviewer.core.common.settings_about_author
import com.ehviewer.core.common.settings_about_author_summary
import com.ehviewer.core.common.settings_about_check_for_updates
import com.ehviewer.core.common.settings_about_commit_time
import com.ehviewer.core.common.settings_about_declaration
import com.ehviewer.core.common.settings_about_declaration_summary
import com.ehviewer.core.common.settings_about_latest_release
import com.ehviewer.core.common.settings_about_source
import com.ehviewer.core.common.settings_about_version
import com.ehviewer.core.common.update_failed
import com.ehviewer.core.common.use_ci_update_channel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private const val REPO_URL = "https://github.com/${BuildConfig.REPO_NAME}"
private const val RELEASE_URL = "$REPO_URL/releases"

@Composable
@Stable
private fun versionCode() = "${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_SHA})\n" + stringResource(Res.string.settings_about_commit_time, AppConfig.commitTime)

@Composable
@Stable
private fun author() = AnnotatedString.fromHtml(stringResource(Res.string.settings_about_author_summary).replace('$', '@'))

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.AboutScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    fun launchSnackBar(content: String) = coroutineScope.launch { snackbarHostState.showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.settings_about)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState()).padding(paddingValues)) {
            Preference(
                title = stringResource(Res.string.settings_about_declaration),
                summary = stringResource(Res.string.settings_about_declaration_summary),
            )
            HtmlPreference(
                title = stringResource(Res.string.settings_about_author),
                summary = author(),
            )
            UrlPreference(
                title = stringResource(Res.string.settings_about_latest_release),
                url = RELEASE_URL,
            )
            UrlPreference(
                title = stringResource(Res.string.settings_about_source),
                url = REPO_URL,
            )
            Preference(title = stringResource(Res.string.license)) {
                navigator.navigate(LicenseScreenDestination)
            }
            Preference(
                title = stringResource(Res.string.settings_about_version),
                summary = versionCode(),
            )
            SwitchPreference(
                title = stringResource(Res.string.backup_before_update),
                value = Settings::backupBeforeUpdate,
            )
            SwitchPreference(
                title = stringResource(Res.string.use_ci_update_channel),
                value = Settings::useCIUpdateChannel,
            )
            SimpleMenuPreferenceInt(
                title = stringResource(Res.string.auto_updates),
                entry = R.array.update_frequency,
                entryValueRes = R.array.update_frequency_values,
                value = Settings::updateIntervalDays.observed,
            )
            WorkPreference(title = stringResource(Res.string.settings_about_check_for_updates)) {
                runSuspendCatching {
                    AppUpdater.checkForUpdate(true)?.let {
                        showNewVersion(context, it)
                    } ?: launchSnackBar(getString(Res.string.already_latest_version))
                }.onFailure {
                    launchSnackBar(getString(Res.string.update_failed, it.displayString()))
                }
            }
        }
    }
}

suspend fun DialogState.showNewVersion(context: Context, release: Release) {
    awaitConfirmationOrCancel(
        confirmText = Res.string.download,
        title = Res.string.new_version_available,
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
