package com.hippo.ehviewer.ui.main

import android.text.Html
import android.text.TextUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.parseAsHtml
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.ui.legacy.CoilImageGetter
import com.hippo.ehviewer.ui.legacy.LinkifyTextView
import com.hippo.ehviewer.util.ReadableTime

@Composable
fun GalleryCommentCard(
    modifier: Modifier = Modifier,
    comment: GalleryComment,
    onCardClick: () -> Unit,
    onUserClick: () -> Unit,
    onUrlClick: (String) -> Unit,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
    processComment: @Composable (GalleryComment, Html.ImageGetter) -> CharSequence = { c, ig -> c.comment.parseAsHtml(imageGetter = ig) },
) = with(comment) {
    val targetUrl = remember { mutableStateOf<String?>(null) }
    Card(
        onClick = {
            targetUrl.value?.also {
                onUrlClick(it)
                targetUrl.value = null
            } ?: onCardClick()
        },
        modifier = modifier,
    ) {
        val margin = dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)
        Row(
            modifier = Modifier.padding(horizontal = margin, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.titleSmall) {
                val userText = if (uploader) stringResource(id = R.string.comment_user_uploader, user.orEmpty()) else user.orEmpty()
                Text(
                    text = userText,
                    modifier = Modifier.clickable(onClick = onUserClick),
                )
                Text(
                    text = ReadableTime.getTimeAgo(time),
                )
            }
        }
        val textColor = LocalContentColor.current.toArgb()
        val linkTextColor = MaterialTheme.colorScheme.primary.toArgb()
        val redrawSignal = remember { mutableStateOf(Unit, neverEqualPolicy()) }
        val commentText = processComment(comment, CoilImageGetter { redrawSignal.value = Unit })
        AndroidView(
            factory = { context ->
                LinkifyTextView(context) { targetUrl.value = it }.apply {
                    setTextColor(textColor)
                    setLinkTextColor(linkTextColor)
                    this.maxLines = maxLines
                    if (ellipsis) ellipsize = TextUtils.TruncateAt.END
                }
            },
            modifier = Modifier.padding(start = margin, end = margin, bottom = 8.dp),
        ) { view ->
            view.text = commentText
            redrawSignal.value
        }
    }
}
