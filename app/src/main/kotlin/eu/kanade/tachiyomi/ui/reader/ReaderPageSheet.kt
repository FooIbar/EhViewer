package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.action_copy
import com.ehviewer.core.common.action_save
import com.ehviewer.core.common.action_save_to
import com.ehviewer.core.common.action_share
import com.ehviewer.core.common.refresh
import com.ehviewer.core.common.show_blocked_image
import com.ehviewer.core.common.view_original
import moe.tarsin.kt.andThen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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
    fun Item(icon: ImageVector, text: StringResource, onClick: () -> Unit) = Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick andThen dismiss).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.size(32.dp))
        Text(text = stringResource(text))
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        showAds?.let { Item(icon = Icons.Default.Visibility, text = Res.string.show_blocked_image, onClick = it) }
        Item(icon = Icons.Default.Refresh, text = Res.string.refresh, onClick = retry)
        Item(icon = Icons.Default.Visibility, text = Res.string.view_original, onClick = retryOrigin)
        Item(icon = Icons.Default.Share, text = Res.string.action_share, onClick = share)
        Item(icon = Icons.Default.FileCopy, text = Res.string.action_copy, onClick = copy)
        Item(icon = Icons.Default.Save, text = Res.string.action_save, onClick = save)
        Item(icon = Icons.Default.Save, text = Res.string.action_save_to, onClick = saveTo)
    }
}
