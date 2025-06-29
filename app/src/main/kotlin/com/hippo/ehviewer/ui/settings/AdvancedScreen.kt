package com.hippo.ehviewer.ui.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.showRestartDialog
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberedAccessor
import com.hippo.ehviewer.util.AdsPlaceholderFile
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.CrashHandler
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.displayPath
import com.hippo.ehviewer.util.getAppLanguage
import com.hippo.ehviewer.util.getLanguages
import com.hippo.ehviewer.util.isAtLeastO
import com.hippo.ehviewer.util.isAtLeastSExtension7
import com.hippo.ehviewer.util.sendTo
import com.hippo.ehviewer.util.setAppLanguage
import com.hippo.files.delete
import com.hippo.files.toOkioPath
import com.jamal.composeprefs3.ui.prefs.DropDownPref
import com.jamal.composeprefs3.ui.prefs.SwitchPref
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.logcat
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import moe.tarsin.coroutines.runSuspendCatching
import moe.tarsin.launch
import moe.tarsin.snackbar
import moe.tarsin.string

context(ctx: Context)
private fun dumplog(uri: Uri): Unit = with(ctx) {
    grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    contentResolver.openOutputStream(uri)?.use { outputStream ->
        val files = ArrayList<File>()
        AppConfig.externalParseErrorDir?.listFiles()?.let { files.addAll(it) }
        AppConfig.externalCrashDir?.listFiles()?.let { files.addAll(it) }
        ZipOutputStream(outputStream).use { zipOs ->
            files.forEach { file ->
                if (!file.isFile) return@forEach
                val entry = ZipEntry(file.name)
                zipOs.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zipOs) }
            }
            val logcatEntry = ZipEntry("logcat-" + ReadableTime.getFilenamableTime() + ".txt")
            zipOs.putNextEntry(logcatEntry)
            CrashHandler.collectInfo(zipOs.writer())
            Runtime.getRuntime().exec("logcat -d").inputStream.use { it.copyTo(zipOs) }
        }
    }
}

context(ctx: Context)
private fun exportDatabase(uri: Uri) {
    ctx.grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    EhDB.exportDB(ctx, uri.toOkioPath())
}

context(ctx: Context)
private suspend fun importDatabase(uri: Uri) {
    ctx.grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    EhDB.importDB(ctx, uri)
}

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.AdvancedScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    fun launchSnackbar(message: String) = launch { snackbar(message) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_advanced)) },
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
            SwitchPreference(
                title = stringResource(id = R.string.settings_advanced_save_parse_error_body),
                summary = stringResource(id = R.string.settings_advanced_save_parse_error_body_summary),
                value = Settings::saveParseErrorBody,
            )
            val stripAds = Settings.stripExtraneousAds.asMutableState()
            SwitchPreference(
                title = stringResource(id = R.string.settings_block_extraneous_ads),
                value = stripAds.rememberedAccessor,
            )
            AnimatedVisibility(visible = stripAds.value) {
                LauncherPreference(
                    title = stringResource(id = R.string.settings_ads_placeholder),
                    contract = ActivityResultContracts.PickVisualMedia(),
                    key = PickVisualMediaRequest(mediaType = ImageOnly),
                ) { uri ->
                    withIOContext {
                        if (uri != null) {
                            uri.toOkioPath() sendTo AdsPlaceholderFile
                        } else {
                            AdsPlaceholderFile.delete()
                        }
                    }
                }
            }
            var saveCrashLog by Settings.saveCrashLog.asMutableState()
            SwitchPref(
                checked = saveCrashLog,
                onMutate = { saveCrashLog = !saveCrashLog },
                title = stringResource(id = R.string.settings_advanced_save_crash_log),
                summary = stringResource(id = R.string.settings_advanced_save_crash_log_summary),
            )
            val dumpLogError = stringResource(id = R.string.settings_advanced_dump_logcat_failed)
            LauncherPreference(
                title = stringResource(id = R.string.settings_advanced_dump_logcat),
                summary = stringResource(id = R.string.settings_advanced_dump_logcat_summary),
                contract = ActivityResultContracts.CreateDocument("application/zip"),
                key = "log-" + ReadableTime.getFilenamableTime() + ".zip",
            ) { uri ->
                uri?.run {
                    runCatching {
                        dumplog(uri)
                        launchSnackbar(string(R.string.settings_advanced_dump_logcat_to, uri.displayPath))
                    }.onFailure {
                        launchSnackbar(dumpLogError)
                        logcat(it)
                    }
                }
            }
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_advanced_read_cache_size),
                entry = R.array.read_cache_size_entries,
                entryValueRes = R.array.read_cache_size_entry_values,
                value = Settings::readCacheSize.observed,
            )
            var currentLanguage by remember { mutableStateOf(getAppLanguage()) }
            val languages = remember { getLanguages() }
            DropDownPref(
                title = stringResource(id = R.string.settings_advanced_app_language_title),
                defaultValue = currentLanguage,
                onValueChange = {
                    setAppLanguage(it)
                    currentLanguage = it
                },
                useSelectedAsSummary = true,
                entries = languages,
            )
            if (isAtLeastSExtension7) {
                var enableCronet by Settings.enableCronet.asMutableState()
                if (BuildConfig.DEBUG || !enableCronet) {
                    SwitchPref(
                        checked = enableCronet,
                        onMutate = { enableCronet = !enableCronet },
                        title = "Enable Cronet",
                    )
                }
                AnimatedVisibility(enableCronet) {
                    var enableQuic by Settings.enableQuic.asMutableState()
                    SwitchPref(
                        checked = enableQuic,
                        onMutate = { enableQuic = !enableQuic },
                        title = stringResource(id = R.string.settings_advanced_enable_quic),
                    )
                }
                LaunchedEffect(Unit) {
                    merge(
                        Settings.enableCronet.changesFlow(),
                        Settings.enableQuic.changesFlow(),
                    ).collectLatest {
                        showRestartDialog()
                    }
                }
            }
            if (isAtLeastO) {
                IntSliderPreference(
                    maxValue = 16384,
                    step = 3,
                    title = stringResource(id = R.string.settings_advanced_hardware_bitmap_threshold),
                    summary = stringResource(id = R.string.settings_advanced_hardware_bitmap_threshold_summary),
                    value = Settings::hardwareBitmapThreshold,
                )
            }
            SwitchPreference(
                title = stringResource(id = R.string.preload_thumb_aggressively),
                value = Settings::preloadThumbAggressively,
            )
            var animateItems by Settings.animateItems.asMutableState()
            SwitchPref(
                checked = animateItems,
                onMutate = { animateItems = !animateItems },
                title = stringResource(id = R.string.animate_items),
                summary = stringResource(id = R.string.animate_items_summary),
            )
            var desktopSite by Settings.desktopSite.asMutableState()
            SwitchPref(
                checked = desktopSite,
                onMutate = { desktopSite = !desktopSite },
                title = stringResource(id = R.string.desktop_site),
                summary = stringResource(id = R.string.desktop_site_summary),
            )
            val exportFailed = stringResource(id = R.string.settings_advanced_export_data_failed)
            LauncherPreference(
                title = stringResource(id = R.string.settings_advanced_export_data),
                summary = stringResource(id = R.string.settings_advanced_export_data_summary),
                contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
                key = ReadableTime.getFilenamableTime() + ".db",
            ) { uri ->
                uri?.let {
                    runCatching {
                        exportDatabase(uri)
                        launchSnackbar(string(R.string.settings_advanced_export_data_to, uri.displayPath))
                    }.onFailure {
                        logcat(it)
                        launchSnackbar(exportFailed)
                    }
                }
            }
            val importFailed = stringResource(id = R.string.cant_read_the_file)
            val importSucceed = stringResource(id = R.string.settings_advanced_import_data_successfully)
            LauncherPreference(
                title = stringResource(id = R.string.settings_advanced_import_data),
                summary = stringResource(id = R.string.settings_advanced_import_data_summary),
                contract = ActivityResultContracts.GetContent(),
                key = "application/octet-stream",
            ) { uri ->
                uri?.let {
                    runCatching {
                        importDatabase(uri)
                        launchSnackbar(importSucceed)
                    }.onFailure {
                        logcat(it)
                        launchSnackbar(importFailed)
                    }
                }
            }
            val hasSignedIn by Settings.hasSignedIn.collectAsState()
            if (hasSignedIn) {
                val backupNothing = stringResource(id = R.string.settings_advanced_backup_favorite_nothing)
                val backupFailed = stringResource(id = R.string.settings_advanced_backup_favorite_failed)
                val backupSucceed = stringResource(id = R.string.settings_advanced_backup_favorite_success)
                Preference(
                    title = stringResource(id = R.string.settings_advanced_backup_favorite),
                    summary = stringResource(id = R.string.settings_advanced_backup_favorite_summary),
                ) {
                    val favListUrlBuilder = FavListUrlBuilder()
                    var favTotal = 0
                    var favIndex = 0
                    tailrec suspend fun doBackup() {
                        val result = EhEngine.getFavorites(favListUrlBuilder.build())
                        if (result.galleryInfoList.isEmpty()) {
                            launchSnackbar(backupNothing)
                        } else {
                            if (favTotal == 0) favTotal = result.countArray.sum()
                            favIndex += result.galleryInfoList.size
                            val status = "($favIndex/$favTotal)"
                            EhDB.putLocalFavorites(result.galleryInfoList)
                            launchSnackbar(string(R.string.settings_advanced_backup_favorite_start, status))
                            if (result.next != null) {
                                delay(Settings.downloadDelay.toLong())
                                favListUrlBuilder.setIndex(result.next, true)
                                doBackup()
                            }
                        }
                    }
                    launch {
                        runSuspendCatching {
                            doBackup()
                        }.onSuccess {
                            launchSnackbar(backupSucceed)
                        }.onFailure {
                            logcat(it)
                            launchSnackbar(backupFailed)
                        }
                    }
                }
            }
            Preference(title = stringResource(id = R.string.open_by_default)) {
                openByDefaultSettings()
            }
        }
    }
}

context(ctx: Context)
private fun openByDefaultSettings() = with(ctx) {
    try {
        @SuppressLint("InlinedApi")
        val intent = Intent(
            ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            "package:$packageName".toUri(),
        )
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val intent = Intent(
            ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:$packageName".toUri(),
        )
        startActivity(intent)
    }
}
