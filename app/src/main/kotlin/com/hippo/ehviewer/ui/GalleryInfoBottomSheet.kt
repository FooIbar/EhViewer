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
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.favorite_name
import com.ehviewer.core.common.gallery_info
import com.ehviewer.core.common.key_category
import com.ehviewer.core.common.key_favorite_count
import com.ehviewer.core.common.key_favorited
import com.ehviewer.core.common.key_gid
import com.ehviewer.core.common.key_language
import com.ehviewer.core.common.key_pages
import com.ehviewer.core.common.key_parent
import com.ehviewer.core.common.key_posted
import com.ehviewer.core.common.key_rating
import com.ehviewer.core.common.key_rating_count
import com.ehviewer.core.common.key_size
import com.ehviewer.core.common.key_thumb
import com.ehviewer.core.common.key_title
import com.ehviewer.core.common.key_title_jpn
import com.ehviewer.core.common.key_token
import com.ehviewer.core.common.key_torrent_url
import com.ehviewer.core.common.key_torrents
import com.ehviewer.core.common.key_uploader
import com.ehviewer.core.common.key_url
import com.ehviewer.core.common.key_visible
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.thumbUrl
import com.hippo.ehviewer.ui.screen.navWithUrl
import com.hippo.ehviewer.util.addTextToClipboard
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.jetbrains.compose.resources.stringResource

private const val INDEX_URL = 2
private const val INDEX_PARENT = 9

private fun GalleryDetail.content() = arrayOf(
    Res.string.key_gid to "$gid",
    Res.string.key_token to token,
    Res.string.key_url to EhUrl.getGalleryDetailUrl(gid, token),
    Res.string.key_title to title,
    Res.string.key_title_jpn to titleJpn,
    Res.string.key_thumb to thumbUrl,
    Res.string.key_category to EhUtils.getCategory(category),
    Res.string.key_uploader to uploader,
    Res.string.key_posted to posted,
    Res.string.key_parent to parent,
    Res.string.key_visible to visible,
    Res.string.key_language to language,
    Res.string.key_pages to pages.toString(),
    Res.string.key_size to size,
    Res.string.key_favorite_count to favoriteCount.toString(),
    Res.string.key_favorited to (favoriteSlot > LOCAL_FAVORITED).toString(),
    Res.string.key_rating_count to ratingCount.toString(),
    Res.string.key_rating to rating.toString(),
    Res.string.key_torrents to torrentCount.toString(),
    Res.string.key_torrent_url to torrentUrl,
    Res.string.favorite_name to favoriteName,
)

context(Context, DestinationsNavigator)
@Composable
fun GalleryInfoBottomSheet(detail: GalleryDetail) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.gallery_info),
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
                        Text(stringResource(key), modifier = Modifier.width(90.dp).padding(8.dp))
                        Text(content.orEmpty(), modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}
