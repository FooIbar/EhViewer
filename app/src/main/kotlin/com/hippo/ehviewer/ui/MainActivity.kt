/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui

import android.annotation.SuppressLint
import android.app.assist.AssistContent
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_NONE
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.Subscriptions
import com.hippo.ehviewer.ui.destinations.DownloadsScreenDestination
import com.hippo.ehviewer.ui.destinations.FavouritesScreenDestination
import com.hippo.ehviewer.ui.destinations.HistoryScreenDestination
import com.hippo.ehviewer.ui.destinations.HomePageScreenDestination
import com.hippo.ehviewer.ui.destinations.SettingsScreenDestination
import com.hippo.ehviewer.ui.destinations.SubscriptionScreenDestination
import com.hippo.ehviewer.ui.destinations.ToplistScreenDestination
import com.hippo.ehviewer.ui.destinations.WhatshotScreenDestination
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LabeledCheckbox
import com.hippo.ehviewer.util.isAtLeastS
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow

private val navItems = arrayOf(
    Triple(HomePageScreenDestination, R.string.homepage, Icons.Default.Home),
    Triple(SubscriptionScreenDestination, R.string.subscription, EhIcons.Default.Subscriptions),
    Triple(WhatshotScreenDestination, R.string.whats_hot, Icons.Default.Whatshot),
    Triple(ToplistScreenDestination, R.string.toplist, Icons.Default.FormatListNumbered),
    Triple(FavouritesScreenDestination, R.string.favourite, Icons.Default.Favorite),
    Triple(HistoryScreenDestination, R.string.history, Icons.Default.History),
    Triple(DownloadsScreenDestination, R.string.downloads, Icons.Default.Download),
    Triple(SettingsScreenDestination, R.string.settings, Icons.Default.Settings),
)

val StartDestination
    get() = navItems[Settings.launchPage].first

class MainActivity : EhActivity() {
    private val sideSheet = mutableStateListOf<@Composable ColumnScope.(DrawerState2) -> Unit>()

    @Composable
    fun ProvideSideSheetContent(content: @Composable ColumnScope.(DrawerState2) -> Unit) {
        DisposableEffect(content) {
            sideSheet.add(0, content)
            onDispose { sideSheet.remove(content) }
        }
    }

    private var shareUrl: String? = null

    @Composable
    fun ProvideAssistContent(url: String) {
        val urlState by rememberUpdatedState(url)
        DisposableEffect(urlState) {
            shareUrl = urlState
            onDispose {
                shareUrl = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentChannel.trySend(intent)
    }

    private val tipFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val intentChannel = Channel<Intent>(capacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val intentFlow = intentChannel.receiveAsFlow()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setMD3Content {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column {
                    var text by remember { mutableStateOf("") }
                    Text(text = text)
                    Button(onClick = { text = isAuthenticationSupported().toString() }) {
                        Text(text = "Click")
                    }
                }
            }
        }
        if (savedInstanceState == null) {
            if (intent.action != Intent.ACTION_MAIN) {
                onNewIntent(intent)
            }
        }
    }

    private suspend fun DialogState.checkAppLinkVerify() {
        if (isAtLeastS && !Settings.appLinkVerifyTip) {
            val manager = getSystemService(DomainVerificationManager::class.java)
            val packageName = packageName
            val userState = manager.getDomainVerificationUserState(packageName) ?: return
            val hasUnverified = userState.hostToStateMap.values.any { it == DOMAIN_STATE_NONE }
            if (hasUnverified) {
                var checked by mutableStateOf(false)
                awaitPermissionOrCancel(
                    confirmText = R.string.open_settings,
                    title = R.string.app_link_not_verified_title,
                    onCancelButtonClick = {
                        if (checked) Settings.appLinkVerifyTip = true
                    },
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_link_not_verified_message),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LabeledCheckbox(
                            modifier = Modifier.fillMaxWidth(),
                            checked = checked,
                            onCheckedChange = { checked = it },
                            label = stringResource(id = R.string.dont_show_again),
                            indication = null,
                        )
                    }
                }
                if (checked) {
                    Settings.appLinkVerifyTip = true
                }
                try {
                    val intent = Intent(
                        android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                        Uri.parse("package:$packageName"),
                    )
                    startActivity(intent)
                } catch (t: Throwable) {
                    val intent = Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName"),
                    )
                    startActivity(intent)
                }
            }
        }
    }

    fun showTip(@StringRes id: Int, useToast: Boolean = false) {
        val message = getString(id)
        if (useToast || !tipFlow.tryEmit(message)) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        shareUrl?.let { outContent?.webUri = Uri.parse(shareUrl) }
    }
}

val LocalNavDrawerState = compositionLocalOf<DrawerState2> { error("CompositionLocal LocalNavDrawerState not present!") }
val LocalSideSheetState = compositionLocalOf<DrawerState2> { error("CompositionLocal LocalSideSheetState not present!") }
val LocalDrawerLockHandle = compositionLocalOf<SnapshotStateList<Int>> { error("CompositionLocal LocalDrawerLockHandle not present!") }
val LocalSnackBarHostState = compositionLocalOf<SnackbarHostState> { error("CompositionLocal LocalSnackBarHostState not present!") }
val LocalSnackBarFabPadding = compositionLocalOf<State<Dp>> { error("CompositionLocal LocalSnackBarFabPadding not present!") }

@Composable
fun LockDrawer(value: Boolean) {
    val updated by rememberUpdatedState(value)
    if (updated) {
        val current = currentCompositeKeyHash
        val handle = LocalDrawerLockHandle.current
        DisposableEffect(current) {
            handle.add(current)
            onDispose {
                handle.remove(current)
            }
        }
    }
}
