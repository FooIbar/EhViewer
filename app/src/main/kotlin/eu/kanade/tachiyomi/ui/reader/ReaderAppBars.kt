package eu.kanade.tachiyomi.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.main.EhAsyncPreview
import com.hippo.ehviewer.ui.screen.collectPreviewItems
import com.hippo.ehviewer.ui.screen.detailCache
import com.hippo.ehviewer.ui.tools.Await

private val animationSpec = tween<IntOffset>(200)

@Composable
fun ReaderAppBars(
    info: GalleryInfo?,
    visible: Boolean,
    isRtl: Boolean,
    showSeekBar: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
    onClickSettings: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    verticalArrangement = Arrangement.SpaceBetween,
) {
    Spacer(modifier = Modifier.weight(1f))

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = animationSpec),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = animationSpec),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showSeekBar) {
                if (info != null) {
                    Await({ detailCache[info.gid] ?: EhEngine.getGalleryDetail(EhUrl.getGalleryDetailUrl(info.gid, info.token)).also { detailCache[info.gid] = it } }) { detail ->
                        val context = LocalContext.current
                        val data = with(context) { detail.collectPreviewItems(8) }
                        val state = rememberCarouselState { data.itemCount }
                        HorizontalMultiBrowseCarousel(
                            state = state,
                            preferredItemWidth = 96.dp,
                            modifier = Modifier.height(128.dp),
                            minSmallItemWidth = Dp.Hairline,
                            maxSmallItemWidth = Dp.Infinity,
                        ) { index ->
                            val item = data[index]
                            if (item != null) {
                                EhAsyncPreview(
                                    model = item,
                                    modifier = Modifier.fillMaxHeight(),
                                    autoCrop = false,
                                )
                            }
                        }
                    }
                }
                ChapterNavigator(
                    isRtl = isRtl,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onSliderValueChange = onSliderValueChange,
                )
            }
            BottomReaderBar(onClickSettings = onClickSettings)
        }
    }
}
