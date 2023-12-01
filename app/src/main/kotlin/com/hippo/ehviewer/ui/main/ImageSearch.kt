package com.hippo.ehviewer.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.tools.LabeledCheckbox

@Composable
fun ImageSearch(
    imagePath: String,
    onSelectImage: () -> Unit,
    uss: Boolean,
    onUssChecked: (Boolean) -> Unit,
    osc: Boolean,
    onOscChecked: (Boolean) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    AnimatedVisibility(visible = imagePath.isNotBlank(), modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Card {
            val maxSize = dimensionResource(id = R.dimen.image_search_max_size)
            AsyncImage(
                model = imagePath,
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
    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        Row {
            LabeledCheckbox(
                checked = uss,
                onCheckedChange = onUssChecked,
                label = stringResource(id = R.string.search_uss),
            )
            LabeledCheckbox(
                checked = osc,
                onCheckedChange = onOscChecked,
                label = stringResource(id = R.string.search_osc),
            )
        }
    }
}
