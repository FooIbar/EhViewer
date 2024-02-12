package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import arrow.core.Tuple7
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.ElevatedCard
import com.hippo.ehviewer.ui.tools.GalleryListCardRating
import com.hippo.ehviewer.util.FavouriteStatusRouter

private val ids = Tuple7(1, 2, 3, 4, 5, 6, 7)

private val constraintSet = ConstraintSet {
    val (titleRef, uploaderRef, ratingRef, categoryRef, postedRef, favRef, iconsRef) =
        createRefsFor(1, 2, 3, 4, 5, 6, 7)
    constrain(titleRef) {
        top.linkTo(parent.top)
        start.linkTo(parent.start)
    }
    constrain(uploaderRef) {
        start.linkTo(parent.start)
        bottom.linkTo(ratingRef.top)
    }
    constrain(ratingRef) {
        start.linkTo(parent.start)
        bottom.linkTo(categoryRef.top)
    }
    constrain(categoryRef) {
        start.linkTo(parent.start)
        bottom.linkTo(parent.bottom)
    }
    constrain(iconsRef) {
        end.linkTo(parent.end)
        bottom.linkTo(postedRef.top)
    }
    constrain(favRef) {
        end.linkTo(parent.end)
        bottom.linkTo(iconsRef.top)
    }
    constrain(postedRef) {
        end.linkTo(parent.end)
        bottom.linkTo(parent.bottom)
    }
}

@Composable
fun GalleryInfoListItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: GalleryInfo,
    modifier: Modifier = Modifier,
    isInFavScene: Boolean = false,
    showPages: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    CrystalCard(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        interactionSource = interactionSource,
    ) {
        Row {
            Card {
                EhAsyncCropThumb(
                    key = info,
                    modifier = Modifier.aspectRatio(DEFAULT_ASPECT).fillMaxSize(),
                )
            }
            ConstraintLayout(modifier = Modifier.padding(8.dp, 4.dp).fillMaxSize(), constraintSet = constraintSet) {
                val (titleRef, uploaderRef, ratingRef, categoryRef, postedRef, favRef, iconsRef) = ids
                Text(
                    text = EhUtils.getSuitableTitle(info),
                    maxLines = 2,
                    modifier = Modifier.layoutId(titleRef).fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
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
                        modifier = Modifier.layoutId(categoryRef).clip(ShapeDefaults.Small).background(categoryColor).padding(vertical = 2.dp, horizontal = 8.dp),
                        color = if (Settings.harmonizeCategoryColor) Color.Unspecified else EhUtils.categoryTextColor,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.layoutId(iconsRef),
                    ) {
                        val download by DownloadManager.collectContainDownloadInfo(info.gid)
                        if (download) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(text = info.simpleLanguage.orEmpty())
                        if (info.pages != 0 && showPages) {
                            Text(text = "${info.pages}P")
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.layoutId(favRef),
                    ) {
                        if (isInFavScene) {
                            Text(
                                text = info.favoriteNote.orEmpty(),
                                fontStyle = FontStyle.Italic,
                            )
                        } else {
                            val showFav by FavouriteStatusRouter.collectAsState(info) { it != NOT_FAVORITED }
                            if (showFav) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(text = info.favoriteName.orEmpty())
                            }
                        }
                    }
                    Text(
                        text = info.posted.orEmpty(),
                        modifier = Modifier.layoutId(postedRef),
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryInfoGridItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: GalleryInfo,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val simpleLang = info.simpleLanguage
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        interactionSource = interactionSource,
    ) {
        Box {
            val placeholder = remember {
                val aspect = if (info.thumbHeight != 0) {
                    (info.thumbWidth.toFloat() / info.thumbHeight).coerceIn(MIN_ASPECT, MAX_ASPECT)
                } else {
                    DEFAULT_ASPECT
                }
                BrushPainter(Brush.linearGradient(PlaceholderColors, end = Offset(aspect, 1f)))
            }
            EhAsyncThumb(
                model = info,
                modifier = Modifier.fillMaxWidth(),
                placeholder = placeholder,
                contentScale = ContentScale.Crop,
            )
            val categoryColor = EhUtils.getCategoryColor(info.category)
            Badge(
                modifier = Modifier.align(Alignment.TopEnd).width(32.dp).height(24.dp),
                containerColor = categoryColor,
                contentColor = if (Settings.harmonizeCategoryColor) contentColorFor(categoryColor) else EhUtils.categoryTextColor,
            ) {
                simpleLang?.let {
                    Text(text = it.uppercase())
                }
            }
        }
    }
}

private val PlaceholderColors = listOf(Color.Transparent, Color.Transparent)

private const val MIN_ASPECT = 0.33F
private const val MAX_ASPECT = 1.5F
const val DEFAULT_ASPECT = 0.67F
