package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.decode.DecodeUtils
import coil3.size.Scale
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.ui.tools.thenIf
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.toIntOrDefault
import kotlin.math.roundToInt

private const val IMAGE_OBJ = '￼'
private const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"
private val IMAGE_PATTERN =
    "(?:<a href=\"([^\"]+)\">)?<img src=\"((?:[^-]+-){2}(\\d+)-(\\d+)-[^_]+_([^.]+)[^\"]+)\"".toRegex()

private fun breakToTextAndInlineContent(
    origin: String,
    text: AnnotatedString,
    density: Density,
    onUrlClick: (String) -> Unit,
): Pair<AnnotatedString, Map<String, InlineTextContent>> = with(density) {
    val urls = IMAGE_PATTERN.findAll(origin)
    val iter = urls.iterator()
    if (!iter.hasNext()) return text to emptyMap()
    var currentOfs = 0
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    return buildAnnotatedString {
        append(text)
        while (true) {
            val index = text.text.indexOf(IMAGE_OBJ, currentOfs)
            if (index == -1) break
            val key = "$index"
            addStringAnnotation(INLINE_CONTENT_TAG, key, index, index + 1)
            val groupValues = iter.next().groupValues
            val url = groupValues[1]
            val imageUrl = groupValues[2]
            val srcWidth = groupValues[3].toInt()
            val srcHeight = groupValues[4].toInt()
            val dstWidth = groupValues[5].toIntOrDefault(200)
            val dstHeight = dstWidth / 2 * 3
            val multiplier = DecodeUtils.computeSizeMultiplier(srcWidth, srcHeight, dstWidth, dstHeight, Scale.FIT)

            // TODO: Use dynamic inline content
            // https://issuetracker.google.com/294110693
            inlineContent[key] = InlineTextContent(
                Placeholder(
                    width = (srcWidth * multiplier).roundToInt().toSp(),
                    height = (srcHeight * multiplier).roundToInt().toSp(),
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Top,
                ),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.thenIf(url.isNotEmpty()) {
                        clickable { onUrlClick(url) }
                    },
                )
            }
            currentOfs = index + 1
        }
    } to inlineContent
}

@Composable
fun GalleryCommentCard(
    modifier: Modifier = Modifier,
    comment: GalleryComment,
    onCardClick: () -> Unit,
    onUserClick: () -> Unit,
    onUrlClick: (String) -> Unit,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    processComment: @Composable (GalleryComment, TextLinkStyles, LinkInteractionListener) -> AnnotatedString = { c, s, l -> AnnotatedString.fromHtml(c.comment, s, l) },
) = with(comment) {
    Card(onClick = onCardClick, modifier = modifier) {
        val margin = dimensionResource(id = R.dimen.keyline_margin)
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
        Column(
            modifier = Modifier.padding(start = margin, end = margin, bottom = 8.dp),
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
                val (text, inlineContent) = breakToTextAndInlineContent(
                    comment.comment,
                    processed,
                    LocalDensity.current,
                    onUrlClick,
                )
                Text(
                    text = text,
                    maxLines = maxLines,
                    overflow = overflow,
                    inlineContent = inlineContent,
                )
            }
        }
    }
}
