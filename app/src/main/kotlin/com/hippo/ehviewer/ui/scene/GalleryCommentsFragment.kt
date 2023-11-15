/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui.scene

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.lerp
import androidx.core.text.inSpans
import androidx.core.text.parseAsHtml
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.jumpToReaderByPage
import com.hippo.ehviewer.ui.legacy.CoilImageGetter
import com.hippo.ehviewer.ui.main.GalleryCommentCard
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.scene.GalleryListScene.Companion.toStartArgs
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.NoopClipboardManager
import com.hippo.ehviewer.ui.tools.animateFloatMergePredictiveBackAsState
import com.hippo.ehviewer.ui.tools.normalizeSpan
import com.hippo.ehviewer.ui.tools.rememberBBCodeTextToolbar
import com.hippo.ehviewer.ui.tools.toAnnotatedString
import com.hippo.ehviewer.ui.tools.toBBCode
import com.hippo.ehviewer.ui.tools.updateSpan
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.TextUrl
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.getParcelableCompat
import com.ramcosta.composedestinations.annotation.Destination
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.replace
import moe.tarsin.coroutines.runSuspendCatching
import rikka.core.res.resolveColor

private fun Context.generateComment(
    textView: TextView,
    comment: GalleryComment,
): CharSequence {
    val sp = comment.comment.parseAsHtml(imageGetter = CoilImageGetter(textView))
    val ssb = SpannableStringBuilder(sp)
    if (0L != comment.id && 0 != comment.score) {
        val score = comment.score
        val scoreString = if (score > 0) "+$score" else score.toString()
        ssb.append("  ").inSpans(
            RelativeSizeSpan(0.8f),
            StyleSpan(Typeface.BOLD),
            ForegroundColorSpan(theme.resolveColor(android.R.attr.textColorSecondary)),
        ) {
            append(scoreString)
        }
    }
    if (comment.lastEdited != 0L) {
        val str = getString(
            R.string.last_edited,
            ReadableTime.getTimeAgo(comment.lastEdited),
        )
        ssb.append("\n\n").inSpans(
            RelativeSizeSpan(0.8f),
            StyleSpan(Typeface.BOLD),
            ForegroundColorSpan(theme.resolveColor(android.R.attr.textColorSecondary)),
        ) {
            append(str)
        }
    }
    return TextUrl.handleTextUrl(ssb)
}

private val MinimumContentPaddingEditText = 88.dp

@Destination
@Composable
fun GalleryCommentsScreen(galleryDetail: GalleryDetail, navigator: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val dialogState = LocalDialogState.current
    val coroutineScope = rememberCoroutineScope()

    val commentingBackField = rememberSaveable { mutableStateOf(false) }
    var commenting by commentingBackField
    val animationProgress by animateFloatMergePredictiveBackAsState(enable = commentingBackField)

    val userCommentBackField = remember { mutableStateOf(TextFieldValue()) }
    var userComment by userCommentBackField
    var commentId by remember { mutableLongStateOf(-1) }
    var comments by rememberSaveable { mutableStateOf(galleryDetail.comments) }
    LaunchedEffect(comments) {
        galleryDetail.comments = comments
    }
    var refreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
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

    suspend fun Context.sendComment() {
        commenting = false
        withUIContext { focusManager.clearFocus() }
        val url = EhUrl.getGalleryDetailUrl(galleryDetail.gid, galleryDetail.token, 0, false)
        runSuspendCatching {
            val bbcode = userComment.annotatedString.normalizeSpan().toBBCode()
            logcat { bbcode }
            EhEngine.commentGallery(url, bbcode, commentId)
        }.onSuccess {
            findActivity<MainActivity>().showTip(
                if (commentId != -1L) editCommentSuccess else commentSuccess,
                BaseScene.LENGTH_SHORT,
            )
            userComment = TextFieldValue()
            commentId = -1L
            comments = it
        }.onFailure {
            val text = if (commentId != -1L) editCommentFail else commentFail
            findActivity<MainActivity>().showTip(
                text + "\n" + ExceptionUtils.getReadableString(it),
                BaseScene.LENGTH_LONG,
            )
        }
    }

    suspend fun Context.showFilterCommenter(comment: GalleryComment) {
        val commenter = comment.user ?: return
        dialogState.awaitPermissionOrCancel { Text(text = stringResource(R.string.filter_the_commenter, commenter)) }
        Filter(FilterMode.COMMENTER, commenter).remember()
        comments = comments.copy(comments = comments.comments.filterNot { it.user == commenter })
        findActivity<MainActivity>().showTip(R.string.filter_added, BaseScene.LENGTH_SHORT)
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
        dialogState.showNoButton<Unit> {
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
            if (!commenting) {
                FloatingActionButton(
                    onClick = {
                        if (commentId != -1L) {
                            commentId = -1L
                            userComment = TextFieldValue()
                        }
                        commenting = true
                    },
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Default.Reply, contentDescription = null)
                }
            }
        },
    ) { paddingValues ->
        val keylineMargin = dimensionResource(id = R.dimen.keyline_margin)
        var editTextMeasured by remember { mutableStateOf(MinimumContentPaddingEditText) }
        val refreshState = rememberPullToRefreshState()
        if (refreshState.isRefreshing) {
            LaunchedEffect(Unit) {
                runSuspendCatching {
                    withIOContext { refreshComment(true) }
                }
                refreshState.endRefresh()
            }
        }
        Box(modifier = Modifier.fillMaxSize().imePadding().nestedScroll(refreshState.nestedScrollConnection)) {
            val additionalPadding = if (commenting) {
                editTextMeasured
            } else {
                if (!comments.hasMore) {
                    MinimumContentPaddingEditText
                } else {
                    0.dp
                }
            }
            LazyColumn(
                modifier = Modifier.padding(horizontal = keylineMargin),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + additionalPadding,
                ),
            ) {
                items(comments.comments) { item ->
                    suspend fun Context.voteComment(comment: GalleryComment, isUp: Boolean) {
                        galleryDetail.runSuspendCatching {
                            EhEngine.voteComment(apiUid, apiKey, gid, token, comment.id, if (isUp) 1 else -1)
                        }.onSuccess { result ->
                            findActivity<MainActivity>().showTip(
                                if (isUp) (if (0 != result.vote) R.string.vote_up_successfully else R.string.cancel_vote_up_successfully) else if (0 != result.vote) R.string.vote_down_successfully else R.string.cancel_vote_down_successfully,
                                BaseScene.LENGTH_SHORT,
                            )
                            val newComment = comment.copy(
                                score = result.score,
                                voteUpEd = 0 != result.vote && isUp,
                                voteDownEd = 0 != result.vote && !isUp,
                            )
                            val list = comments.comments.replace(comment, newComment)
                            comments = comments.copy(comments = list)
                        }.onFailure {
                            findActivity<MainActivity>().showTip(R.string.vote_failed, BaseScene.LENGTH_LONG)
                        }
                    }

                    suspend fun Context.doCommentAction(comment: GalleryComment) {
                        val actions = buildAction {
                            copyComment thenDo { findActivity<MainActivity>().addTextToClipboard(comment.comment.parseAsHtml()) }
                            if (!comment.uploader && !comment.editable) {
                                blockCommenter thenDo { showFilterCommenter(comment) }
                            }
                            if (comment.editable) {
                                editComment thenDo {
                                    userComment = TextFieldValue(comment.comment.parseAsHtml().toAnnotatedString())
                                    commentId = comment.id
                                    commenting = true
                                }
                            }
                            if (comment.voteUpAble) {
                                (if (comment.voteUpEd) cancelVoteUp else voteUp) thenDo { voteComment(comment, true) }
                            }
                            if (comment.voteDownAble) {
                                (if (comment.voteDownEd) cancelVoteDown else voteDown) thenDo { voteComment(comment, false) }
                            }
                            if (!comment.voteState.isNullOrEmpty()) {
                                checkVoteStatus thenDo { showCommentVoteStatus(comment) }
                            }
                        }
                        dialogState.showSelectItem(*actions.toTypedArray()).invoke()
                    }

                    GalleryCommentCard(
                        comment = item,
                        onUserClick = {
                            val lub = ListUrlBuilder(
                                mode = ListUrlBuilder.MODE_UPLOADER,
                                mKeyword = item.user,
                            )
                            navigator.navAnimated(R.id.galleryListScene, lub.toStartArgs(), true)
                        },
                        onCardClick = {
                            coroutineScope.launch {
                                context.doCommentAction(item)
                            }
                        },
                        onUrlClick = {
                            val activity = context.findActivity<MainActivity>()
                            if (!activity.jumpToReaderByPage(it, galleryDetail)) {
                                if (!navigator.navWithUrl(it)) {
                                    activity.openBrowser(it)
                                }
                            }
                        },
                    ) {
                        text = context.generateComment(this, item)
                    }
                }
                if (comments.hasMore) {
                    item {
                        AnimatedVisibility(refreshing) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center).padding(keylineMargin),
                                )
                            }
                        }
                        AnimatedVisibility(!refreshing) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launchIO {
                                        refreshing = true
                                        runSuspendCatching {
                                            refreshComment(true)
                                        }
                                        refreshing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(keylineMargin),
                            ) {
                                Text(text = stringResource(id = R.string.click_more_comments))
                            }
                        }
                    }
                }
            }
            PullToRefreshContainer(
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = paddingValues.calculateTopPadding()),
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).layout { measurable, constraints ->
                    val origin = measurable.measure(constraints)
                    val width = lerp(origin.width, 0, animationProgress)
                    val height = lerp(origin.height, 0, animationProgress)
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) {
                        placeable.placeRelative(0, 0)
                    }
                }.clip(RoundedCornerShape((animationProgress * 100).roundToInt())).height(IntrinsicSize.Min),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().onGloballyPositioned { coordinates ->
                        editTextMeasured = max(with(density) { coordinates.size.height.toDp() }, MinimumContentPaddingEditText)
                    },
                ) {
                    val color = MaterialTheme.colorScheme.onPrimaryContainer
                    val toolbar = rememberBBCodeTextToolbar(userCommentBackField)
                    CompositionLocalProvider(
                        LocalTextToolbar provides toolbar,
                        LocalClipboardManager provides NoopClipboardManager,
                    ) {
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
                        onClick = {
                            coroutineScope.launchIO {
                                context.sendComment()
                            }
                        },
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

class GalleryCommentsFragment : BaseScene() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeWithMD3 {
            val galleryDetail = remember { requireArguments().getParcelableCompat<GalleryDetail>(KEY_GALLERY_DETAIL)!! }
            val navController = remember { findNavController() }
            GalleryCommentsScreen(galleryDetail = galleryDetail, navigator = navController)
        }
    }

    companion object {
        const val KEY_GALLERY_DETAIL = "gallery_detail"
    }
}
