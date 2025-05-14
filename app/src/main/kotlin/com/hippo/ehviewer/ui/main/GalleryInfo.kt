package com.hippo.ehviewer.ui.main

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import arrow.core.Tuple7
import coil3.compose.AsyncImage
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.ElevatedCard
import com.hippo.ehviewer.ui.tools.GalleryListCardRating
import com.hippo.ehviewer.ui.tools.SharedElementBox
import com.hippo.ehviewer.ui.tools.TransitionsVisibilityScope
import com.hippo.ehviewer.ui.tools.listThumbGenerator
import com.hippo.ehviewer.util.FavouriteStatusRouter

private val ids = Tuple7(1, 2, 3, 4, 5, 6, 7)

private val constraintSet = ConstraintSet {
    val (titleRef, uploaderRef, ratingRef, categoryRef, postedRef, favRef, iconsRef) =
        createRefsFor(1, 2, 3, 4, 5, 6, 7)
    constrain(titleRef) {
        top.linkTo(parent.top)
        width = Dimension.matchParent
    }
    constrain(uploaderRef) {
        start.linkTo(parent.start)
        bottom.linkTo(iconsRef.top)
    }
    constrain(ratingRef) {
        start.linkTo(parent.start)
        top.linkTo(iconsRef.top, 1.dp)
    }
    constrain(categoryRef) {
        start.linkTo(parent.start)
        bottom.linkTo(parent.bottom)
    }
    constrain(iconsRef) {
        end.linkTo(parent.end)
        bottom.linkTo(categoryRef.top)
    }
    constrain(favRef) {
        end.linkTo(parent.end)
        bottom.linkTo(iconsRef.top)
    }
    constrain(postedRef) {
        end.linkTo(parent.end)
        linkTo(categoryRef.top, parent.bottom)
    }
}

context(SharedTransitionScope, TransitionsVisibilityScope)
@Composable
fun GalleryInfoListItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: GalleryInfo,
    showPages: Boolean,
    modifier: Modifier = Modifier,
    isInFavScene: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = CrystalCard(
    modifier = modifier,
    onClick = onClick,
    onLongClick = onLongClick,
    interactionSource = interactionSource,
) {
    Row {
        with(listThumbGenerator) {
            EhThumbCard(
                key = info,
                modifier = Modifier.aspectRatio(DEFAULT_RATIO),
            )
        }
        ConstraintLayout(
            modifier = Modifier.padding(start = 8.dp, top = 2.dp, end = 4.dp, bottom = 4.dp).fillMaxSize(),
            constraintSet = constraintSet,
        ) {
            val (titleRef, uploaderRef, ratingRef, categoryRef, postedRef, favRef, iconsRef) = ids
            Text(
                text = EhUtils.getSuitableTitle(info),
                maxLines = 2,
                modifier = Modifier.layoutId(titleRef),
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
                    // Placeholder to reserve minimum height
                    Text(text = "")
                    val download by DownloadManager.collectContainDownloadInfo(info.gid)
                    if (download) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    info.simpleLanguage?.let {
                        Text(text = it)
                    }
                    if (info.pages != 0 && showPages) {
                        Text(text = "${info.pages}P")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.layoutId(favRef),
                ) {
                    // Placeholder to reserve minimum height
                    Text(text = "")
                    if (isInFavScene) {
                        info.favoriteNote?.let {
                            Text(text = it, fontStyle = FontStyle.Italic)
                        }
                    } else {
                        val showFav by FavouriteStatusRouter.collectAsState(info) { it != NOT_FAVORITED }
                        if (showFav) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            info.favoriteName?.let {
                                Text(text = it)
                            }
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

context(SharedTransitionScope, TransitionsVisibilityScope)
@Composable
fun GalleryInfoGridItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: GalleryInfo,
    modifier: Modifier = Modifier,
    showLanguage: Boolean = true,
    showPages: Boolean = true,
    showFavoriteStatus: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = ElevatedCard(
    modifier = modifier,
    onClick = onClick,
    onLongClick = onLongClick,
    interactionSource = interactionSource,
) {
    Box {
        with(listThumbGenerator) {
            SharedElementBox(key = "${info.gid}", shape = ShapeDefaults.Medium) {
                var ratio by remember(info) {
                    val ratio = if (info.thumbHeight != 0) {
                        (info.thumbWidth.toFloat() / info.thumbHeight).coerceIn(MIN_RATIO, MAX_RATIO)
                    } else {
                        DEFAULT_RATIO
                    }
                    mutableFloatStateOf(ratio)
                }
                AsyncImage(
                    model = requestOf(info),
                    contentDescription = null,
                    modifier = Modifier.aspectRatio(ratio),
                    onSuccess = {
                        ratio = (it.result.image.width.toFloat() / it.result.image.height).coerceIn(MIN_RATIO, MAX_RATIO)
                    },
                )
            }
        }
        val categoryColor = EhUtils.getCategoryColor(info.category)
        Badge(
            modifier = Modifier.align(Alignment.TopEnd).widthIn(min = 32.dp).height(24.dp),
            containerColor = categoryColor,
            contentColor = if (Settings.harmonizeCategoryColor) contentColorFor(categoryColor) else EhUtils.categoryTextColor,
        ) {
            val shouldShowLanguage = showLanguage && info.simpleLanguage != null
            if (showPages && info.pages > 0) {
                Text(text = "${info.pages}")
                if (shouldShowLanguage) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            if (shouldShowLanguage) {
                Text(text = info.simpleLanguage.orEmpty())
            }
        }
        if (showFavoriteStatus) {
            val isFavorited by FavouriteStatusRouter.collectAsState(info) { it != NOT_FAVORITED }
            if (isFavorited) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                    tint = EhUtils.favoriteIconColor,
                )
            }
        }
    }
}

private const val MIN_RATIO = 0.5F
private const val MAX_RATIO = 1.5F
const val DEFAULT_RATIO = 0.67F
