package com.hippo.ehviewer.ui.screen

import android.Manifest
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.text.TextUtils.TruncateAt.END
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVerticalCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.window.core.layout.WindowWidthSizeClass
import arrow.core.partially1
import coil3.imageLoader
import com.hippo.ehviewer.EhApplication.Companion.galleryDetailCache
import com.hippo.ehviewer.EhApplication.Companion.imageCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.client.data.GalleryTagGroup
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.TagNamespace
import com.hippo.ehviewer.client.data.asGalleryDetail
import com.hippo.ehviewer.client.data.findBaseInfo
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.NoHAtHClientException
import com.hippo.ehviewer.client.getImageKey
import com.hippo.ehviewer.coil.justDownload
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.download.DownloadManager as EhDownloadManager
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.ui.GalleryInfoBottomSheet
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.composing
import com.hippo.ehviewer.ui.confirmRemoveDownload
import com.hippo.ehviewer.ui.destinations.GalleryCommentsScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryPreviewScreenDestination
import com.hippo.ehviewer.ui.getFavoriteIcon
import com.hippo.ehviewer.ui.jumpToReaderByPage
import com.hippo.ehviewer.ui.legacy.CoilImageGetter
import com.hippo.ehviewer.ui.main.ArchiveList
import com.hippo.ehviewer.ui.main.EhPreviewItem
import com.hippo.ehviewer.ui.main.GalleryCommentCard
import com.hippo.ehviewer.ui.main.GalleryDetailErrorTip
import com.hippo.ehviewer.ui.main.GalleryDetailHeaderCard
import com.hippo.ehviewer.ui.main.GalleryTags
import com.hippo.ehviewer.ui.main.TorrentList
import com.hippo.ehviewer.ui.modifyFavorites
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.startDownload
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.EmptyWindowInsets
import com.hippo.ehviewer.ui.tools.FilledTertiaryIconButton
import com.hippo.ehviewer.ui.tools.FilledTertiaryIconToggleButton
import com.hippo.ehviewer.ui.tools.GalleryDetailRating
import com.hippo.ehviewer.ui.tools.GalleryRatingBar
import com.hippo.ehviewer.ui.tools.LocalWindowSizeClass
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.ui.tools.rememberLambda
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.AppHelper
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.awaitActivityResult
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.displayString
import com.hippo.ehviewer.util.isAtLeastQ
import com.hippo.ehviewer.util.requestPermission
import com.hippo.files.delete
import com.hippo.files.toOkioPath
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import moe.tarsin.coroutines.runSuspendCatching
import splitties.systemservices.downloadManager

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

@StringRes
private fun getRatingText(rating: Float): Int = when ((rating * 2).roundToInt()) {
    0 -> R.string.rating0
    1 -> R.string.rating1
    2 -> R.string.rating2
    3 -> R.string.rating3
    4 -> R.string.rating4
    5 -> R.string.rating5
    6 -> R.string.rating6
    7 -> R.string.rating7
    8 -> R.string.rating8
    9 -> R.string.rating9
    10 -> R.string.rating10
    else -> R.string.rating_none
}

private fun List<GalleryTagGroup>.getArtistTag(): String? {
    for (tagGroup in this) {
        if (tagGroup.isNotEmpty()) {
            val namespace = tagGroup.groupName
            if (namespace == TagNamespace.Artist.value || namespace == TagNamespace.Cosplayer.value) {
                return "$namespace:${tagGroup[0].removePrefix("_")}"
            }
        }
    }
    return null
}

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.GalleryDetailScreen(args: GalleryDetailScreenArgs, navigator: DestinationsNavigator) = composing(navigator) {
    var galleryInfo by rememberInVM {
        val casted = args as? GalleryInfoArgs
        mutableStateOf<GalleryInfo?>(casted?.galleryInfo)
    }
    val (gid, token) = remember {
        when (args) {
            is GalleryInfoArgs -> with(args.galleryInfo) { gid to token }
            is TokenArgs -> args.gid to args.token
        }
    }
    val galleryDetailUrl = remember { EhUrl.getGalleryDetailUrl(gid, token, 0, false) }
    var showReadAction by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(args, galleryInfo) {
        if (showReadAction) {
            val page = (args as? TokenArgs)?.page ?: 0
            val gi = galleryInfo
            if (page != 0 && gi != null) {
                showReadAction = false
                val result = showSnackbar(
                    getString(R.string.read_from, page),
                    getString(R.string.read),
                    true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    navToReader(gi.findBaseInfo(), page)
                }
            }
        }
    }
    ProvideAssistContent(galleryDetailUrl)
    var getDetailError by rememberSaveable { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val voteSuccess = stringResource(R.string.tag_vote_successfully)
    val voteFailed = stringResource(R.string.vote_failed)

    if (galleryInfo !is GalleryDetail && getDetailError.isBlank()) {
        LaunchedEffect(Unit) {
            val galleryDetail = galleryDetailCache[gid]
                ?: runSuspendCatching {
                    withIOContext { EhEngine.getGalleryDetail(galleryDetailUrl) }
                }.onSuccess { galleryDetail ->
                    galleryDetailCache.put(galleryDetail.gid, galleryDetail)
                    if (Settings.preloadThumbAggressively) {
                        launchIO {
                            galleryDetail.previewList.forEach {
                                imageLoader.enqueue(imageRequest(it) { justDownload() })
                            }
                        }
                    }
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

    suspend fun GalleryDetail.voteTag(tag: String, vote: Int) {
        runSuspendCatching {
            EhEngine.voteTag(apiUid, apiKey, gid, token, tag, vote)
        }.onSuccess { result ->
            if (result != null) {
                showSnackbar(result)
            } else {
                showSnackbar(voteSuccess)
            }
        }.onFailure {
            showSnackbar(voteFailed)
        }
    }

    val archiveResult = remember(galleryInfo) {
        async(Dispatchers.IO + Job(), CoroutineStart.LAZY) {
            val detail = galleryInfo as GalleryDetail
            EhEngine.getArchiveList(detail.archiveUrl!!, gid, token)
        }
    }

    val failureNoHath = stringResource(R.string.download_archive_failure_no_hath)
    val noArchive = stringResource(R.string.no_archives)
    val downloadStarted = stringResource(R.string.download_archive_started)
    val downloadFailed = stringResource(R.string.download_archive_failure)
    val signInFirst = stringResource(R.string.sign_in_first)
    fun showArchiveDialog() {
        val galleryDetail = galleryInfo as? GalleryDetail ?: return
        launchIO {
            if (galleryDetail.apiUid < 0) {
                showSnackbar(signInFirst)
            } else {
                runSuspendCatching {
                    val (paramOr, archiveList, funds) = bgWork { archiveResult.await() }
                    if (archiveList.isEmpty()) {
                        showSnackbar(noArchive)
                    } else {
                        val selected = showNoButton {
                            ArchiveList(
                                funds = funds,
                                items = archiveList,
                                onItemClick = { dismissWith(it) },
                            )
                        }
                        EhEngine.downloadArchive(gid, token, paramOr, selected.res, selected.isHAtH)?.let {
                            val uri = Uri.parse(it)
                            val intent = Intent().apply {
                                action = Intent.ACTION_VIEW
                                setDataAndType(uri, "application/zip")
                            }
                            val name = "$gid-${EhUtils.getSuitableTitle(galleryDetail)}.zip"
                            try {
                                startActivity(intent)
                                withUIContext { addTextToClipboard(name, true) }
                            } catch (_: ActivityNotFoundException) {
                                val r = DownloadManager.Request(uri)
                                r.setDestinationInExternalPublicDir(
                                    Environment.DIRECTORY_DOWNLOADS,
                                    AppConfig.APP_DIRNAME + "/" + FileUtils.sanitizeFilename(name),
                                )
                                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                downloadManager.enqueue(r)
                            }
                            if (Settings.archiveMetadata) {
                                SpiderDen(galleryDetail).apply {
                                    initDownloadDir()
                                    writeComicInfo()
                                }
                            }
                        }
                        showSnackbar(downloadStarted)
                    }
                }.onFailure {
                    when (it) {
                        is NoHAtHClientException -> showSnackbar(failureNoHath)
                        is EhException -> showSnackbar(it.displayString())
                        else -> {
                            logcat(it)
                            showSnackbar(downloadFailed)
                        }
                    }
                }
            }
        }
    }

    val keylineMargin = dimensionResource(R.dimen.keyline_margin)

    fun navigateToPreview(nextPage: Boolean = false) {
        (galleryInfo as? GalleryDetail)?.let {
            navigator.navigate(GalleryPreviewScreenDestination(it, nextPage))
        }
    }

    fun LazyGridScope.galleryDetailPreview(gd: GalleryDetail) {
        val previewList = gd.previewList
        items(previewList, key = { it.position }, contentType = { "preview" }) {
            EhPreviewItem(
                galleryPreview = it,
                position = it.position,
                onClick = { navToReader(gd.galleryInfo, it.position) },
            )
        }
        item(
            key = "footer",
            span = { GridItemSpan(maxLineSpan) },
            contentType = "footer",
        ) {
            val footerText = if (gd.previewPages <= 0 || previewList.isEmpty()) {
                stringResource(R.string.no_previews)
            } else if (gd.previewPages == 1) {
                stringResource(R.string.no_more_previews)
            } else {
                stringResource(R.string.more_previews)
            }
            TextButton(onClick = ::navigateToPreview.partially1(true)) {
                Text(footerText)
            }
        }
    }

    @Composable
    fun BelowHeader(galleryDetail: GalleryDetail) {
        @Composable
        fun EhIconButton(
            icon: ImageVector,
            text: String,
            onClick: () -> Unit,
        ) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTertiaryIconButton(onClick = onClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
            Text(text = text)
        }

        @Composable
        fun GalleryDetailComment(commentsList: List<GalleryComment>) {
            val maxShowCount = 2
            val commentText = when {
                commentsList.isEmpty() -> stringResource(R.string.no_comments)
                commentsList.size <= maxShowCount -> stringResource(R.string.no_more_comments)
                else -> stringResource(R.string.more_comment)
            }
            fun onNavigateToCommentScene() {
                navigator.navigate(GalleryCommentsScreenDestination(galleryDetail.gid))
            }
            CrystalCard {
                commentsList.take(maxShowCount).forEach { item ->
                    GalleryCommentCard(
                        modifier = Modifier.padding(vertical = 4.dp),
                        comment = item,
                        onCardClick = ::onNavigateToCommentScene,
                        onUserClick = ::onNavigateToCommentScene,
                        onUrlClick = { if (!jumpToReaderByPage(it, galleryDetail)) if (!navWithUrl(it)) openBrowser(it) },
                    ) {
                        maxLines = 5
                        ellipsize = END
                        text = item.comment.parseAsHtml(imageGetter = CoilImageGetter(this))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensionResource(id = R.dimen.strip_item_padding_v))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = ::onNavigateToCommentScene),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(commentText)
                }
            }
        }
        suspend fun showNewerVersionDialog() {
            val items = galleryDetail.newerVersions.map {
                getString(R.string.newer_version_title, it.title, it.posted)
            }
            val selected = awaitSelectItem(items)
            val info = galleryDetail.newerVersions[selected]
            withUIContext {
                // Can't use GalleryInfoArgs as thumbKey is null
                navigator.navigate(info.gid asDstWith info.token)
            }
        }
        Spacer(modifier = Modifier.size(keylineMargin))
        if (galleryDetail.newerVersions.isNotEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                CrystalCard(
                    onClick = { launchIO { showNewerVersionDialog() } },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                ) {
                }
                Text(text = stringResource(id = R.string.newer_version_available))
            }
            Spacer(modifier = Modifier.size(keylineMargin))
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            val favSlot by FavouriteStatusRouter.collectAsState(galleryDetail) { it }
            val favButtonText = if (favSlot != NOT_FAVORITED) {
                galleryDetail.favoriteName ?: stringResource(id = R.string.local_favorites)
            } else {
                stringResource(id = R.string.not_favorited)
            }
            val favoritesLock = remember { MutatorMutex() }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val removeSucceed = stringResource(R.string.remove_from_favorite_success)
                val addSucceed = stringResource(R.string.add_to_favorite_success)
                // val removeFailed = stringResource(R.string.remove_from_favorite_failure)
                val addFailed = stringResource(R.string.add_to_favorite_failure)
                FilledTertiaryIconToggleButton(
                    checked = favSlot != NOT_FAVORITED,
                    onCheckedChange = {
                        launchIO {
                            favoritesLock.mutate {
                                runSuspendCatching {
                                    modifyFavorites(galleryDetail.galleryInfo)
                                }.onSuccess { add ->
                                    if (add) {
                                        showSnackbar(addSucceed)
                                    } else {
                                        showSnackbar(removeSucceed)
                                    }
                                }.onFailure {
                                    // TODO: We don't know if it's add or remove
                                    showSnackbar(addFailed)
                                }
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = getFavoriteIcon(favSlot != NOT_FAVORITED),
                        contentDescription = null,
                    )
                }
                Text(text = favButtonText)
            }
            EhIconButton(
                icon = Icons.Default.Search,
                text = stringResource(id = R.string.similar_gallery),
                onClick = {
                    val keyword = EhUtils.extractTitle(galleryDetail.title)
                    val artistTag = galleryDetail.tags.getArtistTag()
                    if (null != keyword) {
                        navigate(
                            ListUrlBuilder(
                                mode = ListUrlBuilder.MODE_NORMAL,
                                mKeyword = "\"" + keyword + "\"",
                            ).asDst(),
                        )
                    } else if (artistTag != null) {
                        navigate(
                            ListUrlBuilder(
                                mode = ListUrlBuilder.MODE_TAG,
                                mKeyword = artistTag,
                            ).asDst(),
                        )
                    } else if (null != galleryDetail.uploader) {
                        navigate(
                            ListUrlBuilder(
                                mode = ListUrlBuilder.MODE_UPLOADER,
                                mKeyword = galleryDetail.uploader,
                            ).asDst(),
                        )
                    }
                },
            )
            EhIconButton(
                icon = Icons.Default.ImageSearch,
                text = stringResource(id = R.string.search_cover),
                onClick = {
                    val key = galleryDetail.thumbKey!!
                    navigator.navigate(
                        ListUrlBuilder(
                            mode = ListUrlBuilder.MODE_IMAGE_SEARCH,
                            hash = key.substringAfterLast('/').substringBefore('-'),
                        ).asDst(),
                    )
                },
            )
            val torrentText = stringResource(R.string.torrent_count, galleryDetail.torrentCount)
            val permissionDenied = stringResource(R.string.permission_denied)
            val downloadTorrentFailed = stringResource(R.string.download_torrent_failure)
            val downloadTorrentStarted = stringResource(R.string.download_torrent_started)
            val noTorrents = stringResource(R.string.no_torrents)
            val torrentResult = remember(galleryDetail) {
                async(Dispatchers.IO + Job(), CoroutineStart.LAZY) {
                    EhEngine.getTorrentList(galleryDetail.torrentUrl!!, gid, token)
                }
            }
            suspend fun showTorrentDialog() {
                val torrentList = bgWork { torrentResult.await() }
                if (torrentList.isEmpty()) {
                    showSnackbar(noTorrents)
                } else {
                    val selected = showNoButton(false) {
                        TorrentList(
                            items = torrentList,
                            onItemClick = { dismissWith(it) },
                        )
                    }
                    val url = selected.url
                    val name = "${selected.name}.torrent"
                    val r = DownloadManager.Request(url.toUri())
                    r.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        AppConfig.APP_DIRNAME + "/" + FileUtils.sanitizeFilename(name),
                    )
                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    EhCookieStore.getCookieHeader(url)?.let { r.addRequestHeader("Cookie", it) }
                    downloadManager.enqueue(r)
                    showSnackbar(downloadTorrentStarted)
                }
            }
            EhIconButton(
                icon = Icons.Default.SwapVerticalCircle,
                text = torrentText,
                onClick = {
                    launchIO {
                        if (galleryDetail.torrentCount > 0) {
                            val granted = isAtLeastQ || requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            if (granted) {
                                runSuspendCatching {
                                    showTorrentDialog()
                                }.onFailure {
                                    logcat(it)
                                    showSnackbar(downloadTorrentFailed)
                                }
                            } else {
                                showSnackbar(permissionDenied)
                            }
                        } else {
                            showSnackbar(noTorrents)
                        }
                    }
                },
            )
        }
        Spacer(modifier = Modifier.size(keylineMargin))
        fun getAllRatingText(rating: Float, ratingCount: Int): String = getString(
            R.string.rating_text,
            getString(getRatingText(rating)),
            rating,
            ratingCount,
        )
        var ratingText by rememberSaveable {
            mutableStateOf(getAllRatingText(galleryDetail.rating, galleryDetail.ratingCount))
        }
        val rateSucceed = stringResource(R.string.rate_successfully)
        val rateFailed = stringResource(R.string.rate_failed)
        fun showRateDialog() {
            launchIO {
                if (galleryDetail.apiUid < 0) {
                    showSnackbar(signInFirst)
                    return@launchIO
                }
                val pendingRating = awaitResult(galleryDetail.rating.coerceAtLeast(.5f), title = R.string.rate) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        var text by remember { mutableIntStateOf(getRatingText(expectedValue)) }
                        Text(text = stringResource(id = text), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.size(keylineMargin))
                        GalleryRatingBar(
                            rating = expectedValue,
                            onRatingChange = {
                                expectedValue = it.coerceAtLeast(.5f)
                                text = getRatingText(expectedValue)
                            },
                        )
                    }
                }
                galleryDetail.runSuspendCatching {
                    EhEngine.rateGallery(apiUid, apiKey, gid, token, pendingRating)
                }.onSuccess { result ->
                    galleryInfo = galleryDetail.apply {
                        rating = result.rating
                        ratingCount = result.ratingCount
                    }
                    ratingText = getAllRatingText(result.rating, result.ratingCount)
                    showSnackbar(rateSucceed)
                }.onFailure {
                    logcat(it)
                    showSnackbar(rateFailed)
                }
            }
        }
        CrystalCard(onClick = ::showRateDialog) {
            Column(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GalleryDetailRating(rating = galleryDetail.rating)
                Spacer(modifier = Modifier.size(keylineMargin))
                Text(text = ratingText)
            }
        }
        Spacer(modifier = Modifier.size(keylineMargin))
        val tags = galleryDetail.tags
        if (tags.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(id = R.string.no_tags))
            }
        } else {
            val copy = stringResource(android.R.string.copy)
            val copyTrans = stringResource(R.string.copy_trans)
            val showDefine = stringResource(R.string.show_definition)
            val addFilter = stringResource(R.string.add_filter)
            val filterAdded = stringResource(R.string.filter_added)
            val upTag = stringResource(R.string.tag_vote_up)
            val downTag = stringResource(R.string.tag_vote_down)
            GalleryTags(
                tagGroups = tags,
                onTagClick = {
                    val lub = ListUrlBuilder()
                    lub.mode = ListUrlBuilder.MODE_TAG
                    lub.keyword = it
                    navigate(lub.asDst())
                },
                onTagLongClick = { translated, tag ->
                    val index = tag.indexOf(':')
                    val temp = if (index >= 0) {
                        tag.substring(index + 1)
                    } else {
                        tag
                    }
                    launchIO {
                        awaitSelectAction {
                            onSelect(copy) {
                                addTextToClipboard(tag)
                            }
                            if (temp != translated) {
                                onSelect(copyTrans) {
                                    addTextToClipboard(translated)
                                }
                            }
                            onSelect(showDefine) {
                                openBrowser(EhUrl.getTagDefinitionUrl(temp))
                            }
                            onSelect(addFilter) {
                                awaitPermissionOrCancel { Text(text = stringResource(R.string.filter_the_tag, tag)) }
                                Filter(FilterMode.TAG, tag).remember()
                                showSnackbar(filterAdded)
                            }
                            if (galleryDetail.apiUid >= 0) {
                                onSelect(upTag) { galleryDetail.voteTag(tag, 1) }
                                onSelect(downTag) { galleryDetail.voteTag(tag, -1) }
                            }
                        }()
                    }
                },
            )
        }
        Spacer(modifier = Modifier.size(keylineMargin))
        if (Settings.showComments) {
            GalleryDetailComment(galleryDetail.comments.comments)
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.strip_item_padding_v)))
        }
    }

    @Composable
    fun GalleryDetailContent(
        galleryInfo: GalleryInfo,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        val galleryDetail = galleryInfo.asGalleryDetail()
        val windowSizeClass = LocalWindowSizeClass.current
        val thumbColumns by Settings.thumbColumns.collectAsState()
        val readText = stringResource(R.string.read)
        val startPage by rememberInVM {
            EhDB.getReadProgressFlow(galleryInfo.gid)
        }.collectAsState(0)
        val readButtonText = if (startPage == 0) {
            readText
        } else {
            stringResource(R.string.read_from, startPage + 1)
        }
        val downloadState by EhDownloadManager.collectDownloadState(gid)
        val downloadButtonText = when (downloadState) {
            DownloadInfo.STATE_INVALID -> stringResource(R.string.download)
            DownloadInfo.STATE_NONE -> stringResource(R.string.download_state_none)
            DownloadInfo.STATE_WAIT -> stringResource(R.string.download_state_wait)
            DownloadInfo.STATE_DOWNLOAD -> stringResource(R.string.download_state_downloading)
            DownloadInfo.STATE_FINISH -> stringResource(R.string.download_state_downloaded)
            DownloadInfo.STATE_FAILED -> stringResource(R.string.download_state_failed)
            else -> error("Invalid DownloadState!!!")
        }
        fun onReadButtonClick() {
            if (galleryDetail != null || downloadState != DownloadInfo.STATE_INVALID) {
                navToReader(galleryInfo.findBaseInfo(), startPage)
            }
        }
        fun onCategoryChipClick() {
            val category = galleryInfo.category
            if (category == EhUtils.NONE || category == EhUtils.PRIVATE || category == EhUtils.UNKNOWN) {
                return
            }
            val lub = ListUrlBuilder(category = category)
            navigator.navigate(lub.asDst())
        }
        fun onUploaderChipClick(galleryInfo: GalleryInfo) {
            val uploader = galleryInfo.uploader
            val disowned = uploader == "(Disowned)"
            if (uploader.isNullOrEmpty() || disowned) {
                return
            }
            val lub = ListUrlBuilder()
            lub.mode = ListUrlBuilder.MODE_UPLOADER
            lub.keyword = uploader
            navigator.navigate(lub.asDst())
        }

        var showBottomSheet by remember { mutableStateOf(false) }

        if (showBottomSheet && galleryDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                contentWindowInsets = { EmptyWindowInsets },
            ) {
                GalleryInfoBottomSheet(galleryDetail, navigator)
            }
        }

        fun onGalleryInfoCardClick() {
            showBottomSheet = true
        }

        val filterAdded = stringResource(R.string.filter_added)
        fun showFilterUploaderDialog(galleryInfo: GalleryInfo) {
            val uploader = galleryInfo.uploader
            val disowned = uploader == "(Disowned)"
            if (uploader.isNullOrEmpty() || disowned) {
                return
            }
            launchIO {
                awaitPermissionOrCancel {
                    Text(text = stringResource(R.string.filter_the_uploader, uploader))
                }
                Filter(FilterMode.UPLOADER, uploader).remember()
                showSnackbar(filterAdded)
            }
        }
        val onDownloadButtonClick = rememberLambda(galleryInfo) {
            galleryDetail ?: return@rememberLambda
            if (EhDownloadManager.getDownloadState(galleryDetail.gid) == DownloadInfo.STATE_INVALID) {
                launchUI { startDownload(implicit<MainActivity>(), false, galleryDetail.galleryInfo) }
            } else {
                launch { confirmRemoveDownload(galleryDetail) }
            }
        }

        when (windowSizeClass.windowWidthSizeClass) {
            WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.COMPACT -> LazyVerticalGrid(
                columns = GridCells.Fixed(thumbColumns),
                contentPadding = contentPadding,
                modifier = modifier.padding(horizontal = keylineMargin),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding_v)),
            ) {
                item(
                    key = "header",
                    span = { GridItemSpan(maxCurrentLineSpan) },
                    contentType = "header",
                ) {
                    GalleryDetailHeaderCard(
                        info = galleryInfo,
                        onInfoCardClick = ::onGalleryInfoCardClick,
                        onUploaderChipClick = ::onUploaderChipClick.partially1(galleryInfo),
                        onBlockUploaderIconClick = ::showFilterUploaderDialog.partially1(galleryInfo),
                        onCategoryChipClick = ::onCategoryChipClick,
                        modifier = Modifier.fillMaxWidth().padding(vertical = keylineMargin),
                    )
                }
                item(
                    key = "body",
                    span = { GridItemSpan(maxCurrentLineSpan) },
                    contentType = "body",
                ) {
                    LocalPinnableContainer.current!!.run { remember { pin() } }
                    Column {
                        Row {
                            FilledTonalButton(
                                onClick = onDownloadButtonClick,
                                modifier = Modifier.padding(horizontal = 4.dp).weight(1F),
                            ) {
                                Text(text = downloadButtonText, maxLines = 1)
                            }
                            Button(
                                onClick = ::onReadButtonClick,
                                modifier = Modifier.padding(horizontal = 4.dp).weight(1F),
                            ) {
                                Text(text = readButtonText, maxLines = 1)
                            }
                        }
                        if (getDetailError.isNotBlank()) {
                            GalleryDetailErrorTip(error = getDetailError, onClick = { getDetailError = "" })
                        } else if (galleryDetail != null) {
                            BelowHeader(galleryDetail)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(keylineMargin),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
                if (galleryDetail != null) {
                    galleryDetailPreview(galleryDetail)
                }
            }

            WindowWidthSizeClass.EXPANDED -> LazyVerticalGrid(
                columns = GridCells.Fixed(thumbColumns),
                contentPadding = contentPadding,
                modifier = modifier.padding(horizontal = keylineMargin),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding_v)),
            ) {
                item(
                    key = "header",
                    span = { GridItemSpan(maxCurrentLineSpan) },
                    contentType = "header",
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        GalleryDetailHeaderCard(
                            info = galleryInfo,
                            onInfoCardClick = ::onGalleryInfoCardClick,
                            onUploaderChipClick = ::onUploaderChipClick.partially1(galleryInfo),
                            onBlockUploaderIconClick = ::showFilterUploaderDialog.partially1(galleryInfo),
                            onCategoryChipClick = ::onCategoryChipClick,
                            modifier = Modifier.width(dimensionResource(id = R.dimen.gallery_detail_card_landscape_width)).padding(vertical = keylineMargin),
                        )
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = modifier.height(16.dp))
                            Button(
                                onClick = ::onReadButtonClick,
                                modifier = Modifier.height(56.dp).padding(horizontal = 16.dp).width(192.dp),
                            ) {
                                Text(text = readButtonText, maxLines = 1)
                            }
                            Spacer(modifier = modifier.height(24.dp))
                            FilledTonalButton(
                                onClick = onDownloadButtonClick,
                                modifier = Modifier.height(56.dp).padding(horizontal = 16.dp).width(192.dp),
                            ) {
                                Text(text = downloadButtonText, maxLines = 1)
                            }
                        }
                    }
                }
                item(
                    key = "body",
                    span = { GridItemSpan(maxCurrentLineSpan) },
                    contentType = "body",
                ) {
                    LocalPinnableContainer.current!!.run { remember { pin() } }
                    Column {
                        if (getDetailError.isNotBlank()) {
                            GalleryDetailErrorTip(error = getDetailError, onClick = { getDetailError = "" })
                        } else if (galleryDetail != null) {
                            BelowHeader(galleryDetail)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(keylineMargin),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
                if (galleryDetail != null) {
                    galleryDetailPreview(galleryDetail)
                }
            }
        }
    }

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
                    IconButton(onClick = ::showArchiveDialog) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                        )
                    }
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
                        val addTag = stringResource(R.string.action_add_tag)
                        val addTagTip = stringResource(R.string.action_add_tag_tip)
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.action_add_tag)) },
                            onClick = {
                                dropdown = false
                                val detail = galleryInfo as? GalleryDetail ?: return@DropdownMenuItem
                                launchIO {
                                    if (detail.apiUid < 0) {
                                        showSnackbar(signInFirst)
                                    } else {
                                        val text = awaitInputText(
                                            title = addTag,
                                            hint = addTagTip,
                                        )
                                        detail.voteTag(text.trim(), 1)
                                    }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.refresh)) },
                            onClick = {
                                dropdown = false
                                // Invalidate cache
                                galleryDetailCache.remove(gid)

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
                                    awaitPermissionOrCancel(
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
                                    val downloadInfo = EhDownloadManager.getDownloadInfo(gid)
                                    val canExport = downloadInfo?.state == DownloadInfo.STATE_FINISH
                                    if (!canExport) {
                                        awaitPermissionOrCancel(
                                            showCancelButton = false,
                                            text = { Text(text = stringResource(id = R.string.download_gallery_first)) },
                                        )
                                    } else {
                                        val info = galleryInfo!!
                                        val uri = awaitActivityResult(
                                            CreateDocument("application/vnd.comicbook+zip"),
                                            EhUtils.getSuitableTitle(info) + ".cbz",
                                        )
                                        val dirname = downloadInfo?.dirname
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
            GalleryDetailContent(
                galleryInfo = gi,
                contentPadding = it,
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
