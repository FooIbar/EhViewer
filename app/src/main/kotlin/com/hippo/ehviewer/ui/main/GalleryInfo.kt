package com.hippo.ehviewer.ui.main

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ehviewer.core.ui.component.CrystalCard
import com.ehviewer.core.ui.component.ElevatedCard
import com.ehviewer.core.ui.util.SharedElementBox
import com.ehviewer.core.ui.util.TransitionsVisibilityScope
import com.ehviewer.core.ui.util.listThumbGenerator
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.tools.GalleryListCardRating
import com.hippo.ehviewer.util.FavouriteStatusRouter

@Composable
context(_: SharedTransitionScope, _: TransitionsVisibilityScope)
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
        Column(modifier = Modifier.padding(start = 8.dp, top = 2.dp, end = 4.dp, bottom = 4.dp)) {
            Text(
                text = EhUtils.getSuitableTitle(info),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.weight(1f))
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = info.uploader.orEmpty(),
                        modifier = Modifier.alignByBaseline().alpha(if (info.disowned) 0.5f else 1f),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isInFavScene) {
                        info.favoriteNote?.let {
                            Text(text = it, modifier = Modifier.alignByBaseline(), fontStyle = FontStyle.Italic)
                        }
                    } else {
                        val showFav by FavouriteStatusRouter.collectAsState(info) { it != NOT_FAVORITED }
                        if (showFav) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).align(Alignment.CenterVertically),
                            )
                            info.favoriteName?.let {
                                Text(text = it, modifier = Modifier.alignByBaseline())
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Place the rating near the uploader text as there's more visual space
                    GalleryListCardRating(rating = info.rating, modifier = Modifier.padding(top = 1.dp, bottom = 3.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    val downloaded by DownloadManager.collectContainDownloadInfo(info.gid)
                    if (downloaded) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val categoryColor = EhUtils.getCategoryColor(info.category)
                    val categoryText = EhUtils.getCategory(info.category).uppercase()
                    Text(
                        text = categoryText,
                        modifier = Modifier.clip(ShapeDefaults.Small).background(categoryColor).padding(vertical = 2.dp, horizontal = 8.dp),
                        color = if (Settings.harmonizeCategoryColor.value) Color.Unspecified else EhUtils.categoryTextColor,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = info.posted.orEmpty())
                }
            }
        }
    }
}

@Composable
context(_: SharedTransitionScope, _: TransitionsVisibilityScope)
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
            contentColor = if (Settings.harmonizeCategoryColor.value) contentColorFor(categoryColor) else EhUtils.categoryTextColor,
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
