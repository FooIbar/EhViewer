package com.hippo.ehviewer.ui.main

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hippo.ehviewer.R

@Composable
fun ImageSearch(
    image: Uri?,
    onSelectImage: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    AnimatedVisibility(visible = image != null, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Card {
            val maxSize = dimensionResource(id = R.dimen.image_search_max_size)
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.sizeIn(maxWidth = maxSize, maxHeight = maxSize),
            )
        }
    }
    FilledTonalButton(
        onClick = onSelectImage,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        Text(text = stringResource(id = R.string.select_image))
    }
}
