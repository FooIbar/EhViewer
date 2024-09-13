package eu.kanade.tachiyomi.ui.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import moe.tarsin.kt.andThen

@Composable
fun ReaderPageSheetMeta(
    retry: () -> Unit,
    retryOrigin: () -> Unit,
    share: () -> Unit,
    copy: () -> Unit,
    save: () -> Unit,
    saveTo: () -> Unit,
    showAds: (() -> Unit)?,
    dismiss: () -> Unit,
) {
    @Composable
    fun Item(icon: ImageVector, @StringRes text: Int, onClick: () -> Unit) = Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick andThen dismiss).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.size(32.dp))
        Text(text = stringResource(id = text))
    }
    Column(
        modifier = Modifier.fillMaxSize() // Workaround for https://issuetracker.google.com/341594885
            .verticalScroll(rememberScrollState()).navigationBarsPadding(),
    ) {
        showAds?.let { Item(icon = Icons.Default.Visibility, text = R.string.show_blocked_image, onClick = it) }
        Item(icon = Icons.Default.Refresh, text = R.string.refresh, onClick = retry)
        Item(icon = Icons.Default.Refresh, text = R.string.refresh_original, onClick = retryOrigin)
        Item(icon = Icons.Default.Share, text = R.string.action_share, onClick = share)
        Item(icon = Icons.Default.FileCopy, text = R.string.action_copy, onClick = copy)
        Item(icon = Icons.Default.Save, text = R.string.action_save, onClick = save)
        Item(icon = Icons.Default.Save, text = R.string.action_save_to, onClick = saveTo)
    }
}
