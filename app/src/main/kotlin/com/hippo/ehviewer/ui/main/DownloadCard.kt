package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.GalleryListCardRating
import com.hippo.ehviewer.util.FileUtils

@Composable
fun DownloadCard(
    onClick: () -> Unit,
    onThumbClick: () -> Unit,
    onLongClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    info: DownloadInfo,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    CrystalCard(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        interactionSource = interactionSource,
    ) {
        Row {
            Card(onClick = onThumbClick) {
                EhAsyncCropThumb(
                    key = info,
                    modifier = Modifier.aspectRatio(DEFAULT_ASPECT).fillMaxSize(),
                )
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
            ConstraintLayout(modifier = Modifier.padding(8.dp, 4.dp).fillMaxSize()) {
                val (
                    titleRef, uploaderRef, ratingRef, categoryRef, actionsRef, stateTextRef,
                    progressBarRef, progressTextRef, speedRef,
                ) = createRefs()
                Text(
                    text = EhUtils.getSuitableTitle(info),
                    maxLines = 2,
                    modifier = Modifier.constrainAs(titleRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }.fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (downloadState != DownloadInfo.STATE_DOWNLOAD) {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                        info.uploader?.let {
                            Text(
                                text = it,
                                modifier = Modifier.constrainAs(uploaderRef) {
                                    start.linkTo(parent.start)
                                    bottom.linkTo(ratingRef.top)
                                }.alpha(if (info.disowned) 0.5f else 1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        GalleryListCardRating(
                            rating = info.rating,
                            modifier = Modifier.constrainAs(ratingRef) {
                                start.linkTo(parent.start)
                                bottom.linkTo(categoryRef.top)
                            },
                        )
                        val categoryColor = EhUtils.getCategoryColor(info.category)
                        val categoryText = EhUtils.getCategory(info.category).uppercase()
                        Text(
                            text = categoryText,
                            modifier = Modifier.constrainAs(categoryRef) {
                                start.linkTo(parent.start)
                                bottom.linkTo(parent.bottom)
                            }.clip(ShapeDefaults.Small).background(categoryColor).padding(vertical = 2.dp, horizontal = 8.dp),
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
                            LinearProgressIndicator(
                                modifier = Modifier.constrainAs(progressBarRef) {
                                    bottom.linkTo(actionsRef.top)
                                }.fillMaxWidth(),
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { finished.toFloat() / total.toFloat() },
                                modifier = Modifier.constrainAs(progressBarRef) {
                                    bottom.linkTo(actionsRef.top)
                                }.fillMaxWidth(),
                            )
                            Text(
                                text = "$finished/$total",
                                modifier = Modifier.constrainAs(progressTextRef) {
                                    bottom.linkTo(progressBarRef.top)
                                    start.linkTo(progressBarRef.start)
                                },
                            )
                        }
                        Text(
                            text = FileUtils.humanReadableByteCount(speed.coerceAtLeast(0), false) + "/S",
                            modifier = Modifier.constrainAs(speedRef) {
                                bottom.linkTo(progressBarRef.top)
                                end.linkTo(progressBarRef.end)
                            },
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.constrainAs(actionsRef) {
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                ) {
                    if (downloadState == DownloadInfo.STATE_WAIT || downloadState == DownloadInfo.STATE_DOWNLOAD) {
                        IconButton(onClick = onStop) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                        }
                    } else {
                        IconButton(onClick = onStart) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        }
                    }
                }
                if (stateText != null) {
                    Text(
                        text = stateText,
                        modifier = Modifier.constrainAs(stateTextRef) {
                            end.linkTo(parent.end, 16.dp)
                            bottom.linkTo(actionsRef.top)
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}
