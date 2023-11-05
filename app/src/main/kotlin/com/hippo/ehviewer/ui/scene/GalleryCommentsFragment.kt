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
import android.content.DialogInterface
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text2.BasicTextField2
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.text.getSpans
import androidx.core.text.inSpans
import androidx.core.text.parseAsHtml
import androidx.core.text.set
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.GalleryCommentList
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.databinding.SceneGalleryCommentsBinding
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.jumpToReaderByPage
import com.hippo.ehviewer.ui.legacy.CoilImageGetter
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.legacy.WindowInsetsAnimationHelper
import com.hippo.ehviewer.ui.main.GalleryCommentCard
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.scene.GalleryListScene.Companion.toStartArgs
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.TextUrl
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.applyNavigationBarsPadding
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.getParcelableCompat
import com.hippo.ehviewer.util.toBBCode
import com.ramcosta.composedestinations.annotation.Destination
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import rikka.core.res.resolveColor

interface ActionScope {
    infix fun String.thenDo(that: suspend () -> Unit)
}

private inline fun buildAction(builder: ActionScope.() -> Unit) = buildList {
    builder(object : ActionScope {
        override fun String.thenDo(that: suspend () -> Unit) {
            add(this to that)
        }
    })
}

private fun Context.generateComment(
    textView: TextView,
    comment: GalleryComment,
): CharSequence {
    val sp = comment.comment.orEmpty().parseAsHtml(imageGetter = CoilImageGetter(textView))
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

@Destination
@Composable
fun GalleryCommentsScreen(galleryDetail: GalleryDetail, navigator: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val dialogState = LocalDialogState.current
    val coroutineScope = rememberCoroutineScope()
    var commenting by rememberSaveable { mutableStateOf(false) }
    var userComment by rememberSaveable { mutableStateOf("") }
    var comments by rememberSaveable { mutableStateOf(galleryDetail.comments) }
    var refreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    suspend fun refreshComment(showAll: Boolean) {
        val url = EhUrl.getGalleryDetailUrl(galleryDetail.gid, galleryDetail.token, 0, showAll)
        val detail = EhEngine.getGalleryDetail(url)
        comments = detail.comments
    }

    val copyComment = stringResource(R.string.copy_comment_text)
    val blockCommenter = stringResource(R.string.block_commenter)
    val cancelVoteUp = stringResource(R.string.cancel_vote_up)
    val cancelVoteDown = stringResource(R.string.cancel_vote_down)
    val voteUp = stringResource(R.string.vote_up)
    val voteDown = stringResource(R.string.vote_down)
    val checkVoteStatus = stringResource(R.string.check_vote_status)

    suspend fun Context.showFilterCommenter(comment: GalleryComment) {
        val commenter = comment.user ?: return
        dialogState.awaitPermissionOrCancel { Text(text = stringResource(R.string.filter_the_commenter, commenter)) }
        Filter(FilterMode.COMMENTER, commenter).remember()
        comments = comments.copy(comments = comments.comments.filter { it == comment })
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

    BackHandler(commenting) {
        commenting = false
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
                FloatingActionButton(onClick = { commenting = true }) {
                    Icon(imageVector = Icons.AutoMirrored.Default.Reply, contentDescription = null)
                }
            }
        },
    ) { paddingValues ->
        val keylineMargin = dimensionResource(id = R.dimen.keyline_margin)
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = keylineMargin),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = paddingValues,
            ) {
                items(comments.comments) { item ->
                    val recomposeScope = currentRecomposeScope

                    suspend fun Context.voteComment(comment: GalleryComment, isUp: Boolean) {
                        galleryDetail.runSuspendCatching {
                            EhEngine.voteComment(apiUid, apiKey, gid, token, comment.id, if (isUp) 1 else -1)
                        }.onSuccess { result ->
                            findActivity<MainActivity>().showTip(
                                if (isUp) (if (0 != result.vote) R.string.vote_up_successfully else R.string.cancel_vote_up_successfully) else if (0 != result.vote) R.string.vote_down_successfully else R.string.cancel_vote_down_successfully,
                                BaseScene.LENGTH_SHORT,
                            )
                            comment.score = result.score
                            if (isUp) {
                                comment.voteUpEd = 0 != result.vote
                                comment.voteDownEd = false
                            } else {
                                comment.voteDownEd = 0 != result.vote
                                comment.voteUpEd = false
                            }
                            recomposeScope.invalidate()
                        }.onFailure {
                            findActivity<MainActivity>().showTip(R.string.vote_failed, BaseScene.LENGTH_LONG)
                        }
                    }

                    suspend fun Context.doCommentAction(comment: GalleryComment) {
                        val actions = buildAction {
                            copyComment thenDo { findActivity<MainActivity>().addTextToClipboard(comment.comment.orEmpty().parseAsHtml()) }
                            if (!comment.uploader && !comment.editable) {
                                blockCommenter thenDo { showFilterCommenter(comment) }
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
                    ) {
                        maxLines = 5
                        text = context.generateComment(this, item)
                        setOnClickListener {
                            val span = currentSpan
                            clearCurrentSpan()
                            if (span is URLSpan) {
                                val activity = context.findActivity<MainActivity>()
                                if (!activity.jumpToReaderByPage(span.url, galleryDetail)) {
                                    if (!navigator.navWithUrl(span.url)) {
                                        activity.openBrowser(span.url)
                                    }
                                }
                            }
                        }
                    }
                }
                if (comments.hasMore) {
                    item {
                        // TODO: This animation need to be investigated
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
            if (commenting) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        BasicTextField2(
                            value = userComment,
                            onValueChange = { userComment = it },
                            modifier = Modifier.weight(1f).padding(keylineMargin),
                        )
                        IconButton(
                            onClick = { commenting = false },
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
}

class GalleryCommentsFragment : BaseScene(), View.OnClickListener {
    private var _binding: SceneGalleryCommentsBinding? = null
    private val binding get() = _binding!!
    private var mGalleryDetail: GalleryDetail? = null
    private var mSendDrawable: Drawable? = null
    private var mPencilDrawable: Drawable? = null
    private var mCommentId: Long = 0
    private var mInAnimation = false
    private var mShowAllComments = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SceneGalleryCommentsBinding.inflate(inflater, FrameLayout(inflater.context))
        val tip = binding.tip
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.root,
            WindowInsetsAnimationHelper(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
                binding.editPanel,
                binding.fabLayout,
            ),
        )
        val context = requireContext()
        val drawable = ContextCompat.getDrawable(context, R.drawable.big_sad_pandroid)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        tip.setCompoundDrawables(null, drawable, null, null)
        mSendDrawable = ContextCompat.getDrawable(context, R.drawable.v_send_dark_x24)
        mPencilDrawable = ContextCompat.getDrawable(context, R.drawable.v_pencil_dark_x24)
        binding.recyclerView.layoutManager = LinearLayoutManager(
            context,
            RecyclerView.VERTICAL,
            false,
        )
        binding.recyclerView.setHasFixedSize(true)
        // Cancel change animator
        val itemAnimator = binding.recyclerView.itemAnimator
        if (itemAnimator is DefaultItemAnimator) {
            itemAnimator.supportsChangeAnimations = false
        }
        binding.send.setOnClickListener(this)
        binding.editText.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                requireActivity().menuInflater.inflate(R.menu.context_comment, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                item?.let {
                    val text = binding.editText.editableText
                    val start = binding.editText.selectionStart
                    val end = binding.editText.selectionEnd
                    when (item.itemId) {
                        R.id.action_bold -> text[start, end] = StyleSpan(Typeface.BOLD)

                        R.id.action_italic -> text[start, end] = StyleSpan(Typeface.ITALIC)

                        R.id.action_underline -> text[start, end] = UnderlineSpan()

                        R.id.action_strikethrough -> text[start, end] = StrikethroughSpan()

                        R.id.action_url -> {
                            val oldSpans = text.getSpans<URLSpan>(start, end)
                            var oldUrl = "https://"
                            oldSpans.forEach {
                                if (!it.url.isNullOrEmpty()) {
                                    oldUrl = it.url
                                }
                            }
                            val builder = EditTextDialogBuilder(
                                context,
                                oldUrl,
                                getString(R.string.format_url),
                            )
                            builder.setTitle(getString(R.string.format_url))
                            builder.setPositiveButton(android.R.string.ok, null)
                            val dialog = builder.show()
                            val button: View? = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                            button?.setOnClickListener(
                                View.OnClickListener {
                                    val url = builder.text.trim()
                                    if (url.isEmpty()) {
                                        builder.setError(getString(R.string.text_is_empty))
                                        return@OnClickListener
                                    } else {
                                        builder.setError(null)
                                    }
                                    text.clearSpan(start, end, true)
                                    text[start, end] = URLSpan(url)
                                    dialog.dismiss()
                                },
                            )
                        }

                        R.id.action_clear -> {
                            text.clearSpan(start, end, false)
                        }

                        else -> return false
                    }
                    mode?.finish()
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
            }
        }
        binding.fab.setOnClickListener(this)
        binding.editPanel.applyInsetter {
            type(ime = true, navigationBars = true) {
                padding()
            }
        }
        binding.recyclerView.applyInsetter {
            type(ime = true, navigationBars = true) {
                padding()
            }
        }
        binding.fabLayout.applyNavigationBarsPadding()
        return ComposeWithMD3 {
            val galleryDetail = remember { requireArguments().getParcelableCompat<GalleryDetail>(KEY_GALLERY_DETAIL)!! }
            val navController = remember { findNavController() }
            GalleryCommentsScreen(galleryDetail = galleryDetail, navigator = navController)
        }
    }

    fun Spannable.clearSpan(start: Int, end: Int, url: Boolean) {
        val spans = if (url) getSpans<URLSpan>(start, end) else getSpans<CharacterStyle>(start, end)
        spans.forEach {
            val spanStart = getSpanStart(it)
            val spanEnd = getSpanEnd(it)
            removeSpan(it)
            if (spanStart < start) {
                this[spanStart, start] = it
            }
            if (spanEnd > end) {
                this[end, spanEnd] = it
            }
        }
    }

    private fun prepareNewComment() {
        mCommentId = 0
        binding.send.setImageDrawable(mSendDrawable)
    }

    private fun prepareEditComment(commentId: Long, text: CharSequence) {
        mCommentId = commentId
        binding.editText.setText(text)
        binding.send.setImageDrawable(mPencilDrawable)
    }

    private val galleryDetailUrl: String?
        get() = if (mGalleryDetail != null && mGalleryDetail!!.gid != -1L && mGalleryDetail!!.token != null) {
            EhUrl.getGalleryDetailUrl(
                mGalleryDetail!!.gid,
                mGalleryDetail!!.token,
                0,
                mShowAllComments,
            )
        } else {
            null
        }

    override fun onClick(v: View) {
        val context = context
        val activity = mainActivity
        if (null == context || null == activity) {
            return
        }
        if (binding.fab === v) {
            if (!mInAnimation) {
                prepareNewComment()
            }
        } else if (binding.send === v) {
            if (!mInAnimation) {
                val comment = binding.editText.text?.toBBCode()?.takeIf { it.isNotBlank() } ?: return
                val url = galleryDetailUrl ?: return
                lifecycleScope.launchIO {
                    runSuspendCatching {
                        EhEngine.commentGallery(
                            url,
                            comment,
                            if (mCommentId != 0L) mCommentId.toString() else null,
                        )
                    }.onSuccess {
                        showTip(
                            if (mCommentId != 0L) R.string.edit_comment_successfully else R.string.comment_successfully,
                            LENGTH_SHORT,
                        )
                        withUIContext {
                            onCommentGallerySuccess(it)
                        }
                    }.onFailure {
                        showTip(
                            """
    ${getString(if (mCommentId != 0L) R.string.edit_comment_failed else R.string.comment_failed)}
    ${ExceptionUtils.getReadableString(it)}
                            """.trimIndent(),
                            LENGTH_LONG,
                        )
                    }
                }
            }
        }
    }

    private fun onCommentGallerySuccess(result: GalleryCommentList) {
        mGalleryDetail!!.comments = result
        // Remove text
        binding.editText.setText("")
    }

    companion object {
        val TAG: String = GalleryCommentsFragment::class.java.simpleName
        const val KEY_API_UID = "api_uid"
        const val KEY_API_KEY = "api_key"
        const val KEY_GID = "gid"
        const val KEY_TOKEN = "token"
        const val KEY_COMMENT_LIST = "comment_list"
        const val KEY_GALLERY_DETAIL = "gallery_detail"
    }
}
