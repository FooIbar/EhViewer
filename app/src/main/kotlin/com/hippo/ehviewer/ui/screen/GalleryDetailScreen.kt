package com.hippo.ehviewer.ui.screen

import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.collection.SieveCache
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.viewModelScope
import com.hippo.ehviewer.EhApplication.Companion.imageCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.findBaseInfo
import com.hippo.ehviewer.client.getImageKey
import com.hippo.ehviewer.coil.justDownload
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ktbuilder.executeIn
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.GalleryDetailErrorTip
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.tools.launchInVM
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.AppHelper
import com.hippo.ehviewer.util.awaitActivityResult
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.displayString
import com.hippo.files.delete
import com.hippo.files.toOkioPath
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.parcelize.Parcelize
import moe.tarsin.coroutines.runSuspendCatching

typealias VoteTag = suspend GalleryDetail.(String, Int) -> Unit

val detailCache = SieveCache<Long, GalleryDetail>(25)

sealed interface GalleryDetailScreenArgs : Parcelable

@Parcelize
data class GalleryInfoArgs(
    val galleryInfo: BaseGalleryInfo,
) : GalleryDetailScreenArgs

@Parcelize
data class TokenArgs(
    val gid: Long,
    val token: String,
    val page: Int = 0,
) : GalleryDetailScreenArgs

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.GalleryDetailScreen(args: GalleryDetailScreenArgs, navigator: DestinationsNavigator) = Screen(navigator) {
    val (gid, token) = remember(args) {
        when (args) {
            is GalleryInfoArgs -> with(args.galleryInfo) { gid to token }
            is TokenArgs -> args.gid to args.token
        }
    }
    val galleryDetailUrl = remember(gid, token) { EhUrl.getGalleryDetailUrl(gid, token, 0, false) }
    ProvideAssistContent(galleryDetailUrl)

    var galleryInfo by rememberInVM {
        val casted = args as? GalleryInfoArgs
        mutableStateOf<GalleryInfo?>(casted?.galleryInfo)
    }

    var getDetailError by rememberSaveable { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    (galleryInfo as? GalleryDetail)?.apply {
        rememberInVM(this) {
            if (Settings.preloadThumbAggressively) {
                previewList.forEach {
                    imageRequest(it) { justDownload() }.executeIn(viewModelScope)
                }
            }
        }
    }

    if (galleryInfo !is GalleryDetail && getDetailError.isBlank()) {
        LaunchedEffect(Unit) {
            val galleryDetail = detailCache[gid]
                ?: runSuspendCatching {
                    withIOContext { EhEngine.getGalleryDetail(galleryDetailUrl) }
                }.onSuccess { galleryDetail ->
                    detailCache[galleryDetail.gid] = galleryDetail
                }.onFailure {
                    galleryInfo?.let { info -> EhDB.putHistoryInfo(info.findBaseInfo()) }
                    getDetailError = it.displayString()
                }.getOrNull()
            galleryDetail?.let {
                EhDB.putHistoryInfo(it.galleryInfo)
                galleryInfo = it
            }
        }
    }

    val voteTag: VoteTag = { tag, vote ->
        runSuspendCatching {
            EhEngine.voteTag(apiUid, apiKey, gid, token, tag, vote)
        }.onSuccess { result ->
            val new = copy(tagGroups = result).apply { fillInfo() }
            detailCache[gid] = new
            galleryInfo = new
            showTip(R.string.tag_vote_successfully)
        }.onFailure { e ->
            showTip(e.displayString())
        }
    }

    val signInFirst = stringResource(R.string.sign_in_first)
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    galleryInfo?.let {
                        Text(
                            text = EhUtils.getSuitableTitle(it),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            AppHelper.share(implicit<MainActivity>(), galleryDetailUrl)
                            // In case the link is copied to the clipboard
                            Settings.clipboardTextHashCode = galleryDetailUrl.hashCode()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                        )
                    }
                    var dropdown by rememberSaveable { mutableStateOf(false) }
                    IconButton(onClick = { dropdown = !dropdown }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                        )
                    }
                    DropdownMenu(
                        expanded = dropdown,
                        onDismissRequest = { dropdown = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.action_add_tag)) },
                            onClick = {
                                dropdown = false
                                val detail = galleryInfo as? GalleryDetail ?: return@DropdownMenuItem
                                launchIO {
                                    if (detail.apiUid < 0) {
                                        showSnackbar(signInFirst)
                                    } else {
                                        val tags = awaitSelectTags()
                                        if (tags.isNotEmpty()) {
                                            val text = tags.fastJoinToString(",")
                                            detail.voteTag(text, 1)
                                        }
                                    }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.refresh)) },
                            onClick = {
                                dropdown = false
                                // Invalidate cache
                                detailCache.remove(gid)

                                // Trigger recompose
                                galleryInfo = galleryInfo?.findBaseInfo()
                                getDetailError = ""
                            },
                        )
                        val imageCacheClear = stringResource(R.string.image_cache_cleared)
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.clear_image_cache)) },
                            onClick = {
                                dropdown = false
                                val gd = galleryInfo as? GalleryDetail ?: return@DropdownMenuItem
                                launchIO {
                                    awaitConfirmationOrCancel(
                                        confirmText = R.string.clear_all,
                                        title = R.string.clear_image_cache,
                                    ) {
                                        Text(text = stringResource(id = R.string.clear_image_cache_confirm))
                                    }
                                    (0..<gd.pages).forEach {
                                        val key = getImageKey(gd.gid, it)
                                        imageCache.remove(key)
                                    }
                                    showSnackbar(imageCacheClear)
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.open_in_other_app)) },
                            onClick = {
                                dropdown = false
                                openBrowser(galleryDetailUrl)
                            },
                        )
                        val exportSuccess = stringResource(id = R.string.export_as_archive_success)
                        val exportFailed = stringResource(id = R.string.export_as_archive_failed)
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.export_as_archive)) },
                            onClick = {
                                dropdown = false
                                launchIO {
                                    val downloadInfo = DownloadManager.getDownloadInfo(gid)
                                    val canExport = downloadInfo?.state == DownloadInfo.STATE_FINISH
                                    if (!canExport) {
                                        awaitConfirmationOrCancel(
                                            showCancelButton = false,
                                            text = { Text(text = stringResource(id = R.string.download_gallery_first)) },
                                        )
                                    } else {
                                        val info = galleryInfo!!
                                        val uri = awaitActivityResult(
                                            CreateDocument("application/vnd.comicbook+zip"),
                                            EhUtils.getSuitableTitle(info) + ".cbz",
                                        )
                                        val dirname = downloadInfo.dirname
                                        if (uri != null && dirname != null) {
                                            val file = uri.toOkioPath()
                                            val msg = runCatching {
                                                bgWork {
                                                    withIOContext {
                                                        SpiderDen(info, dirname).exportAsCbz(file)
                                                    }
                                                }
                                                exportSuccess
                                            }.getOrElse {
                                                logcat(it)
                                                file.delete()
                                                exportFailed
                                            }
                                            showSnackbar(message = msg)
                                        }
                                    }
                                }
                            },
                        )
                    }
                },
            )
        },
    ) {
        val gi = galleryInfo
        if (gi != null) {
            if (args is TokenArgs && args.page != 0) {
                val from = stringResource(id = R.string.read_from, args.page)
                val read = stringResource(id = R.string.read)
                launchInVM {
                    val result = showSnackbar(from, read, true)
                    if (result == SnackbarResult.ActionPerformed) {
                        navToReader(gi.findBaseInfo(), args.page)
                    }
                }
            }
            GalleryDetailContent(
                galleryInfo = gi,
                contentPadding = it,
                getDetailError = getDetailError,
                onRetry = { getDetailError = "" },
                voteTag = voteTag,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            )
        } else if (getDetailError.isNotBlank()) {
            GalleryDetailErrorTip(error = getDetailError, onClick = { getDetailError = "" })
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
