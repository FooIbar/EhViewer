package com.hippo.ehviewer.ui.screen

import android.Manifest
import android.app.Dialog
import android.app.DownloadManager
import android.content.DialogInterface
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.text.TextUtils.TruncateAt.END
import android.view.View
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.parseAsHtml
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import com.hippo.ehviewer.client.data.asGalleryDetail
import com.hippo.ehviewer.client.data.findBaseInfo
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.NoHAtHClientException
import com.hippo.ehviewer.client.getImageKey
import com.hippo.ehviewer.client.parser.ArchiveParser
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.client.parser.TorrentResult
import com.hippo.ehviewer.client.parser.format
import com.hippo.ehviewer.coil.justDownload
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.databinding.DialogArchiveListBinding
import com.hippo.ehviewer.databinding.DialogTorrentListBinding
import com.hippo.ehviewer.download.DownloadManager as EhDownloadManager
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.MODE_READ
import com.hippo.ehviewer.ui.GalleryInfoBottomSheet
import com.hippo.ehviewer.ui.LocalSnackbarHostState
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.confirmRemoveDownload
import com.hippo.ehviewer.ui.destinations.GalleryCommentsScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryDetailScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryListScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryPreviewScreenDestination
import com.hippo.ehviewer.ui.getFavoriteIcon
import com.hippo.ehviewer.ui.jumpToReaderByPage
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.CoilImageGetter
import com.hippo.ehviewer.ui.main.EhPreviewItem
import com.hippo.ehviewer.ui.main.GalleryCommentCard
import com.hippo.ehviewer.ui.main.GalleryDetailErrorTip
import com.hippo.ehviewer.ui.main.GalleryDetailHeaderCard
import com.hippo.ehviewer.ui.main.GalleryTags
import com.hippo.ehviewer.ui.modifyFavorites
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.startDownload
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.FilledTertiaryIconButton
import com.hippo.ehviewer.ui.tools.FilledTertiaryIconToggleButton
import com.hippo.ehviewer.ui.tools.GalleryDetailRating
import com.hippo.ehviewer.ui.tools.GalleryRatingBar
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.rememberLambda
import com.hippo.ehviewer.util.AppHelper
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.isAtLeastQ
import com.hippo.ehviewer.util.requestPermission
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.parcelize.Parcelize
import moe.tarsin.coroutines.runSuspendCatching
import splitties.systemservices.downloadManager
import splitties.systemservices.layoutInflater

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
private fun getRatingText(rating: Float): Int {
    return when ((rating * 2).roundToInt()) {
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
}

private fun List<GalleryTagGroup>.getArtist(): String? {
    for (tagGroup in this) {
        if ("artist" == tagGroup.groupName && tagGroup.size > 0) {
            return tagGroup[0].removePrefix("_")
        }
    }
    return null
}

@Destination
@Composable
fun GalleryDetailScreen(args: GalleryDetailScreenArgs, navigator: DestinationsNavigator) {
    LockDrawer(true)
    var galleryInfo by remember {
        val casted = args as? GalleryInfoArgs
        mutableStateOf<GalleryInfo?>(casted?.galleryInfo)
    }
    val (gid, token) = remember {
        when (args) {
            is GalleryInfoArgs -> args.galleryInfo.run { gid to token }
            is TokenArgs -> args.gid to args.token
        }
    }
    val galleryDetailUrl = remember { EhUrl.getGalleryDetailUrl(gid, token, 0, false) }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity<MainActivity>() }
    val snackbarState = LocalSnackbarHostState.current
    var showReadAction by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(args, galleryInfo) {
        if (showReadAction) {
            val page = (args as? TokenArgs)?.page ?: 0
            val gi = galleryInfo
            if (page != 0 && gi != null) {
                showReadAction = false
                val result = snackbarState.showSnackbar(
                    context.getString(R.string.read_from, page),
                    context.getString(R.string.read),
                    true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.navToReader(gi.findBaseInfo(), page)
                }
            }
        }
    }
    with(activity) {
        ProvideAssistContent(galleryDetailUrl)
    }
    var getDetailError by rememberSaveable { mutableStateOf("") }
    val dialogState = LocalDialogState.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()

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
                        coroutineScope.launchIO {
                            galleryDetail.previewList.forEach {
                                context.run { imageLoader.enqueue(imageRequest(it) { justDownload() }) }
                            }
                        }
                    }
                }.onFailure {
                    galleryInfo?.let { info -> EhDB.putHistoryInfo(info.findBaseInfo()) }
                    getDetailError = ExceptionUtils.getReadableString(it)
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
                activity.showTip(result)
            } else {
                activity.showTip(voteSuccess)
            }
        }.onFailure {
            activity.showTip(voteFailed)
        }
    }

    var mArchiveFormParamOr by remember { mutableStateOf<String?>(null) }
    var mArchiveList by remember { mutableStateOf<List<ArchiveParser.Archive>?>(null) }
    var mCurrentFunds by remember { mutableStateOf<HomeParser.Funds?>(null) }

    val archiveFree = stringResource(R.string.archive_free)
    val archiveOriginal = stringResource(R.string.archive_original)
    val archiveResample = stringResource(R.string.archive_resample)
    fun showArchiveDialog() {
        val galleryDetail = galleryInfo as? GalleryDetail ?: return
        if (galleryDetail.apiUid < 0) {
            activity.showTip(R.string.sign_in_first)
            return
        }
        class ArchiveListDialogHelper : DialogInterface.OnDismissListener {
            @Suppress("ktlint:standard:property-naming")
            private var _binding: DialogArchiveListBinding? = null
            private val binding get() = _binding!!
            private var mJob: Job? = null
            private var mDialog: Dialog? = null
            fun setDialog(dialog: Dialog?, dialogBinding: DialogArchiveListBinding, url: String?) {
                mDialog = dialog
                _binding = dialogBinding
                binding.listView.setOnItemClickListener { _, _, position, _ ->
                    val gi = galleryInfo ?: return@setOnItemClickListener
                    if (null != mArchiveList && position < mArchiveList!!.size) {
                        val res = mArchiveList!![position].res
                        val isHAtH = mArchiveList!![position].isHAtH
                        coroutineScope.launchIO {
                            runSuspendCatching {
                                EhEngine.downloadArchive(gid, token, mArchiveFormParamOr, res, isHAtH)
                            }.onSuccess { result ->
                                result?.let {
                                    val r = DownloadManager.Request(Uri.parse(result))
                                    val name = "$gid-" + EhUtils.getSuitableTitle(gi) + ".zip"
                                    r.setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS,
                                        FileUtils.sanitizeFilename(name),
                                    )
                                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    runCatching {
                                        downloadManager.enqueue(r)
                                    }.onFailure {
                                        it.printStackTrace()
                                    }
                                }
                                activity.showTip(R.string.download_archive_started)
                            }.onFailure {
                                when (it) {
                                    is NoHAtHClientException -> activity.showTip(R.string.download_archive_failure_no_hath)
                                    is EhException -> activity.showTip(ExceptionUtils.getReadableString(it))
                                    else -> activity.showTip(R.string.download_archive_failure)
                                }
                            }
                        }
                    }
                    mDialog?.dismiss()
                    mDialog = null
                }
                if (mArchiveList == null) {
                    binding.text.visibility = View.GONE
                    binding.listView.visibility = View.GONE
                    mJob = coroutineScope.launchIO {
                        runSuspendCatching {
                            EhEngine.getArchiveList(url!!, gid, token)
                        }.onSuccess { result ->
                            mArchiveFormParamOr = result.paramOr
                            mArchiveList = result.archiveList
                            mCurrentFunds = result.funds
                            withUIContext {
                                bind(result.archiveList, result.funds)
                            }
                        }.onFailure {
                            withUIContext {
                                binding.progress.visibility = View.GONE
                                binding.text.visibility = View.VISIBLE
                                binding.listView.visibility = View.GONE
                                binding.text.text = ExceptionUtils.getReadableString(it)
                            }
                        }
                        mJob = null
                    }
                } else {
                    bind(mArchiveList, mCurrentFunds)
                }
            }

            fun bind(data: List<ArchiveParser.Archive>?, funds: HomeParser.Funds?) {
                mDialog ?: return
                if (data.isNullOrEmpty()) {
                    binding.progress.visibility = View.GONE
                    binding.text.visibility = View.VISIBLE
                    binding.listView.visibility = View.GONE
                    binding.text.setText(R.string.no_archives)
                } else {
                    val nameArray = data.map {
                        it.run {
                            if (isHAtH) {
                                val costStr = if (cost == "Free") archiveFree else cost
                                "[H@H] $name [$size] [$costStr]"
                            } else {
                                val nameStr = if (res == "org") archiveOriginal else archiveResample
                                val costStr = if (cost == "Free!") archiveFree else cost
                                "$nameStr [$size] [$costStr]"
                            }
                        }
                    }.toTypedArray()
                    binding.progress.visibility = View.GONE
                    binding.text.visibility = View.GONE
                    binding.listView.visibility = View.VISIBLE
                    binding.listView.adapter = ArrayAdapter(mDialog!!.context, R.layout.item_select_dialog, nameArray)
                    if (funds != null) {
                        var fundsGP = funds.fundsGP.toString()
                        // Ex GP numbers are rounded down to the nearest thousand
                        if (EhUtils.isExHentai) {
                            fundsGP += "+"
                        }
                        mDialog!!.setTitle(context.resources.getString(R.string.current_funds, fundsGP, funds.fundsC))
                    }
                }
            }

            override fun onDismiss(dialog: DialogInterface) {
                mJob?.cancel()
                mJob = null
                mDialog = null
                _binding = null
            }
        }
        val helper = ArchiveListDialogHelper()
        val binding = DialogArchiveListBinding.inflate(context.layoutInflater)
        val dialog: Dialog = BaseDialogBuilder(context)
            .setTitle(R.string.settings_download)
            .setView(binding.root)
            .setOnDismissListener(helper)
            .show()
        helper.setDialog(dialog, binding, galleryDetail.archiveUrl)
    }

    val keylineMargin = dimensionResource(R.dimen.keyline_margin)

    fun navigateToPreview(nextPage: Boolean = false) {
        (galleryInfo as? GalleryDetail)?.let {
            navigator.navigate(GalleryPreviewScreenDestination(it, nextPage))
        }
    }

    fun LazyGridScope.galleryDetailPreview(gd: GalleryDetail) {
        val previewList = gd.previewList
        items(previewList) {
            EhPreviewItem(
                galleryPreview = it,
                position = it.position,
                onClick = { context.navToReader(gd.galleryInfo, it.position) },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
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
                        onUrlClick = {
                            if (!activity.jumpToReaderByPage(it, galleryDetail)) {
                                if (!navigator.navWithUrl(it)) {
                                    activity.openBrowser(it)
                                }
                            }
                        },
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
                context.getString(
                    R.string.newer_version_title,
                    it.title,
                    it.posted,
                ) to {
                    navigator.navigate(GalleryDetailScreenDestination(TokenArgs(it.gid, it.token!!)))
                }
            }.toTypedArray()
            val navAction = dialogState.showSelectItem(*items)
            withUIContext { navAction.invoke() }
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        if (galleryDetail.newerVersions.isNotEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                CrystalCard(
                    onClick = {
                        coroutineScope.launchIO {
                            showNewerVersionDialog()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                ) {
                }
                Text(text = stringResource(id = R.string.newer_version_available))
            }
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
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
            val favoritesLock = remember { Mutex() }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTertiaryIconToggleButton(
                    checked = favSlot != NOT_FAVORITED,
                    onCheckedChange = {
                        coroutineScope.launchIO {
                            favoritesLock.withLock {
                                var remove = false
                                runCatching {
                                    remove = !dialogState.modifyFavorites(galleryDetail.galleryInfo)
                                    if (remove) {
                                        activity.showTip(R.string.remove_from_favorite_success)
                                    } else {
                                        activity.showTip(R.string.add_to_favorite_success)
                                    }
                                }.onFailure {
                                    if (it !is CancellationException) {
                                        if (remove) {
                                            activity.showTip(R.string.remove_from_favorite_failure)
                                        } else {
                                            activity.showTip(R.string.add_to_favorite_failure)
                                        }
                                    }
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
                    val artist = galleryDetail.tags.getArtist()
                    if (null != keyword) {
                        navigator.navigate(
                            GalleryListScreenDestination(
                                ListUrlBuilder(
                                    mode = ListUrlBuilder.MODE_NORMAL,
                                    mKeyword = "\"" + keyword + "\"",
                                ),
                            ),
                        )
                    } else if (artist != null) {
                        navigator.navigate(
                            GalleryListScreenDestination(
                                ListUrlBuilder(
                                    mode = ListUrlBuilder.MODE_TAG,
                                    mKeyword = "artist:$artist",
                                ),
                            ),
                        )
                    } else if (null != galleryDetail.uploader) {
                        navigator.navigate(
                            GalleryListScreenDestination(
                                ListUrlBuilder(
                                    mode = ListUrlBuilder.MODE_UPLOADER,
                                    mKeyword = galleryDetail.uploader,
                                ),
                            ),
                        )
                    }
                },
            )
            EhIconButton(
                icon = Icons.Default.ImageSearch,
                text = stringResource(id = R.string.search_cover),
                onClick = {
                    val key = galleryDetail.thumbKey.orEmpty()
                    navigator.navigate(
                        GalleryListScreenDestination(
                            ListUrlBuilder(
                                mode = ListUrlBuilder.MODE_NORMAL,
                                hash = key.substringAfterLast('/').substringBefore('-'),
                            ),
                        ),
                    )
                },
            )
            val torrentText = stringResource(R.string.torrent_count, galleryDetail.torrentCount)
            val permissionDenied = stringResource(R.string.permission_denied)
            var mTorrentList by remember { mutableStateOf<TorrentResult?>(null) }
            EhIconButton(
                icon = Icons.Default.SwapVerticalCircle,
                text = torrentText,
                onClick = {
                    coroutineScope.launchIO {
                        val granted = isAtLeastQ || context.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        if (granted) {
                            class TorrentListDialogHelper : DialogInterface.OnDismissListener {
                                @Suppress("ktlint:standard:property-naming")
                                private var _binding: DialogTorrentListBinding? = null
                                private val binding get() = _binding!!
                                private var mJob: Job? = null
                                private var mDialog: Dialog? = null
                                fun setDialog(dialog: Dialog?, dialogBinding: DialogTorrentListBinding, url: String?) {
                                    mDialog = dialog
                                    _binding = dialogBinding
                                    binding.listView.setOnItemClickListener { _, _, position, _ ->
                                        if (null != mTorrentList && position < mTorrentList!!.size) {
                                            val itemUrl = mTorrentList!![position].url
                                            val name = mTorrentList!![position].name
                                            val r = DownloadManager.Request(Uri.parse(itemUrl.replace("exhentai.org", "ehtracker.org")))
                                            r.setDestinationInExternalPublicDir(
                                                Environment.DIRECTORY_DOWNLOADS,
                                                FileUtils.sanitizeFilename("$name.torrent"),
                                            )
                                            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            r.addRequestHeader("Cookie", EhCookieStore.getCookieHeader(itemUrl))
                                            try {
                                                downloadManager.enqueue(r)
                                                activity.showTip(R.string.download_torrent_started)
                                            } catch (e: Throwable) {
                                                e.printStackTrace()
                                                ExceptionUtils.throwIfFatal(e)
                                                activity.showTip(R.string.download_torrent_failure)
                                            }
                                        }
                                        mDialog?.dismiss()
                                        mDialog = null
                                    }
                                    if (mTorrentList == null) {
                                        binding.text.visibility = View.GONE
                                        binding.listView.visibility = View.GONE
                                        mJob = coroutineScope.launchIO {
                                            runSuspendCatching {
                                                EhEngine.getTorrentList(url!!, gid, token)
                                            }.onSuccess {
                                                mTorrentList = it
                                                withUIContext {
                                                    bind(it)
                                                }
                                            }.onFailure {
                                                withUIContext {
                                                    binding.progress.visibility = View.GONE
                                                    binding.text.visibility = View.VISIBLE
                                                    binding.listView.visibility = View.GONE
                                                    binding.text.text = ExceptionUtils.getReadableString(it)
                                                }
                                            }
                                            mJob = null
                                        }
                                    } else {
                                        bind(mTorrentList!!)
                                    }
                                }

                                private fun bind(data: TorrentResult) {
                                    mDialog ?: return
                                    if (data.isEmpty()) {
                                        binding.progress.visibility = View.GONE
                                        binding.text.visibility = View.VISIBLE
                                        binding.listView.visibility = View.GONE
                                        binding.text.setText(R.string.no_torrents)
                                    } else {
                                        val nameArray = data.map { it.format() }.toTypedArray()
                                        binding.progress.visibility = View.GONE
                                        binding.text.visibility = View.GONE
                                        binding.listView.visibility = View.VISIBLE
                                        binding.listView.adapter =
                                            ArrayAdapter(mDialog!!.context, R.layout.item_select_dialog, nameArray)
                                    }
                                }

                                override fun onDismiss(dialog: DialogInterface) {
                                    mJob?.cancel()
                                    mJob = null
                                    mDialog = null
                                    _binding = null
                                }
                            }
                            val helper = TorrentListDialogHelper()
                            withUIContext {
                                val binding = DialogTorrentListBinding.inflate(context.layoutInflater)
                                val dialog: Dialog = BaseDialogBuilder(context)
                                    .setTitle(R.string.torrents)
                                    .setView(binding.root)
                                    .setOnDismissListener(helper)
                                    .show()
                                helper.setDialog(dialog, binding, galleryDetail.torrentUrl)
                            }
                        } else {
                            activity.showTip(permissionDenied)
                        }
                    }
                },
            )
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        fun getAllRatingText(rating: Float, ratingCount: Int): String {
            return context.getString(
                R.string.rating_text,
                context.getString(getRatingText(rating)),
                rating,
                ratingCount,
            )
        }
        var ratingText by rememberSaveable {
            mutableStateOf(getAllRatingText(galleryDetail.rating, galleryDetail.ratingCount))
        }
        fun showRateDialog() {
            if (galleryDetail.apiUid < 0) {
                activity.showTip(R.string.sign_in_first)
                return
            }
            coroutineScope.launchIO {
                dialogState.awaitPermissionOrCancel(title = R.string.rate) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        var rating by remember { mutableFloatStateOf(galleryDetail.rating.coerceAtLeast(.5f)) }
                        var text by remember { mutableIntStateOf(getRatingText(rating)) }
                        Text(text = stringResource(id = text), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                        GalleryRatingBar(
                            rating = rating,
                            onRatingChange = {
                                rating = it.coerceAtLeast(.5f)
                                text = getRatingText(rating)
                            },
                        )
                    }
                }
                galleryDetail.runSuspendCatching {
                    EhEngine.rateGallery(apiUid, apiKey, gid, token, rating)
                }.onSuccess { result ->
                    activity.showTip(R.string.rate_successfully)
                    galleryInfo = galleryDetail.apply {
                        rating = result.rating
                        ratingCount = result.ratingCount
                    }
                    ratingText = getAllRatingText(result.rating, result.ratingCount)
                }.onFailure {
                    it.printStackTrace()
                    activity.showTip(R.string.rate_failed)
                }
            }
        }
        CrystalCard(onClick = ::showRateDialog) {
            Column(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GalleryDetailRating(rating = galleryDetail.rating)
                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                Text(text = ratingText)
            }
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
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
                    navigator.navigate(GalleryListScreenDestination(lub))
                },
                onTagLongClick = { translated, tag ->
                    val index = tag.indexOf(':')
                    val temp = if (index >= 0) {
                        tag.substring(index + 1)
                    } else {
                        tag
                    }
                    coroutineScope.launchIO {
                        dialogState.showSelectActions {
                            with(activity) {
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
                                    dialogState.awaitPermissionOrCancel { Text(text = stringResource(R.string.filter_the_tag, tag)) }
                                    Filter(FilterMode.TAG, tag).remember()
                                    showTip(filterAdded)
                                }
                                if (galleryDetail.apiUid >= 0) {
                                    onSelect(upTag) { galleryDetail.voteTag(tag, 1) }
                                    onSelect(downTag) { galleryDetail.voteTag(tag, -1) }
                                }
                            }
                        }
                    }
                },
            )
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
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
        val windowSizeClass = calculateWindowSizeClass(activity)
        val thumbColumns by Settings.thumbColumns.collectAsState()
        val readText = stringResource(R.string.read)
        var readButtonText by rememberSaveable { mutableStateOf(readText) }
        LifecycleResumeEffect(Unit) {
            coroutineScope.launchIO {
                runSuspendCatching {
                    val queen = SpiderQueen.obtainSpiderQueen(galleryInfo, MODE_READ)
                    val startPage = queen.awaitStartPage()
                    SpiderQueen.releaseSpiderQueen(queen, MODE_READ)
                    readButtonText = if (startPage == 0) {
                        readText
                    } else {
                        context.getString(R.string.read_from, startPage + 1)
                    }
                }
            }
            onPauseOrDispose { }
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
        fun onReadButtonClick() = context.navToReader(galleryInfo.findBaseInfo())
        fun onCategoryChipClick() {
            val category = galleryInfo.category
            if (category == EhUtils.NONE || category == EhUtils.PRIVATE || category == EhUtils.UNKNOWN) {
                return
            }
            val lub = ListUrlBuilder(category = category)
            navigator.navigate(GalleryListScreenDestination(lub))
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
            navigator.navigate(GalleryListScreenDestination(lub))
        }

        var showBottomSheet by remember { mutableStateOf(false) }

        if (showBottomSheet && galleryDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                GalleryInfoBottomSheet(galleryDetail, navigator)
            }
        }

        fun onGalleryInfoCardClick() {
            showBottomSheet = true
        }

        fun showFilterUploaderDialog(galleryInfo: GalleryInfo) {
            val uploader = galleryInfo.uploader
            val disowned = uploader == "(Disowned)"
            if (uploader.isNullOrEmpty() || disowned) {
                return
            }
            coroutineScope.launchIO {
                dialogState.awaitPermissionOrCancel {
                    Text(text = stringResource(R.string.filter_the_uploader, uploader))
                }
                Filter(FilterMode.UPLOADER, uploader).remember()
                activity.showTip(R.string.filter_added)
            }
        }
        val onDownloadButtonClick = rememberLambda(galleryInfo) {
            galleryDetail ?: return@rememberLambda
            if (EhDownloadManager.getDownloadState(galleryDetail.gid) == DownloadInfo.STATE_INVALID) {
                coroutineScope.launchUI {
                    dialogState.startDownload(activity, false, galleryDetail.galleryInfo)
                }
            } else {
                coroutineScope.launch {
                    dialogState.confirmRemoveDownload(galleryDetail)
                }
            }
        }

        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Medium, WindowWidthSizeClass.Compact -> LazyVerticalGrid(
                columns = GridCells.Fixed(thumbColumns),
                contentPadding = contentPadding,
                modifier = modifier.padding(horizontal = keylineMargin),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding_v)),
            ) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    LocalPinnableContainer.current!!.run { remember { pin() } }
                    Column {
                        GalleryDetailHeaderCard(
                            info = galleryInfo,
                            onInfoCardClick = ::onGalleryInfoCardClick,
                            onCategoryChipClick = ::onCategoryChipClick,
                            onUploaderChipClick = ::onUploaderChipClick.partially1(galleryInfo),
                            onBlockUploaderIconClick = ::showFilterUploaderDialog.partially1(galleryInfo),
                            modifier = Modifier.fillMaxWidth().padding(vertical = keylineMargin),
                        )
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
                                modifier = Modifier.fillMaxSize(),
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

            WindowWidthSizeClass.Expanded -> LazyVerticalGrid(
                columns = GridCells.Fixed(thumbColumns),
                contentPadding = contentPadding,
                modifier = modifier.padding(horizontal = keylineMargin),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding_v)),
            ) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    LocalPinnableContainer.current!!.run { remember { pin() } }
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            GalleryDetailHeaderCard(
                                info = galleryInfo,
                                onInfoCardClick = ::onGalleryInfoCardClick,
                                onCategoryChipClick = ::onCategoryChipClick,
                                onUploaderChipClick = ::onUploaderChipClick.partially1(galleryInfo),
                                onBlockUploaderIconClick = ::showFilterUploaderDialog.partially1(galleryInfo),
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
                        if (getDetailError.isNotBlank()) {
                            GalleryDetailErrorTip(error = getDetailError, onClick = { getDetailError = "" })
                        } else if (galleryDetail != null) {
                            BelowHeader(galleryDetail)
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
                            AppHelper.share(activity, galleryDetailUrl)
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
                                if (detail.apiUid < 0) {
                                    activity.showTip(R.string.sign_in_first)
                                } else {
                                    coroutineScope.launchIO {
                                        val text = dialogState.awaitInputText(
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
                                coroutineScope.launchIO {
                                    dialogState.awaitPermissionOrCancel(
                                        confirmText = R.string.clear_all,
                                        title = R.string.clear_image_cache,
                                    ) {
                                        Text(text = stringResource(id = R.string.clear_image_cache_confirm))
                                    }
                                    (0..<gd.pages).forEach {
                                        val key = getImageKey(gd.gid, it)
                                        imageCache.remove(key)
                                    }
                                    activity.showTip(imageCacheClear)
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.open_in_other_app)) },
                            onClick = {
                                dropdown = false
                                context.openBrowser(galleryDetailUrl)
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
