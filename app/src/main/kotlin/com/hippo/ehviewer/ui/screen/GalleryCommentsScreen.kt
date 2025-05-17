package com.hippo.ehviewer.ui.screen

import android.graphics.Typeface
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.annotation.ColorInt
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.lerp
import androidx.core.text.buildSpannedString
import androidx.core.text.getSpans
import androidx.core.text.inSpans
import androidx.core.text.parseAsHtml
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.jumpToReaderByPage
import com.hippo.ehviewer.ui.main.GalleryCommentCard
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.tools.animateFloatMergePredictiveBackAsState
import com.hippo.ehviewer.ui.tools.normalizeSpan
import com.hippo.ehviewer.ui.tools.rememberBBCodeTextToolbar
import com.hippo.ehviewer.ui.tools.snackBarPadding
import com.hippo.ehviewer.ui.tools.thenIf
import com.hippo.ehviewer.ui.tools.toBBCode
import com.hippo.ehviewer.ui.tools.updateSpan
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.displayString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import kotlin.collections.forEach
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

private val URL_PATTERN = Regex("(http|https)://[a-z0-9A-Z%-]+(\\.[a-z0-9A-Z%-]+)+(:\\d{1,5})?(/[a-zA-Z0-9-_~:#@!&',;=%/*.?+$\\[\\]()]+)?/?")

private inline fun SpannableStringBuilder.withSpans(
    @ColorInt color: Int,
    builderAction: SpannableStringBuilder.() -> Unit,
) = inSpans(
    RelativeSizeSpan(0.8f),
    StyleSpan(Typeface.BOLD),
    ForegroundColorSpan(color),
    builderAction = builderAction,
)

@Composable
fun processComment(
    comment: GalleryComment,
    imageGetter: Html.ImageGetter,
) = comment.comment.parseAsHtml(imageGetter = imageGetter).let { text ->
    buildSpannedString {
        append(text)
        URL_PATTERN.findAll(text).forEach { result ->
            val start = result.range.first
            val end = result.range.last + 1
            if (getSpans<URLSpan>(start, end).isEmpty()) {
                setSpan(URLSpan(result.groupValues[0]), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        val color = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
        if (comment.id != 0L && comment.score != 0) {
            val score = comment.score
            val scoreString = if (score > 0) "+$score" else score.toString()
            append("  ")
            withSpans(color) {
                append(scoreString)
            }
        }
        if (comment.lastEdited != 0L) {
            append("\n\n")
            withSpans(color) {
                append(
                    stringResource(
                        R.string.last_edited,
                        ReadableTime.getTimeAgo(comment.lastEdited),
                    ),
                )
            }
        }
    }
}

private val MinimumContentPaddingEditText = 88.dp

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.GalleryCommentsScreen(gid: Long, navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var commenting by rememberSaveable { mutableStateOf(false) }
    val animationProgress by animateFloatMergePredictiveBackAsState(enable = commenting) { commenting = false }
    val animateItems by Settings.animateItems.collectAsState()

    val galleryDetail = remember { detailCache[gid]!! }
    val userCommentBackField = remember { mutableStateOf(TextFieldValue()) }
    var userComment by userCommentBackField
    var commentId by remember { mutableLongStateOf(-1) }
    var comments by rememberSaveable { mutableStateOf(galleryDetail.comments) }
    LaunchedEffect(comments) {
        galleryDetail.comments = comments
    }
    var refreshing by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    suspend fun refreshComment(showAll: Boolean) {
        val url = EhUrl.getGalleryDetailUrl(galleryDetail.gid, galleryDetail.token, 0, showAll)
        val detail = EhEngine.getGalleryDetail(url)
        comments = detail.comments
    }

    val copyComment = stringResource(R.string.copy_comment_text)
    val blockCommenter = stringResource(R.string.block_commenter)
    val editComment = stringResource(R.string.edit_comment)
    val cancelVoteUp = stringResource(R.string.cancel_vote_up)
    val cancelVoteDown = stringResource(R.string.cancel_vote_down)
    val voteUp = stringResource(R.string.vote_up)
    val voteDown = stringResource(R.string.vote_down)
    val checkVoteStatus = stringResource(R.string.check_vote_status)
    val editCommentSuccess = stringResource(R.string.edit_comment_successfully)
    val commentSuccess = stringResource(R.string.comment_successfully)
    val editCommentFail = stringResource(R.string.edit_comment_failed)
    val commentFail = stringResource(R.string.comment_failed)

    val focusManager = LocalFocusManager.current

    suspend fun sendComment() {
        commenting = false
        withUIContext { focusManager.clearFocus() }
        val url = EhUrl.getGalleryDetailUrl(galleryDetail.gid, galleryDetail.token, 0, false)
        userComment.runSuspendCatching {
            val bbcode = annotatedString.normalizeSpan().toBBCode()
            logcat("sendComment") { bbcode }
            EhEngine.commentGallery(url, bbcode, commentId)
        }.onSuccess {
            val msg = if (commentId != -1L) editCommentSuccess else commentSuccess
            userComment = TextFieldValue()
            commentId = -1L
            comments = it
            showSnackbar(msg)
        }.onFailure {
            val text = if (commentId != -1L) editCommentFail else commentFail
            showSnackbar(text + "\n" + it.displayString())
        }
    }

    val filterAdded = stringResource(R.string.filter_added)
    suspend fun showFilterCommenter(comment: GalleryComment) {
        val commenter = comment.user ?: return
        awaitConfirmationOrCancel { Text(text = stringResource(R.string.filter_the_commenter, commenter)) }
        Filter(FilterMode.COMMENTER, commenter).remember()
        comments = comments.copy(comments = comments.comments.filterNot { it.user == commenter })
        showSnackbar(filterAdded)
    }

    suspend fun showCommentVoteStatus(comment: GalleryComment) {
        val statusStr = comment.voteState ?: return
        val data = statusStr.split(',').map {
            val str = it.trim()
            val index = str.lastIndexOf(' ')
            if (index < 0) {
                str to ""
            } else {
                str.substring(0, index).trim() to str.substring(index + 1).trim()
            }
        }
        // Wait cancellation
        showNoButton<Unit> {
            Column {
                data.forEach { (name, vote) ->
                    ListItem(
                        headlineContent = {
                            Text(text = name)
                        },
                        trailingContent = {
                            Text(text = vote)
                        },
                    )
                }
            }
        }
    }
    val hasSignedIn by Settings.hasSignedIn.collectAsState()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.gallery_comments)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (hasSignedIn && !commenting) {
                FloatingActionButton(
                    onClick = {
                        if (commentId != -1L) {
                            commentId = -1L
                            userComment = TextFieldValue()
                        }
                        commenting = true
                    },
                    modifier = Modifier.snackBarPadding(),
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Default.Reply, contentDescription = null)
                }
            }
        },
    ) { paddingValues ->
        val keylineMargin = dimensionResource(id = R.dimen.keyline_margin)
        var editTextMeasured by remember { mutableStateOf(MinimumContentPaddingEditText) }
        var isRefreshing by remember { mutableStateOf(false) }
        val refreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                launchIO {
                    runSuspendCatching {
                        refreshComment(true)
                    }
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().imePadding().padding(top = paddingValues.calculateTopPadding()),
            state = refreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = isRefreshing,
                    state = refreshState,
                )
            },
        ) {
            val additionalPadding = if (commenting) {
                editTextMeasured
            } else {
                if (!comments.hasMore) {
                    MinimumContentPaddingEditText
                } else {
                    0.dp
                }
            }
            val voteUpSucceed = stringResource(R.string.vote_up_successfully)
            val cancelVoteUpSucceed = stringResource(R.string.cancel_vote_up_successfully)
            val voteDownSucceed = stringResource(R.string.vote_down_successfully)
            val cancelVoteDownSucceed = stringResource(R.string.cancel_vote_down_successfully)
            val voteFailed = stringResource(R.string.vote_failed)
            val layoutDirection = LocalLayoutDirection.current
            val lazyListState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.padding(horizontal = keylineMargin),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    bottom = paddingValues.calculateBottomPadding() + additionalPadding,
                ),
            ) {
                items(
                    items = comments.comments,
                    key = { it.id },
                ) { item ->
                    suspend fun voteComment(comment: GalleryComment, isUp: Boolean) {
                        galleryDetail.runSuspendCatching {
                            EhEngine.voteComment(apiUid, apiKey, gid, token, comment.id, if (isUp) 1 else -1).also {
                                refreshComment(true)
                            }
                        }.onSuccess { result ->
                            showSnackbar(
                                if (isUp) {
                                    if (0 != result.vote) voteUpSucceed else cancelVoteUpSucceed
                                } else {
                                    if (0 != result.vote) voteDownSucceed else cancelVoteDownSucceed
                                },
                            )
                        }.onFailure {
                            showSnackbar(voteFailed)
                        }
                    }

                    suspend fun doCommentAction(comment: GalleryComment) = awaitSelectAction {
                        onSelect(copyComment) {
                            addTextToClipboard(comment.comment.parseAsHtml())
                        }
                        if (!comment.uploader && !comment.editable) {
                            onSelect(blockCommenter) { showFilterCommenter(comment) }
                        }
                        if (comment.editable) {
                            onSelect(editComment) {
                                userComment = TextFieldValue(AnnotatedString.fromHtml(comment.comment))
                                commentId = comment.id
                                commenting = true
                            }
                        }
                        if (comment.voteUpAble) {
                            onSelect(if (comment.voteUpEd) cancelVoteUp else voteUp) {
                                voteComment(comment, true)
                            }
                        }
                        if (comment.voteDownAble) {
                            onSelect(if (comment.voteDownEd) cancelVoteDown else voteDown) {
                                voteComment(comment, false)
                            }
                        }
                        if (!comment.voteState.isNullOrEmpty()) {
                            onSelect(checkVoteStatus) {
                                showCommentVoteStatus(comment)
                            }
                        }
                    }()

                    GalleryCommentCard(
                        modifier = Modifier.thenIf(animateItems) { animateItem() },
                        comment = item,
                        onUserClick = {
                            navigate(
                                ListUrlBuilder(
                                    mode = ListUrlBuilder.MODE_UPLOADER,
                                    mKeyword = item.user,
                                ).asDst(),
                            )
                        },
                        onCardClick = { launch { doCommentAction(item) } },
                        onUrlClick = {
                            if (it.startsWith("#c")) {
                                it.substring(2).toLongOrNull()?.let { id ->
                                    val index = comments.comments.indexOfFirst { c -> c.id == id }
                                    if (index != -1) {
                                        launch { lazyListState.animateScrollToItem(index) }
                                    }
                                }
                            } else {
                                if (!jumpToReaderByPage(it, galleryDetail)) if (!navWithUrl(it)) openBrowser(it)
                            }
                        },
                        processComment = { c, ig -> processComment(c, ig) },
                    )
                }
                if (comments.hasMore) {
                    item {
                        Crossfade(targetState = refreshing, modifier = Modifier.padding(keylineMargin), label = "refreshing") {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (it) {
                                    CircularWavyProgressIndicator()
                                } else {
                                    TextButton(
                                        onClick = {
                                            launchIO {
                                                refreshing = true
                                                runSuspendCatching { refreshComment(true) }
                                                refreshing = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(text = stringResource(id = R.string.click_more_comments))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).layout { measurable, constraints ->
                    val origin = measurable.measure(constraints)
                    val width = lerp(origin.width, 0, animationProgress)
                    val height = lerp(origin.height, 0, animationProgress)
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) {
                        placeable.placeRelative(0, 0)
                    }
                }.graphicsLayer {
                    shape = RoundedCornerShape((animationProgress * 100).roundToInt())
                    clip = true
                }.height(IntrinsicSize.Min),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().onGloballyPositioned { coordinates ->
                        editTextMeasured = max(with(density) { coordinates.size.height.toDp() }, MinimumContentPaddingEditText)
                    },
                ) {
                    val color = MaterialTheme.colorScheme.onPrimaryContainer
                    val toolbar = rememberBBCodeTextToolbar(userCommentBackField)
                    CompositionLocalProvider(LocalTextToolbar provides toolbar) {
                        BasicTextField(
                            value = userComment,
                            onValueChange = { textFieldValue ->
                                userComment = textFieldValue.updateSpan(userComment)
                            },
                            modifier = Modifier.weight(1f).padding(keylineMargin),
                            textStyle = MaterialTheme.typography.bodyLarge.merge(color = color),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        )
                    }
                    IconButton(
                        onClick = { launchIO { sendComment() } },
                        modifier = Modifier.align(Alignment.CenterVertically).padding(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
