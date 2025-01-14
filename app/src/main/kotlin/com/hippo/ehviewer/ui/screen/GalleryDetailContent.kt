package com.hippo.ehviewer.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.window.core.layout.WindowWidthSizeClass
import arrow.core.partially1
import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.parZip
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.GalleryTagGroup
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.TagNamespace
import com.hippo.ehviewer.client.data.V2GalleryPreview
import com.hippo.ehviewer.client.data.VoteStatus
import com.hippo.ehviewer.client.data.asGalleryDetail
import com.hippo.ehviewer.client.data.findBaseInfo
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.NoHAtHClientException
import com.hippo.ehviewer.coil.PrefetchAround
import com.hippo.ehviewer.coil.justDownload
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.Magnet
import com.hippo.ehviewer.ktbuilder.executeIn
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ui.GalleryInfoBottomSheet
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.confirmRemoveDownload
import com.hippo.ehviewer.ui.destinations.GalleryCommentsScreenDestination
import com.hippo.ehviewer.ui.getFavoriteIcon
import com.hippo.ehviewer.ui.jumpToReaderByPage
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
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.EmptyWindowInsets
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalGrid
import com.hippo.ehviewer.ui.tools.FilledTertiaryIconButton
import com.hippo.ehviewer.ui.tools.FilledTertiaryIconToggleButton
import com.hippo.ehviewer.ui.tools.GalleryDetailRating
import com.hippo.ehviewer.ui.tools.GalleryRatingBar
import com.hippo.ehviewer.ui.tools.LocalWindowSizeClass
import com.hippo.ehviewer.ui.tools.TransitionsVisibilityScope
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import com.hippo.ehviewer.ui.tools.getClippedRefreshKey
import com.hippo.ehviewer.ui.tools.getLimit
import com.hippo.ehviewer.ui.tools.getOffset
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.displayString
import com.hippo.ehviewer.util.flattenForEach
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.http.encodeURLParameter
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import moe.tarsin.coroutines.runSwallowingWithUI

context(CoroutineScope, DestinationsNavigator, DialogState, MainActivity, SnackbarHostState, SharedTransitionScope, TransitionsVisibilityScope)
@Composable
fun GalleryDetailContent(
    galleryInfo: GalleryInfo,
    contentPadding: PaddingValues,
    getDetailError: String,
    onRetry: () -> Unit,
    voteTag: VoteTag,
    modifier: Modifier,
) {
    val keylineMargin = dimensionResource(R.dimen.keyline_margin)
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
    val downloadState by DownloadManager.collectDownloadState(galleryInfo.gid)
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
        navigate(ListUrlBuilder(category = category).asDst())
    }
    fun onUploaderChipClick(galleryInfo: GalleryInfo) {
        val uploader = galleryInfo.uploader
        val disowned = uploader == "(Disowned)"
        if (uploader.isNullOrEmpty() || disowned) {
            return
        }
        navigate(ListUrlBuilder(mode = ListUrlBuilder.MODE_UPLOADER, mKeyword = uploader).asDst())
    }

    fun onGalleryInfoCardClick() {
        galleryDetail ?: return
        launch {
            dialog { cont ->
                ModalBottomSheet(
                    onDismissRequest = { cont.cancel() },
                    contentWindowInsets = { EmptyWindowInsets },
                ) {
                    GalleryInfoBottomSheet(galleryDetail)
                }
            }
        }
    }

    val filterAdded = stringResource(R.string.filter_added)
    fun showFilterUploaderDialog(galleryInfo: GalleryInfo) {
        val uploader = galleryInfo.uploader
        val disowned = uploader == "(Disowned)"
        if (uploader.isNullOrEmpty() || disowned) {
            return
        }
        launchIO {
            awaitConfirmationOrCancel {
                Text(text = stringResource(R.string.filter_the_uploader, uploader))
            }
            Filter(FilterMode.UPLOADER, uploader).remember()
            showSnackbar(filterAdded)
        }
    }
    fun onDownloadButtonClick() {
        galleryDetail ?: return
        if (DownloadManager.getDownloadState(galleryDetail.gid) == DownloadInfo.STATE_INVALID) {
            launchUI { startDownload(implicit<MainActivity>(), false, galleryDetail.galleryInfo) }
        } else {
            launch { confirmRemoveDownload(galleryDetail) }
        }
    }

    val previews = galleryDetail?.collectPreviewItems()
    when (windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.COMPACT -> FastScrollLazyVerticalGrid(
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
                            onClick = ::onDownloadButtonClick,
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
                        GalleryDetailErrorTip(error = getDetailError, onClick = onRetry)
                    } else if (galleryDetail != null) {
                        BelowHeader(galleryDetail, voteTag)
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
            if (galleryDetail != null && previews != null) {
                galleryPreview(galleryDetail, previews) { navToReader(galleryDetail.galleryInfo, it) }
            }
        }

        WindowWidthSizeClass.EXPANDED -> FastScrollLazyVerticalGrid(
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
                            onClick = ::onDownloadButtonClick,
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
                        GalleryDetailErrorTip(error = getDetailError, onClick = onRetry)
                    } else if (galleryDetail != null) {
                        BelowHeader(galleryDetail, voteTag)
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
            if (galleryDetail != null && previews != null) {
                galleryPreview(galleryDetail, previews) { navToReader(galleryDetail.galleryInfo, it) }
            }
        }
    }
}

context(Context, CoroutineScope, DestinationsNavigator, DialogState, SnackbarHostState)
@Composable
fun BelowHeader(galleryDetail: GalleryDetail, voteTag: VoteTag) {
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
        fun navigateToCommentScreen() {
            navigate(GalleryCommentsScreenDestination(galleryDetail.gid))
        }
        CrystalCard {
            commentsList.take(maxShowCount).forEach { item ->
                GalleryCommentCard(
                    modifier = Modifier.padding(vertical = 4.dp),
                    comment = item,
                    onCardClick = ::navigateToCommentScreen,
                    onUserClick = ::navigateToCommentScreen,
                    onUrlClick = {
                        if (it.startsWith("#c")) {
                            navigateToCommentScreen()
                        } else {
                            if (!jumpToReaderByPage(it, galleryDetail)) if (!navWithUrl(it)) openBrowser(it)
                        }
                    },
                    maxLines = 5,
                    ellipsis = true,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(id = R.dimen.strip_item_padding_v))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = ::navigateToCommentScreen),
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
            navigate(info.gid asDstWith info.token)
        }
    }
    val keylineMargin = dimensionResource(R.dimen.keyline_margin)
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
                val artistTag = galleryDetail.tagGroups.artistTag()
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
        val signInFirst = stringResource(R.string.sign_in_first)
        val noArchive = stringResource(R.string.no_archives)
        val downloadStarted = stringResource(R.string.download_archive_started)
        val downloadFailed = stringResource(R.string.download_archive_failure)
        val failureNoHath = stringResource(R.string.download_archive_failure_no_hath)
        val archiveResult = remember(galleryDetail) {
            async(Dispatchers.IO + Job(), CoroutineStart.LAZY) {
                with(galleryDetail) {
                    EhEngine.getArchiveList(archiveUrl!!, gid, token)
                }
            }
        }
        fun showArchiveDialog() {
            launchIO {
                if (galleryDetail.apiUid < 0) {
                    showSnackbar(signInFirst)
                } else {
                    runSuspendCatching {
                        val (archiveList, funds) = bgWork { archiveResult.await() }
                        if (archiveList.isEmpty()) {
                            showSnackbar(noArchive)
                        } else {
                            val selected = showNoButton {
                                ArchiveList(
                                    funds = funds,
                                    items = archiveList,
                                    onItemClick = { resume(it) },
                                )
                            }
                            EhUtils.downloadArchive(galleryDetail, selected)
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
        EhIconButton(
            icon = Icons.Default.FolderZip,
            text = stringResource(id = R.string.archive),
            onClick = ::showArchiveDialog,
        )
        val torrentText = stringResource(R.string.torrent_count, galleryDetail.torrentCount)
        val noTorrents = stringResource(R.string.no_torrents)
        val torrentResult = remember(galleryDetail) {
            async(Dispatchers.IO + Job(), CoroutineStart.LAZY) {
                parZip(
                    { EhEngine.getTorrentList(galleryDetail.torrentUrl!!, galleryDetail.gid, galleryDetail.token) },
                    { EhEngine.getTorrentKey() },
                    { list, key -> list to key },
                )
            }
        }
        suspend fun showTorrentDialog() {
            val (torrentList, key) = bgWork { torrentResult.await() }
            if (torrentList.isEmpty()) {
                showSnackbar(noTorrents)
            } else {
                val selected = showNoButton(false) {
                    TorrentList(
                        items = torrentList,
                        onItemClick = { resume(it) },
                    )
                }
                val hash = selected.url.dropLast(8).takeLast(40)
                val name = selected.name.encodeURLParameter()
                val tracker = EhUrl.getTrackerUrl(galleryDetail.gid, key).encodeURLParameter()
                val link = "magnet:?xt=urn:btih:$hash&dn=$name&tr=$tracker"
                val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                try {
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    withUIContext { addTextToClipboard(link, true) }
                }
            }
        }
        EhIconButton(
            icon = EhIcons.Default.Magnet,
            text = torrentText,
            onClick = {
                launchIO {
                    when {
                        galleryDetail.torrentCount <= 0 -> showSnackbar(noTorrents)
                        else -> runSwallowingWithUI { showTorrentDialog() }
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
    val signInFirst = stringResource(R.string.sign_in_first)
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
                galleryDetail.apply {
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
    val tags = galleryDetail.tagGroups
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
        val withDraw = stringResource(R.string.tag_vote_withdraw)
        GalleryTags(
            tagGroups = tags,
            onTagClick = {
                navigate(ListUrlBuilder(mode = ListUrlBuilder.MODE_TAG, mKeyword = it).asDst())
            },
            onTagLongClick = { tag, translation, vote ->
                val rawValue = tag.substringAfter(':')
                launchIO {
                    awaitSelectAction {
                        onSelect(copy) {
                            addTextToClipboard(tag)
                        }
                        if (rawValue != translation) {
                            onSelect(copyTrans) {
                                addTextToClipboard(translation)
                            }
                        }
                        onSelect(showDefine) {
                            openBrowser(EhUrl.getTagDefinitionUrl(rawValue))
                        }
                        onSelect(addFilter) {
                            awaitConfirmationOrCancel { Text(text = stringResource(R.string.filter_the_tag, tag)) }
                            Filter(FilterMode.TAG, tag).remember()
                            showSnackbar(filterAdded)
                        }
                        if (galleryDetail.apiUid >= 0) {
                            when (vote) {
                                VoteStatus.NONE -> {
                                    onSelect(upTag) { galleryDetail.voteTag(tag, 1) }
                                    onSelect(downTag) { galleryDetail.voteTag(tag, -1) }
                                }
                                VoteStatus.UP -> onSelect(withDraw) { galleryDetail.voteTag(tag, -1) }
                                VoteStatus.DOWN -> onSelect(withDraw) { galleryDetail.voteTag(tag, 1) }
                            }
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

private fun List<GalleryTagGroup>.artistTag() = find { (ns, _) -> ns == TagNamespace.Artist || ns == TagNamespace.Cosplayer }?.let { (ns, tags) -> "$ns:${tags[0].text}" }

context(Context)
@Composable
private fun GalleryDetail.collectPreviewItems() = rememberInVM(previewList) {
    val pageSize = previewList.size
    val pages = pages
    val previewPagesMap = previewList.associateBy { it.position } as MutableMap
    Pager(
        PagingConfig(
            pageSize = pageSize,
            prefetchDistance = pageSize.coerceAtMost(100),
            initialLoadSize = pageSize,
            jumpThreshold = 2 * pageSize,
        ),
    ) {
        object : PagingSource<Int, GalleryPreview>() {
            override fun getRefreshKey(state: PagingState<Int, GalleryPreview>) = state.getClippedRefreshKey()
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryPreview> = withIOContext {
                val key = params.key ?: 0
                val up = getOffset(params, key, pages)
                val end = (up + getLimit(params, key) - 1).coerceAtMost(pages - 1)
                runSuspendCatching {
                    (up..end).filterNot { it in previewPagesMap }.map { it / pageSize }.toSet()
                        .parMap(concurrency = Settings.multiThreadDownload) { page ->
                            val url = EhUrl.getGalleryDetailUrl(gid, token, page, false)
                            EhEngine.getPreviewList(url).first
                        }.flattenForEach {
                            previewPagesMap[it.position] = it
                            if (Settings.preloadThumbAggressively) {
                                imageRequest(it) { justDownload() }.executeIn(viewModelScope)
                            }
                        }
                }.foldToLoadResult {
                    val r = (up..end).map { requireNotNull(previewPagesMap[it]) }
                    val prevK = if (up <= 0 || r.isEmpty()) null else up
                    val nextK = if (end == pages - 1) null else end + 1
                    LoadResult.Page(r, prevK, nextK, up, pages - end - 1)
                }
            }
            override val jumpingSupported = true
        }
    }.flow.cachedIn(viewModelScope)
}.collectAsLazyPagingItems()

context(Context)
private fun LazyGridScope.galleryPreview(detail: GalleryDetail, data: LazyPagingItems<GalleryPreview>, onClick: (Int) -> Unit) {
    val isV2Thumb = detail.previewList.first() is V2GalleryPreview
    items(
        count = data.itemCount,
        key = data.itemKey(key = { item -> item.position }),
        contentType = { "preview" },
    ) { index ->
        val item = data[index]
        EhPreviewItem(item, index) { onClick(index) }
        PrefetchAround(data, index, if (isV2Thumb) 20 else 6, ::imageRequest)
    }
}
