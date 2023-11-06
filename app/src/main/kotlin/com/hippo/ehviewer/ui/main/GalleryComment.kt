package com.hippo.ehviewer.ui.main

import android.text.style.ClickableSpan
import android.text.style.URLSpan
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.ui.legacy.LinkifyTextView
import com.hippo.ehviewer.util.ReadableTime

@Composable
fun GalleryCommentCard(
    modifier: Modifier = Modifier,
    comment: GalleryComment,
    onCardClick: () -> Unit,
    onUserClick: () -> Unit,
    onUrlClick: (String) -> Unit,
    update: LinkifyTextView.() -> Unit,
) = comment.run {
    var currentSpan by remember(comment) { mutableStateOf<ClickableSpan?>(null) }
    Card(
        onClick = {
            val span = currentSpan.apply { currentSpan = null }
            if (span is URLSpan) {
                onUrlClick(span.url)
            } else {
                onCardClick()
            }
        },
        modifier = modifier,
    ) {
        val keylineMargin = dimensionResource(id = R.dimen.keyline_margin)
        ConstraintLayout(modifier = Modifier.padding(horizontal = keylineMargin, vertical = 8.dp).fillMaxWidth()) {
            val (userRef, timeRef, commentRef) = createRefs()
            ProvideTextStyle(value = MaterialTheme.typography.titleSmall) {
                val userText = if (uploader) stringResource(id = R.string.comment_user_uploader, user.orEmpty()) else user.orEmpty()
                Text(
                    text = userText,
                    modifier = Modifier.constrainAs(userRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }.clickable(onClick = onUserClick),
                )
                Text(
                    text = ReadableTime.getTimeAgo(time),
                    modifier = Modifier.constrainAs(timeRef) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    },
                )
            }
            AndroidView(
                factory = { context -> LinkifyTextView(context) { currentSpan = it } },
                modifier = Modifier.constrainAs(commentRef) {
                    start.linkTo(parent.start)
                    top.linkTo(userRef.bottom, margin = 8.dp)
                },
                update = update,
            )
        }
    }
}
