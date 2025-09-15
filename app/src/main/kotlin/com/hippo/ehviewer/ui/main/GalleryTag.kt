package com.hippo.ehviewer.ui.main

import android.content.Context
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.data.GalleryTagGroup
import com.hippo.ehviewer.client.data.PowerStatus
import com.hippo.ehviewer.client.data.TagNamespace
import com.hippo.ehviewer.client.data.VoteStatus
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.tools.includeFontPadding

@Composable
context(_: Context)
fun GalleryTags(
    tagGroups: List<GalleryTagGroup>,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String, String, VoteStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canTranslate = Settings.showTagTranslations.value && EhTagDatabase.translatable && EhTagDatabase.initialized
    val ehTags = EhTagDatabase.takeIf { canTranslate }
    fun TagNamespace.translate() = ehTags?.getTranslation(tag = value) ?: value
    fun String.translate(ns: TagNamespace) = ehTags?.getTranslation(prefix = ns.prefix, tag = this) ?: this
    val showVote by Settings.showVoteStatus.collectAsState()
    Column(modifier) {
        tagGroups.forEach { (ns, tags) ->
            Row {
                BaseRoundText(
                    text = ns.translate(),
                    isGroup = true,
                )
                FlowRow {
                    tags.forEach { (text, power, vote) ->
                        val translation = text.translate(ns)
                        val tag = ns.value + ":" + text
                        val hapticFeedback = LocalHapticFeedback.current
                        Box {
                            BaseRoundText(
                                text = translation,
                                weak = power == PowerStatus.Weak,
                                modifier = Modifier.combinedClickable(
                                    onClick = { onTagClick(tag) },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTagLongClick(tag, translation, vote)
                                    },
                                ),
                            )
                            if (vote != VoteStatus.None && showVote) {
                                Text(
                                    text = vote.display,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 2.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmallEmphasized.copy(fontSize = 10.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseRoundText(
    text: String,
    modifier: Modifier = Modifier,
    weak: Boolean = false,
    isGroup: Boolean = false,
) {
    val bgColor = if (isGroup) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    Surface(
        modifier = Modifier.padding(4.dp),
        color = bgColor,
        shape = GalleryTagCorner,
    ) {
        Text(
            text = text,
            modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp).width(IntrinsicSize.Max),
            color = LocalContentColor.current.let { if (weak) it.copy(0.5F) else it },
            style = MaterialTheme.typography.labelLarge.includeFontPadding,
        )
    }
}

private val GalleryTagCorner = RoundedCornerShape(64.dp)
