package com.hippo.ehviewer.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.animate_items
import com.ehviewer.core.common.animate_items_summary
import com.ehviewer.core.common.cant_read_the_file
import com.ehviewer.core.common.open_by_default
import com.ehviewer.core.common.preload_thumb_aggressively
import com.ehviewer.core.common.settings_ads_placeholder
import com.ehviewer.core.common.settings_advanced
import com.ehviewer.core.common.settings_advanced_app_language_title
import com.ehviewer.core.common.settings_advanced_backup_favorite
import com.ehviewer.core.common.settings_advanced_backup_favorite_failed
import com.ehviewer.core.common.settings_advanced_backup_favorite_nothing
import com.ehviewer.core.common.settings_advanced_backup_favorite_start
import com.ehviewer.core.common.settings_advanced_backup_favorite_success
import com.ehviewer.core.common.settings_advanced_backup_favorite_summary
import com.ehviewer.core.common.settings_advanced_dump_logcat
import com.ehviewer.core.common.settings_advanced_dump_logcat_failed
import com.ehviewer.core.common.settings_advanced_dump_logcat_summary
import com.ehviewer.core.common.settings_advanced_dump_logcat_to
import com.ehviewer.core.common.settings_advanced_export_data
import com.ehviewer.core.common.settings_advanced_export_data_failed
import com.ehviewer.core.common.settings_advanced_export_data_summary
import com.ehviewer.core.common.settings_advanced_export_data_to
import com.ehviewer.core.common.settings_advanced_hardware_bitmap_threshold
import com.ehviewer.core.common.settings_advanced_hardware_bitmap_threshold_summary
import com.ehviewer.core.common.settings_advanced_import_data
import com.ehviewer.core.common.settings_advanced_import_data_successfully
import com.ehviewer.core.common.settings_advanced_import_data_summary
import com.ehviewer.core.common.settings_advanced_read_cache_size
import com.ehviewer.core.common.settings_advanced_save_crash_log
import com.ehviewer.core.common.settings_advanced_save_crash_log_summary
import com.ehviewer.core.common.settings_advanced_save_parse_error_body
import com.ehviewer.core.common.settings_advanced_save_parse_error_body_summary
import com.ehviewer.core.common.settings_block_extraneous_ads
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.collectAsState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Destination<RootGraph>
@Composable
fun AdvancedScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    fun launchSnackBar(content: String) = coroutineScope.launch { snackbarHostState.showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.settings_advanced)) },
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
            SwitchPreference(
                title = stringResource(Res.string.settings_advanced_save_parse_error_body),
                summary = stringResource(Res.string.settings_advanced_save_parse_error_body_summary),
                value = Settings::saveParseErrorBody,
            )
            val stripAds = Settings.stripExtraneousAds.asMutableState()
            SwitchPreference(
                title = stringResource(Res.string.settings_block_extraneous_ads),
                value = stripAds.rememberedAccessor,
            )
            AnimatedVisibility(visible = stripAds.value) {
                LauncherPreference(
                    title = stringResource(Res.string.settings_ads_placeholder),
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
            SwitchPreference(
                title = stringResource(Res.string.settings_advanced_save_crash_log),
                summary = stringResource(Res.string.settings_advanced_save_crash_log_summary),
                value = Settings::saveCrashLog,
            )
            val dumpLogError = stringResource(Res.string.settings_advanced_dump_logcat_failed)
            LauncherPreference(
                title = stringResource(Res.string.settings_advanced_dump_logcat),
                summary = stringResource(Res.string.settings_advanced_dump_logcat_summary),
                contract = ActivityResultContracts.CreateDocument("application/zip"),
                key = "log-" + ReadableTime.getFilenamableTime() + ".zip",
            ) { uri ->
                uri?.run {
                    context.runCatching {
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
                            launchSnackBar(getString(Res.string.settings_advanced_dump_logcat_to, uri.displayPath.orEmpty()))
                        }
                    }.onFailure {
                        launchSnackBar(dumpLogError)
                        logcat(it)
                    }
                }
            }
            SimpleMenuPreferenceInt(
                title = stringResource(Res.string.settings_advanced_read_cache_size),
                entry = R.array.read_cache_size_entries,
                entryValueRes = R.array.read_cache_size_entry_values,
                value = Settings::readCacheSize.observed,
            )
            var currentLanguage by remember { mutableStateOf(getAppLanguage()) }
            val languages = remember { context.getLanguages() }
            DropDownPref(
                title = stringResource(Res.string.settings_advanced_app_language_title),
                defaultValue = currentLanguage,
                onValueChange = {
                    setAppLanguage(it)
                    currentLanguage = it
                },
                useSelectedAsSummary = true,
                entries = languages,
            )
            var enableCronet by Settings.enableCronet.asMutableState()
            if (BuildConfig.DEBUG || !enableCronet) {
                SwitchPref(
                    checked = enableCronet,
                    onMutate = { enableCronet = !enableCronet },
                    title = "Enable Cronet",
                )
            }
            if (isAtLeastO) {
                IntSliderPreference(
                    maxValue = 16384,
                    step = 3,
                    title = stringResource(Res.string.settings_advanced_hardware_bitmap_threshold),
                    summary = stringResource(Res.string.settings_advanced_hardware_bitmap_threshold_summary),
                    value = Settings::hardwareBitmapThreshold,
                )
            }
            SwitchPreference(
                title = stringResource(Res.string.preload_thumb_aggressively),
                value = Settings::preloadThumbAggressively,
            )
            var animateItems by Settings.animateItems.asMutableState()
            SwitchPref(
                checked = animateItems,
                onMutate = { animateItems = !animateItems },
                title = stringResource(Res.string.animate_items),
                summary = stringResource(Res.string.animate_items_summary),
            )
            val exportFailed = stringResource(Res.string.settings_advanced_export_data_failed)
            LauncherPreference(
                title = stringResource(Res.string.settings_advanced_export_data),
                summary = stringResource(Res.string.settings_advanced_export_data_summary),
                contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
                key = ReadableTime.getFilenamableTime() + ".db",
            ) { uri ->
                uri?.let {
                    context.runCatching {
                        grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        EhDB.exportDB(context, uri.toOkioPath())
                        launchSnackBar(getString(Res.string.settings_advanced_export_data_to, uri.displayPath.orEmpty()))
                    }.onFailure {
                        logcat(it)
                        launchSnackBar(exportFailed)
                    }
                }
            }
            val importFailed = stringResource(Res.string.cant_read_the_file)
            val importSucceed = stringResource(Res.string.settings_advanced_import_data_successfully)
            LauncherPreference(
                title = stringResource(Res.string.settings_advanced_import_data),
                summary = stringResource(Res.string.settings_advanced_import_data_summary),
                contract = ActivityResultContracts.GetContent(),
                key = "application/octet-stream",
            ) { uri ->
                uri?.let {
                    context.runCatching {
                        grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        EhDB.importDB(context, uri)
                        launchSnackBar(importSucceed)
                    }.onFailure {
                        logcat(it)
                        launchSnackBar(importFailed)
                    }
                }
            }
            val hasSignedIn by Settings.hasSignedIn.collectAsState()
            if (hasSignedIn) {
                val backupNothing = stringResource(Res.string.settings_advanced_backup_favorite_nothing)
                val backupFailed = stringResource(Res.string.settings_advanced_backup_favorite_failed)
                val backupSucceed = stringResource(Res.string.settings_advanced_backup_favorite_success)
                Preference(
                    title = stringResource(Res.string.settings_advanced_backup_favorite),
                    summary = stringResource(Res.string.settings_advanced_backup_favorite_summary),
                ) {
                    val favListUrlBuilder = FavListUrlBuilder()
                    var favTotal = 0
                    var favIndex = 0
                    tailrec suspend fun doBackup() {
                        val result = EhEngine.getFavorites(favListUrlBuilder.build())
                        if (result.galleryInfoList.isEmpty()) {
                            launchSnackBar(backupNothing)
                        } else {
                            if (favTotal == 0) favTotal = result.countArray.sum()
                            favIndex += result.galleryInfoList.size
                            val status = "($favIndex/$favTotal)"
                            EhDB.putLocalFavorites(result.galleryInfoList)
                            launchSnackBar(getString(Res.string.settings_advanced_backup_favorite_start, status))
                            if (result.next != null) {
                                delay(Settings.downloadDelay.toLong())
                                favListUrlBuilder.setIndex(result.next, true)
                                doBackup()
                            }
                        }
                    }
                    coroutineScope.launch {
                        runSuspendCatching {
                            doBackup()
                        }.onSuccess {
                            launchSnackBar(backupSucceed)
                        }.onFailure {
                            logcat(it)
                            launchSnackBar(backupFailed)
                        }
                    }
                }
            }
            Preference(title = stringResource(Res.string.open_by_default)) {
                context.run {
                    try {
                        @SuppressLint("InlinedApi")
                        val intent = Intent(
                            ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                            Uri.parse("package:$packageName"),
                        )
                        startActivity(intent)
                    } catch (t: Throwable) {
                        val intent = Intent(
                            ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:$packageName"),
                        )
                        startActivity(intent)
                    }
                }
            }
        }
    }
}
