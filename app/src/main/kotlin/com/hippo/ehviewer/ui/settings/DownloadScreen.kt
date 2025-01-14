package com.hippo.ehviewer.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.parMapNotNull
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine.fillGalleryListByApi
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.ParserUtils
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.downloadDir
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.spider.COMIC_INFO_FILE
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.SPIDER_INFO_FILENAME
import com.hippo.ehviewer.spider.readComicInfo
import com.hippo.ehviewer.spider.readCompatFromPath
import com.hippo.ehviewer.spider.speedLevelToSpeed
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberedAccessor
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.displayPath
import com.hippo.ehviewer.util.displayString
import com.hippo.ehviewer.util.isAtLeastQ
import com.hippo.ehviewer.util.requestPermission
import com.hippo.files.delete
import com.hippo.files.find
import com.hippo.files.isDirectory
import com.hippo.files.list
import com.hippo.files.metadataOrNull
import com.hippo.files.mkdirs
import com.hippo.files.toOkioPath
import com.hippo.files.toUri
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import okio.Path
import okio.Path.Companion.toOkioPath
import splitties.init.appCtx

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.DownloadScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    fun launchSnackBar(content: String) = launch { showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_download)) },
                navigationIcon = {
                    IconButton(onClick = { popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState()).padding(paddingValues)) {
            var downloadLocationState by ::downloadLocation.observed
            val cannotGetDownloadLocation = stringResource(id = R.string.settings_download_cant_get_download_location)
            val selectDownloadDirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
                treeUri?.run {
                    launchIO {
                        runCatching {
                            contentResolver.takePersistableUriPermission(treeUri, FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)
                            val path = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri)).toOkioPath()
                            check(path.isDirectory) { "$path is not a directory" }
                            keepNoMediaFileStatus(path) // Check if the directory is writable
                            downloadLocationState = path
                        }.onFailure {
                            logcat(it)
                            launchSnackBar(cannotGetDownloadLocation)
                        }
                    }
                }
            }
            Preference(
                title = stringResource(id = R.string.settings_download_download_location),
                summary = downloadLocationState.toUri().displayPath,
            ) {
                launchIO {
                    val defaultDownloadDir = AppConfig.defaultDownloadDir
                    if (defaultDownloadDir?.delete() == false) {
                        val path = defaultDownloadDir.toOkioPath()
                        awaitConfirmationOrCancel(
                            confirmText = R.string.pick_new_download_location,
                            dismissText = if (downloadLocationState != path) {
                                R.string.reset_download_location
                            } else {
                                android.R.string.cancel
                            },
                            title = R.string.waring,
                            onCancelButtonClick = {
                                downloadLocationState = path
                            },
                        ) {
                            Text(stringResource(id = R.string.default_download_dir_not_empty))
                        }
                    }
                    try {
                        selectDownloadDirLauncher.launch(null)
                    } catch (_: ActivityNotFoundException) {
                        // Best effort for devices without DocumentsUI
                        if (!isAtLeastQ && requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            runCatching {
                                val path = Environment.getExternalStorageDirectory().toOkioPath() / AppConfig.APP_DIRNAME
                                path.mkdirs()
                                check(path.isDirectory) { "$path is not a directory" }
                                keepNoMediaFileStatus(path) // Check if the directory is writable
                                downloadLocationState = path
                                return@launchIO
                            }.onFailure {
                                logcat(it)
                            }
                        }
                        launchSnackBar(cannotGetDownloadLocation)
                    }
                }
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
                maxValue = 10,
                minValue = 2,
                step = 7,
                title = stringResource(id = R.string.settings_download_connection_timeout),
                value = Settings::connTimeout,
            )
            IntSliderPreference(
                maxValue = 10,
                minValue = 4,
                step = 5,
                title = stringResource(id = R.string.settings_download_timeout_speed),
                value = Settings::timeoutSpeed,
                display = ::speedLevelToSpeed,
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
                title = stringResource(id = R.string.settings_download_save_as_cbz),
                value = Settings::saveAsCbz,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_download_archive_metadata),
                summary = stringResource(id = R.string.settings_download_archive_metadata_summary),
                value = Settings::archiveMetadata,
            )
            WorkPreference(
                title = stringResource(id = R.string.settings_download_reload_metadata),
                summary = stringResource(id = R.string.settings_download_reload_metadata_summary),
            ) {
                fun DownloadInfo.isStable(): Boolean {
                    val downloadTime = downloadDir?.resolve(COMIC_INFO_FILE)?.metadataOrNull()?.lastModifiedAtMillis ?: return false
                    val postedTime = posted?.let { ParserUtils.parseDate(it) } ?: return false
                    // stable 30 days after posted
                    val stableTime = postedTime + 30L * 24L * 60L * 60L * 1000L
                    return downloadTime > stableTime
                }

                runSuspendCatching {
                    DownloadManager.downloadInfoList.parMapNotNull {
                        if (it.state == DownloadInfo.STATE_FINISH && !it.isStable()) it else null
                    }.apply {
                        fillGalleryListByApi(this, EhUrl.referer)
                        val toUpdate = parMap { di ->
                            di.galleryInfo.also { SpiderDen(it, di.dirname!!).writeComicInfo(false) }
                        }
                        EhDB.updateGalleryInfo(toUpdate)
                        launchSnackBar(getString(R.string.settings_download_reload_metadata_successfully, toUpdate.size))
                    }
                }.onFailure {
                    launchSnackBar(getString(R.string.settings_download_reload_metadata_failed, it.displayString()))
                }
            }
            val restoreFailed = stringResource(id = R.string.settings_download_restore_failed)
            WorkPreference(
                title = stringResource(id = R.string.settings_download_restore_download_items),
                summary = stringResource(id = R.string.settings_download_restore_download_items_summary),
            ) {
                var restoreDirCount = 0
                suspend fun getRestoreItem(file: Path): RestoreItem? {
                    if (!file.isDirectory) return null
                    return runSuspendCatching {
                        val (gid, token) = file.find(SPIDER_INFO_FILENAME)?.let {
                            readCompatFromPath(it)?.run {
                                GalleryDetailUrlParser.Result(gid, token)
                            }
                        } ?: file.find(COMIC_INFO_FILE)?.let {
                            readComicInfo(it)?.run {
                                GalleryDetailUrlParser.parse(web)
                            }
                        } ?: return null
                        val dirname = file.name
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
                runSuspendCatching {
                    val result = downloadLocation.list().parMapNotNull { getRestoreItem(it) }.also {
                        fillGalleryListByApi(it, EhUrl.referer)
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
                    launchSnackBar(restoreFailed)
                }
            }
            WorkPreference(
                title = stringResource(id = R.string.settings_download_clean_redundancy),
                summary = stringResource(id = R.string.settings_download_clean_redundancy_summary),
            ) {
                fun isRedundant(file: Path): Boolean {
                    if (!file.isDirectory) return false
                    val name = file.name
                    val gid = name.substringBefore('-').toLongOrNull() ?: return false
                    return name != DownloadManager.getDownloadInfo(gid)?.dirname
                }
                val list = downloadLocation.list().filter(::isRedundant)
                if (list.isNotEmpty()) {
                    awaitConfirmationOrCancel(
                        confirmText = R.string.delete,
                        title = R.string.settings_download_clean_redundancy,
                    ) {
                        LazyColumn {
                            items(list) {
                                Text(it.name, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
                val cnt = list.count { runCatching { it.delete() }.getOrNull() != null }
                launchSnackBar(FINAL_CLEAR_REDUNDANCY_MSG(cnt))
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
