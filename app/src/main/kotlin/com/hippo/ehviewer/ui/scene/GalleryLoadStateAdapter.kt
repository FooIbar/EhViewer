package com.hippo.ehviewer.ui.scene

import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.util.ExceptionUtils

class LoadStateViewHolder(
    private val composeView: ComposeView,
    private val retry: () -> Unit,
) : RecyclerView.ViewHolder(composeView) {
    fun bind(loadState: LoadState) {
        composeView.setMD3Content {
            when (loadState) {
                is LoadState.Loading -> {
                    LinearProgressIndicator()
                }

                is LoadState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = ExceptionUtils.getReadableString(loadState.error))
                        Button(onClick = retry) {
                            Text(text = stringResource(id = R.string.action_retry))
                        }
                    }
                }

                is LoadState.NotLoading -> Unit
            }
        }
    }
}

class GalleryLoadStateAdapter(
    private val retry: () -> Unit,
) : LoadStateAdapter<LoadStateViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState) =
        LoadStateViewHolder(ComposeView(parent.context), retry)

    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) =
        holder.bind(loadState)
}
