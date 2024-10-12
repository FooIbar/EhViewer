package com.hippo.ehviewer.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.login.refreshAccountInfo

@Composable
fun AvatarIcon(onClick: () -> Unit) {
    val hasSignedIn by Settings.hasSignedIn.collectAsState()
    val avatar by Settings.avatar.collectAsState()
    IconButton(onClick = onClick) {
        if (hasSignedIn) {
            AnimatedContent(targetState = avatar == null) { needRefresh ->
                if (needRefresh) {
                    LaunchedEffect(Unit) {
                        refreshAccountInfo()
                    }
                    Icon(imageVector = Icons.Default.NoAccounts, contentDescription = null)
                } else {
                    AsyncImage(
                        model = avatar,
                        contentDescription = null,
                        modifier = Modifier.clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        } else {
            Icon(imageVector = Icons.Default.NoAccounts, contentDescription = null)
        }
    }
}
