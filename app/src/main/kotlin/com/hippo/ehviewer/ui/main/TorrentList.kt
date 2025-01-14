package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.parser.Torrent
import com.hippo.ehviewer.client.parser.format

@Composable
fun TorrentList(
    items: List<Torrent>,
    onItemClick: (Torrent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.torrents)
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        val labelStyle = MaterialTheme.typography.labelLarge
        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) {
                Column(modifier = Modifier.clickable { onItemClick(it) }.minimumInteractiveComponentSize().padding(horizontal = 8.dp)) {
                    Text(
                        text = it.name,
                        modifier = Modifier.basicMarquee(
                            spacing = MarqueeSpacing(16.dp),
                            velocity = 60.dp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = it.posted,
                            modifier = Modifier.weight(2.5f),
                            color = if (it.outdated) MaterialTheme.colorScheme.error else Color.Unspecified,
                            style = labelStyle,
                        )
                        Text(
                            text = it.uploader,
                            modifier = Modifier.weight(2.5f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = labelStyle,
                        )
                        Text(
                            text = it.size,
                            modifier = Modifier.weight(2f),
                            style = labelStyle,
                            textAlign = TextAlign.End,
                        )
                        Text(
                            text = it.format(),
                            modifier = Modifier.weight(3f),
                            style = labelStyle,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}
