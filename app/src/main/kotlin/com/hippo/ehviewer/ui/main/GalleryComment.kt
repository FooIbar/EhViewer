package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import arrow.core.Either
import coil3.compose.AsyncImage
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.ui.screen.breakToTextAndUrl
import com.hippo.ehviewer.util.ReadableTime

typealias TextOrUrl = Either<String, AnnotatedString>
typealias TextOrUrlList = List<TextOrUrl>

@Composable
fun GalleryCommentCard(
    modifier: Modifier = Modifier,
    comment: GalleryComment,
    onCardClick: () -> Unit,
    onUserClick: () -> Unit,
    onUrlClick: (String) -> Unit,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    showImage: Boolean = false,
    processComment: @Composable (GalleryComment, TextLinkStyles, LinkInteractionListener) -> AnnotatedString = { c, s, l -> AnnotatedString.fromHtml(c.comment, s, l) },
) = with(comment) {
    Card(onClick = onCardClick, modifier = modifier) {
        val margin = dimensionResource(id = R.dimen.keyline_margin)
        ConstraintLayout(modifier = Modifier.padding(horizontal = margin, vertical = 8.dp).fillMaxWidth()) {
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
            Column(
                modifier = Modifier.constrainAs(commentRef) {
                    start.linkTo(parent.start)
                    top.linkTo(userRef.bottom, margin = 8.dp)
                },
            ) {
                ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                    val linkColor = MaterialTheme.colorScheme.primary
                    val processed = processComment(
                        comment,
                        TextLinkStyles(style = SpanStyle(color = linkColor)),
                    ) { link ->
                        check(link is LinkAnnotation.Url)
                        onUrlClick(link.url)
                    }
                    if (showImage) {
                        val list = breakToTextAndUrl(comment.comment, processed)
                        list.forEach {
                            when (it) {
                                is Either.Left -> AsyncImage(
                                    model = it.value,
                                    contentDescription = null,
                                )
                                is Either.Right -> Text(text = it.value)
                            }
                        }
                    } else {
                        Text(
                            text = processed,
                            maxLines = maxLines,
                            overflow = overflow,
                        )
                    }
                }
            }
        }
    }
}
