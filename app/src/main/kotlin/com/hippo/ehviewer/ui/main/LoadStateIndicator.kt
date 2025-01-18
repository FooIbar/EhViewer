package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.action_retry
import com.hippo.ehviewer.util.displayString
import org.jetbrains.compose.resources.stringResource

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
                Text(text = stringResource(Res.string.action_retry))
            }
        }
    }

    is LoadState.NotLoading -> Unit
}
