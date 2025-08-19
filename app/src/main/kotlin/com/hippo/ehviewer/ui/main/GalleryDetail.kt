package com.hippo.ehviewer.ui.main

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.big.SadAndroid
import com.ehviewer.core.ui.util.TransitionsVisibilityScope
import com.ehviewer.core.ui.util.detailThumbGenerator
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo

@Composable
fun GalleryDetailHeaderInfoCard(
    detail: GalleryDetail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = detail.run {
    Card(
        onClick = onClick,
        modifier = modifier.width(IntrinsicSize.Max),
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelMedium) {
            Row(modifier = Modifier.padding(8.dp)) {
                Text(text = language.orEmpty())
                Spacer(modifier = Modifier.width(16.dp).weight(1f))
                Text(text = size.orEmpty())
            }
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = stringResource(id = R.string.favored_times, favoriteCount),
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(modifier = Modifier.width(16.dp).weight(1f))
                Text(
                    text = pluralStringResource(id = R.plurals.page_count, pages, pages),
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Text(
                text = posted.orEmpty(),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
            )
        }
    }
}

@Composable
context(_: SharedTransitionScope, _: TransitionsVisibilityScope)
fun GalleryDetailHeaderCard(
    info: GalleryInfo,
    onInfoCardClick: () -> Unit,
    onUploaderChipClick: () -> Unit,
    onBlockUploaderIconClick: () -> Unit,
    onCategoryChipClick: () -> Unit,
    modifier: Modifier = Modifier,
) = ElevatedCard(modifier = modifier) {
    Row {
        with(detailThumbGenerator) {
            EhThumbCard(
                key = remember(info.gid) { info },
                modifier = Modifier.size(
                    dimensionResource(id = com.hippo.ehviewer.R.dimen.gallery_detail_thumb_width),
                    dimensionResource(id = com.hippo.ehviewer.R.dimen.gallery_detail_thumb_height),
                ),
            )
        }
        Spacer(modifier = Modifier.weight(0.5F))
        Column(
            modifier = Modifier.height(dimensionResource(id = com.hippo.ehviewer.R.dimen.gallery_detail_thumb_height)),
            horizontalAlignment = Alignment.End,
        ) {
            (info as? GalleryDetail)?.let {
                GalleryDetailHeaderInfoCard(
                    detail = it,
                    onClick = onInfoCardClick,
                    modifier = Modifier.padding(top = 8.dp, end = dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                )
            }
            Spacer(modifier = Modifier.weight(1F))
            val categoryText = EhUtils.getCategory(info.category).uppercase()
            AssistChip(
                onClick = onCategoryChipClick,
                label = { Text(text = categoryText, overflow = TextOverflow.Visible, softWrap = false, maxLines = 1) },
                modifier = Modifier.padding(horizontal = dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Label,
                        contentDescription = null,
                    )
                },
            )
            val uploaderText = info.uploader.orEmpty()
            AssistChip(
                onClick = onUploaderChipClick,
                label = { Text(text = uploaderText, overflow = TextOverflow.Visible, softWrap = false, maxLines = 1) },
                modifier = Modifier.padding(horizontal = dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.NoAccounts,
                        contentDescription = null,
                        modifier = Modifier.clickable(onClick = onBlockUploaderIconClick),
                    )
                },
            )
        }
    }
}

@Composable
fun GalleryDetailErrorTip(error: String, onClick: () -> Unit) = Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
) {
    Icon(
        imageVector = EhIcons.Big.Default.SadAndroid,
        contentDescription = null,
        modifier = Modifier.clickable(onClick = onClick),
    )
    Spacer(modifier = Modifier.size(8.dp))
    Text(
        text = error,
        modifier = Modifier.widthIn(max = 228.dp),
    )
}
