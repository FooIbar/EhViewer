package com.hippo.ehviewer.ui.screen

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_IMAGE_SEARCH
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.ImageSearch
import com.hippo.ehviewer.ui.main.plus
import com.hippo.ehviewer.ui.tools.snackBarPadding
import com.hippo.ehviewer.util.pickVisualMedia
import com.hippo.ehviewer.util.sha1
import com.hippo.files.toOkioPath
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.launch

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.ImageSearchScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val selectImageFirst = stringResource(R.string.select_image_first)
    val marginH = dimensionResource(id = R.dimen.gallery_list_margin_h)
    val marginV = dimensionResource(id = R.dimen.gallery_list_margin_v)
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.image_search)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val uri = imageUri
                    if (uri != null) {
                        launchUI {
                            navigate(
                                ListUrlBuilder(
                                    mode = MODE_IMAGE_SEARCH,
                                    hash = withIOContext { uri.toOkioPath().sha1() },
                                ).asDst(),
                            )
                        }
                    } else {
                        launch { showSnackbar(selectImageFirst) }
                    }
                },
                modifier = Modifier.snackBarPadding(),
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            }
        },
    ) { contentPadding ->
        ElevatedCard(
            modifier = Modifier.padding(
                paddingValues = contentPadding + PaddingValues(marginH, marginV),
            ).padding(
                vertical = dimensionResource(id = R.dimen.search_layout_margin_v),
            ),
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = dimensionResource(id = R.dimen.search_category_padding_h),
                    vertical = dimensionResource(id = R.dimen.search_category_padding_v),
                ).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.search_image),
                    modifier = Modifier.height(dimensionResource(id = R.dimen.search_category_title_height)),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                ImageSearch(
                    image = imageUri,
                    onSelectImage = {
                        launch { imageUri = pickVisualMedia(ImageOnly) }
                    },
                )
            }
        }
    }
}
