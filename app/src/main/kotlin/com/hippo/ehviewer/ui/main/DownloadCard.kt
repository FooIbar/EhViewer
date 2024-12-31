package com.hippo.ehviewer.ui.main

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import arrow.core.Tuple9
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.GalleryListCardRating
import com.hippo.ehviewer.ui.tools.TransitionsVisibilityScope
import com.hippo.ehviewer.ui.tools.listThumbGenerator
import com.hippo.ehviewer.util.FileUtils

private val ids = Tuple9(1, 2, 3, 4, 5, 6, 7, 8, 9)

private val constraintSet = ConstraintSet {
    val (
        titleRef, uploaderRef, ratingRef, categoryRef, actionsRef, stateTextRef,
        progressBarRef, progressTextRef, speedRef,
    ) = createRefsFor(1, 2, 3, 4, 5, 6, 7, 8, 9)
    constrain(titleRef) {
        top.linkTo(parent.top)
        width = Dimension.matchParent
    }
    constrain(uploaderRef) {
        start.linkTo(parent.start)
        bottom.linkTo(actionsRef.top)
    }
    constrain(ratingRef) {
        start.linkTo(parent.start)
        top.linkTo(actionsRef.top, 1.dp)
    }
    constrain(categoryRef) {
        start.linkTo(parent.start)
        bottom.linkTo(parent.bottom)
    }
    constrain(progressBarRef) {
        bottom.linkTo(actionsRef.top)
        width = Dimension.matchParent
    }
    constrain(progressTextRef) {
        bottom.linkTo(progressBarRef.top)
        start.linkTo(progressBarRef.start)
    }
    constrain(speedRef) {
        bottom.linkTo(progressBarRef.top)
        end.linkTo(progressBarRef.end)
    }
    constrain(actionsRef) {
        end.linkTo(parent.end, (-4).dp)
        bottom.linkTo(parent.bottom, (-4).dp)
    }
    constrain(stateTextRef) {
        end.linkTo(parent.end)
        bottom.linkTo(actionsRef.top)
    }
}

context(SharedTransitionScope, TransitionsVisibilityScope)
@Composable
fun DownloadCard(
    onClick: () -> Unit,
    onThumbClick: () -> Unit,
    onLongClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    info: DownloadInfo,
    selectMode: Boolean,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = CrystalCard(modifier = modifier, onClick = onClick, onLongClick = onLongClick, interactionSource = interactionSource) {
    Row {
        val thumb = remember {
            movableContentOf<DownloadInfo> {
                with(listThumbGenerator) {
                    EhAsyncCropThumb(
                        key = it,
                        modifier = Modifier.aspectRatio(DEFAULT_RATIO).fillMaxSize(),
                    )
                }
            }
        }
        if (selectMode) {
            Card {
                thumb(info)
            }
        } else {
            Card(onClick = onThumbClick) {
                thumb(info)
            }
        }
        val stateFailed = stringResource(R.string.download_state_failed)
        val stateFailed2 = stringResource(R.string.download_state_failed_2, info.legacy)
        val downloadState by DownloadManager.collectDownloadState(info.gid)
        val stateText = when (downloadState) {
            DownloadInfo.STATE_NONE -> stringResource(R.string.download_state_none)
            DownloadInfo.STATE_WAIT -> stringResource(R.string.download_state_wait)
            DownloadInfo.STATE_DOWNLOAD -> null
            DownloadInfo.STATE_FAILED -> if (info.legacy <= 0) stateFailed else stateFailed2
            DownloadInfo.STATE_FINISH -> stringResource(R.string.download_state_finish)
            else -> null // Chill, will be removed soon
        }
        ConstraintLayout(
            modifier = Modifier.padding(start = 8.dp, top = 2.dp, end = 4.dp, bottom = 4.dp).fillMaxSize(),
            constraintSet = constraintSet,
        ) {
            val (
                titleRef, uploaderRef, ratingRef, categoryRef, actionsRef, stateTextRef,
                progressBarRef, progressTextRef, speedRef,
            ) = ids
            Text(
                text = EhUtils.getSuitableTitle(info),
                maxLines = 2,
                modifier = Modifier.layoutId(titleRef),
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            if (downloadState != DownloadInfo.STATE_DOWNLOAD) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    info.uploader?.let {
                        Text(
                            text = it,
                            modifier = Modifier.layoutId(uploaderRef).alpha(if (info.disowned) 0.5f else 1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    GalleryListCardRating(
                        rating = info.rating,
                        modifier = Modifier.layoutId(ratingRef),
                    )
                    val categoryColor = EhUtils.getCategoryColor(info.category)
                    val categoryText = EhUtils.getCategory(info.category).uppercase()
                    Text(
                        text = categoryText,
                        modifier = Modifier.layoutId(categoryRef).clip(ShapeDefaults.Small)
                            .background(categoryColor).padding(vertical = 2.dp, horizontal = 8.dp),
                        color = if (Settings.harmonizeCategoryColor) Color.Unspecified else EhUtils.categoryTextColor,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val (total, finished, speed) = DownloadManager.updatedDownloadInfo(info) {
                    Triple(total, finished, speed)
                }
                ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                    if (total <= 0 || finished < 0) {
                        LinearProgressIndicator(modifier = Modifier.layoutId(progressBarRef))
                    } else {
                        LinearProgressIndicator(
                            progress = { finished.toFloat() / total.toFloat() },
                            modifier = Modifier.layoutId(progressBarRef),
                        )
                        Text(
                            text = "$finished/$total",
                            modifier = Modifier.layoutId(progressTextRef),
                        )
                    }
                    Text(
                        text = FileUtils.humanReadableByteCount(speed.coerceAtLeast(0)) + "/S",
                        modifier = Modifier.layoutId(speedRef),
                    )
                }
            }
            val running = downloadState == DownloadInfo.STATE_WAIT || downloadState == DownloadInfo.STATE_DOWNLOAD
            val icon = remember {
                movableContentOf<Boolean> {
                    Icon(imageVector = if (it) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                }
            }
            if (selectMode) {
                Box(modifier = Modifier.layoutId(actionsRef).minimumInteractiveComponentSize()) {
                    icon(running)
                }
            } else {
                IconButton(onClick = if (running) onStop else onStart, modifier = Modifier.layoutId(actionsRef)) {
                    icon(running)
                }
            }
            if (stateText != null) {
                Text(
                    text = stateText,
                    modifier = Modifier.layoutId(stateTextRef),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
