package com.hippo.ehviewer.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.thumbUrl
import com.hippo.ehviewer.ui.i18n.Strings
import com.hippo.ehviewer.ui.screen.navWithUrl
import com.hippo.ehviewer.util.addTextToClipboard
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private const val INDEX_URL = 2
private const val INDEX_PARENT = 9

context(Strings)
fun GalleryDetail.content() = arrayOf(
    keyGid to "$gid",
    keyToken to token,
    keyUrl to EhUrl.getGalleryDetailUrl(gid, token),
    keyTitle to title,
    keyTitleJpn to titleJpn,
    keyThumb to thumbUrl,
    keyCategory to EhUtils.getCategory(category),
    keyUploader to uploader,
    keyPosted to posted,
    keyParent to parent,
    keyVisible to visible,
    keyLanguage to language,
    keyPages to "$pages",
    keySize to size,
    keyFavoriteCount to "$favoriteCount",
    keyFavorited to "${(favoriteSlot > LOCAL_FAVORITED)}",
    keyRatingCount to "$ratingCount",
    keyRating to "$rating",
    keyTorrents to "$torrentCount",
    keyTorrentUrl to torrentUrl,
    keyFavoriteName to favoriteName,
)

context(Context, DestinationsNavigator, Strings)
@Composable
fun GalleryInfoBottomSheet(detail: GalleryDetail) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = galleryInfo,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleLarge,
        )
        val data = remember(detail) { detail.content() }
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            LazyColumn(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues()) {
                itemsIndexed(data) { index, (key, content) ->
                    Row(
                        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.keyline_margin)).clickable {
                            if (index == INDEX_PARENT) {
                                if (content != null) {
                                    navWithUrl(content)
                                }
                            } else {
                                addTextToClipboard(content, true)
                                if (index == INDEX_URL) {
                                    // Save it to avoid detect the gallery
                                    Settings.clipboardTextHashCode = data[index].hashCode()
                                }
                            }
                        }.fillMaxWidth(),
                    ) {
                        Text(key, modifier = Modifier.width(90.dp).padding(8.dp))
                        Text(content.orEmpty(), modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}
