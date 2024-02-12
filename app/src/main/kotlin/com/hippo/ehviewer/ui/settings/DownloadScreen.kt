package com.hippo.ehviewer.ui.settings

import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import arrow.fx.coroutines.parMap
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine.fillGalleryListByApi
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.spider.COMIC_INFO_FILE
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.SPIDER_INFO_FILENAME
import com.hippo.ehviewer.spider.readComicInfo
import com.hippo.ehviewer.spider.readCompatFromUniFile
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberedAccessor
import com.hippo.unifile.UniFile
import com.hippo.unifile.asUniFile
import com.hippo.unifile.displayPath
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import splitties.init.appCtx

@Destination
@Composable
fun DownloadScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val snackbarHostState = remember { SnackbarHostState() }
    fun launchSnackBar(content: String) = coroutineScope.launch { snackbarHostState.showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_download)) },
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
            var downloadLocationState by ::downloadLocation.observed
            val cannotGetDownloadLocation = stringResource(id = R.string.settings_download_cant_get_download_location)
            val selectDownloadDirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
                treeUri?.run {
                    coroutineScope.launch {
                        context.runCatching {
                            contentResolver.takePersistableUriPermission(treeUri, FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)
                            downloadLocationState = treeUri.asUniFile()
                            coroutineScope.launchNonCancellable {
                                keepNoMediaFileStatus(downloadLocationState)
                            }
                        }.onFailure {
                            launchSnackBar(cannotGetDownloadLocation)
                        }
                    }
                }
            }
            Preference(
                title = stringResource(id = R.string.settings_download_download_location),
                summary = downloadLocationState.uri.displayPath,
            ) {
                selectDownloadDirLauncher.launch(null)
            }
            val mediaScan = Settings::mediaScan.observed
            SwitchPreference(
                title = stringResource(id = R.string.settings_download_media_scan),
                summary = if (mediaScan.value) stringResource(id = R.string.settings_download_media_scan_summary_on) else stringResource(id = R.string.settings_download_media_scan_summary_off),
                value = mediaScan.rememberedAccessor,
            )
            val multiThreadDownload = Settings::multiThreadDownload.observed
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_download_concurrency),
                summary = stringResource(id = R.string.settings_download_concurrency_summary, multiThreadDownload.value),
                entry = R.array.multi_thread_download_entries,
                entryValueRes = R.array.multi_thread_download_entry_values,
                value = multiThreadDownload,
            )
            val downloadDelay = Settings::downloadDelay.observed
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_download_download_delay),
                summary = stringResource(id = R.string.settings_download_download_delay_summary, downloadDelay.value),
                entry = R.array.download_delay_entries,
                entryValueRes = R.array.download_delay_entry_values,
                value = downloadDelay,
            )
            IntSliderPreference(
                maxValue = 120,
                minValue = 10,
                step = 10,
                title = stringResource(id = R.string.settings_download_download_timeout),
                value = Settings::downloadTimeout,
            )
            val preloadImage = Settings::preloadImage.observed
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_download_preload_image),
                summary = stringResource(id = R.string.settings_download_preload_image_summary, preloadImage.value),
                entry = R.array.preload_image_entries,
                entryValueRes = R.array.preload_image_entry_values,
                value = preloadImage,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_download_download_origin_image),
                summary = stringResource(id = R.string.settings_download_download_origin_image_summary),
                value = Settings::downloadOriginImage,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_download_archive_metadata),
                summary = stringResource(id = R.string.settings_download_archive_metadata_summary),
                value = Settings::archiveMetadata,
            )
            WorkPreference(
                title = stringResource(id = R.string.settings_download_restore_download_items),
                summary = stringResource(id = R.string.settings_download_restore_download_items_summary),
            ) {
                var restoreDirCount = 0
                suspend fun getRestoreItem(file: UniFile): RestoreItem? {
                    if (!file.isDirectory) return null
                    return runCatching {
                        val (gid, token) = file.findFile(SPIDER_INFO_FILENAME)?.let {
                            readCompatFromUniFile(it)?.run {
                                GalleryDetailUrlParser.Result(gid, token)
                            }
                        } ?: file.findFile(COMIC_INFO_FILE)?.let {
                            readComicInfo(it)?.run {
                                GalleryDetailUrlParser.parse(web)
                            }
                        } ?: return null
                        val dirname = file.name ?: return null
                        if (DownloadManager.containDownloadInfo(gid)) {
                            // Restore download dir to avoid redownload
                            val dbdirname = EhDB.getDownloadDirname(gid)
                            if (null == dbdirname || dirname != dbdirname) {
                                EhDB.putDownloadDirname(gid, dirname)
                                restoreDirCount++
                            }
                            return null
                        }
                        RestoreItem(dirname, gid, token)
                    }.onFailure {
                        logcat(it)
                    }.getOrNull()
                }
                runCatching {
                    val result = downloadLocation.listFiles().mapNotNull { getRestoreItem(it) }.apply {
                        runSuspendCatching {
                            fillGalleryListByApi(this, EhUrl.referer)
                        }.onFailure {
                            logcat(it)
                        }
                    }
                    if (result.isEmpty()) {
                        launchSnackBar(RESTORE_COUNT_MSG(restoreDirCount))
                    } else {
                        val count = result.parMap {
                            if (it.pages != 0) {
                                EhDB.putDownloadDirname(it.gid, it.dirname)
                                DownloadManager.restoreDownload(it.galleryInfo, it.dirname)
                                SpiderDen(it.galleryInfo, it.dirname).writeComicInfo(false)
                            }
                        }.size
                        launchSnackBar(RESTORE_COUNT_MSG(count + restoreDirCount))
                    }
                }.onFailure {
                    logcat(it)
                }
            }
            WorkPreference(
                title = stringResource(id = R.string.settings_download_clean_redundancy),
                summary = stringResource(id = R.string.settings_download_clean_redundancy_summary),
            ) {
                fun clearFile(file: UniFile): Boolean {
                    var name = file.name ?: return false
                    val index = name.indexOf('-')
                    if (index >= 0) {
                        name = name.substring(0, index)
                    }
                    val gid = name.toLongOrNull() ?: return false
                    if (DownloadManager.containDownloadInfo(gid)) {
                        return false
                    }
                    file.delete()
                    return true
                }
                runSuspendCatching {
                    val cnt = downloadLocation.listFiles().sumOf { clearFile(it).compareTo(false) }
                    launchSnackBar(FINAL_CLEAR_REDUNDANCY_MSG(cnt))
                }.onFailure {
                    logcat(it)
                }
            }
        }
    }
}

private class RestoreItem(
    val dirname: String,
    gid: Long,
    token: String,
    val galleryInfo: BaseGalleryInfo = BaseGalleryInfo(gid, token),
) : GalleryInfo by galleryInfo
private val RESTORE_NOT_FOUND = appCtx.getString(R.string.settings_download_restore_not_found)
private val RESTORE_COUNT_MSG = { cnt: Int -> if (cnt == 0) RESTORE_NOT_FOUND else appCtx.getString(R.string.settings_download_restore_successfully, cnt) }
private val NO_REDUNDANCY = appCtx.getString(R.string.settings_download_clean_redundancy_no_redundancy)
private val CLEAR_REDUNDANCY_DONE = { cnt: Int -> appCtx.getString(R.string.settings_download_clean_redundancy_done, cnt) }
private val FINAL_CLEAR_REDUNDANCY_MSG = { cnt: Int -> if (cnt == 0) NO_REDUNDANCY else CLEAR_REDUNDANCY_DONE(cnt) }
