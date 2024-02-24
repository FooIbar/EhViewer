package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.displayString

@Composable
fun LoadStateIndicator(
    modifier: Modifier = Modifier,
    state: LoadState,
    retry: () -> Unit,
) = when (state) {
    is LoadState.Loading -> {
        LinearProgressIndicator(modifier = modifier)
    }

    is LoadState.Error -> {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = state.error.displayString())
            Button(onClick = retry) {
                Text(text = stringResource(id = R.string.action_retry))
            }
        }
    }

    is LoadState.NotLoading -> Unit
}
