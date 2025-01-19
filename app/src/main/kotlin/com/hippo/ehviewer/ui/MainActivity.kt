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
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_NONE
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerState2
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalSideDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberDrawerState2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.rememberNavController
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.app_link_not_verified_message
import com.ehviewer.core.common.app_link_not_verified_title
import com.ehviewer.core.common.clipboard_gallery_url_snack_action
import com.ehviewer.core.common.clipboard_gallery_url_snack_message
import com.ehviewer.core.common.dont_show_again
import com.ehviewer.core.common.downloads
import com.ehviewer.core.common.error_cannot_parse_the_url
import com.ehviewer.core.common.favourite
import com.ehviewer.core.common.history
import com.ehviewer.core.common.homepage
import com.ehviewer.core.common.invalid_download_location
import com.ehviewer.core.common.metered_network_warning
import com.ehviewer.core.common.no_network
import com.ehviewer.core.common.open_settings
import com.ehviewer.core.common.settings
import com.ehviewer.core.common.subscription
import com.ehviewer.core.common.toplist
import com.ehviewer.core.common.update_failed
import com.ehviewer.core.common.waring
import com.ehviewer.core.common.whats_hot
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.Subscriptions
import com.hippo.ehviewer.ui.destinations.DownloadScreenDestination
import com.hippo.ehviewer.ui.destinations.DownloadsScreenDestination
import com.hippo.ehviewer.ui.destinations.FavouritesScreenDestination
import com.hippo.ehviewer.ui.destinations.HistoryScreenDestination
import com.hippo.ehviewer.ui.destinations.HomePageScreenDestination
import com.hippo.ehviewer.ui.destinations.ProgressScreenDestination
import com.hippo.ehviewer.ui.destinations.SettingsScreenDestination
import com.hippo.ehviewer.ui.destinations.SignInScreenDestination
import com.hippo.ehviewer.ui.destinations.SubscriptionScreenDestination
import com.hippo.ehviewer.ui.destinations.ToplistScreenDestination
import com.hippo.ehviewer.ui.destinations.WhatshotScreenDestination
import com.hippo.ehviewer.ui.screen.asDst
import com.hippo.ehviewer.ui.screen.asDstWith
import com.hippo.ehviewer.ui.screen.navWithUrl
import com.hippo.ehviewer.ui.settings.showNewVersion
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LabeledCheckbox
import com.hippo.ehviewer.ui.tools.LocalWindowSizeClass
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.calculateFraction
import com.hippo.ehviewer.util.displayString
import com.hippo.ehviewer.util.getParcelableExtraCompat
import com.hippo.ehviewer.util.getUrlFromClipboard
import com.hippo.ehviewer.util.isAtLeastQ
import com.hippo.ehviewer.util.isAtLeastS
import com.hippo.ehviewer.util.sha1
import com.hippo.files.isDirectory
import com.hippo.files.toOkioPath
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.utils.currentDestinationAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import splitties.systemservices.clipboardManager
import splitties.systemservices.connectivityManager

private val navItems = arrayOf<Triple<Direction, StringResource, ImageVector>>(
    Triple(HomePageScreenDestination, Res.string.homepage, Icons.Default.Home),
    Triple(SubscriptionScreenDestination, Res.string.subscription, EhIcons.Default.Subscriptions),
    Triple(WhatshotScreenDestination, Res.string.whats_hot, Icons.Default.Whatshot),
    Triple(ToplistScreenDestination, Res.string.toplist, Icons.Default.FormatListNumbered),
    Triple(FavouritesScreenDestination, Res.string.favourite, Icons.Default.Favorite),
    Triple(HistoryScreenDestination, Res.string.history, Icons.Default.History),
    Triple(DownloadsScreenDestination, Res.string.downloads, Icons.Default.Download),
    Triple(SettingsScreenDestination, Res.string.settings, Icons.Default.Settings),
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
            val configuration = LocalConfiguration.current
            val navDrawerState = rememberDrawerState(DrawerValue.Closed)
            val sideSheetState = rememberDrawerState2(DrawerValue.Closed)
            val snackbarState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()
            val navigator = navController.rememberDestinationsNavigator()
            fun closeDrawer(callback: () -> Unit = {}) = scope.launch {
                navDrawerState.close()
                callback()
            }

            suspend fun DialogState.checkDownloadLocation() {
                val valid = withIOContext { downloadLocation.isDirectory }
                if (!valid) {
                    awaitConfirmationOrCancel(
                        confirmText = Res.string.open_settings,
                        title = Res.string.waring,
                        showCancelButton = false,
                    ) {
                        Text(
                            text = stringResource(Res.string.invalid_download_location),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    navigator.navigate(DownloadScreenDestination)
                }
            }

            val hasNetwork = remember { connectivityManager.activeNetwork != null }
            if (!AppConfig.isBenchmark) {
                val noNetwork = stringResource(Res.string.no_network)
                LaunchedEffect(Unit) {
                    runCatching { checkDownloadLocation() }
                    runCatching { checkAppLinkVerify() }
                    if (hasNetwork) {
                        runSuspendCatching {
                            withIOContext {
                                AppUpdater.checkForUpdate()?.let {
                                    showNewVersion(this@MainActivity, it)
                                }
                            }
                        }.onFailure {
                            snackbarState.showSnackbar(getString(Res.string.update_failed, it.displayString()))
                        }
                    } else {
                        snackbarState.showSnackbar(noNetwork)
                    }
                }
            }

            val cannotParse = stringResource(Res.string.error_cannot_parse_the_url)
            LaunchedEffect(Unit) {
                intentFlow.collect { intent ->
                    when (intent.action) {
                        Intent.ACTION_VIEW -> {
                            val uri = intent.data ?: return@collect
                            when (uri.scheme) {
                                SCHEME_CONTENT, SCHEME_FILE -> {
                                    navigator.navToReader(uri)
                                }

                                else -> {
                                    val url = uri.toString()
                                    if (!navigator.navWithUrl(url)) {
                                        val new = awaitInputText(initial = url, title = cannotParse)
                                        addTextToClipboard(new)
                                    }
                                }
                            }
                        }
                        Intent.ACTION_SEND -> {
                            val type = intent.type
                            if ("text/plain" == type) {
                                val keyword = intent.getStringExtra(Intent.EXTRA_TEXT)
                                if (keyword != null && !navigator.navWithUrl(keyword)) {
                                    navigator.navigate(ListUrlBuilder(mKeyword = keyword).asDst())
                                }
                            } else if (type != null && type.startsWith("image/")) {
                                val uri = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
                                if (null != uri) {
                                    val hash = withIOContext { uri.toOkioPath().sha1() }
                                    navigator.navigate(
                                        ListUrlBuilder(
                                            mode = ListUrlBuilder.MODE_IMAGE_SEARCH,
                                            hash = hash,
                                        ).asDst(),
                                    )
                                }
                            }
                        }
                        DownloadService.ACTION_START_DOWNLOADSCENE -> {
                            val args = intent.getBundleExtra(DownloadService.ACTION_START_DOWNLOADSCENE_ARGS)!!
                            if (args.getString(DownloadService.KEY_ACTION) == DownloadService.ACTION_CLEAR_DOWNLOAD_SERVICE) {
                                DownloadService.clear()
                            }
                            navigator.navigate(DownloadsScreenDestination)
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                tipFlow.collectLatest {
                    snackbarState.showSnackbar(it)
                }
            }
            val warning = stringResource(Res.string.metered_network_warning)
            val settings = stringResource(Res.string.settings)
            val checkMeteredNetwork by Settings.meteredNetworkWarning.collectAsState()
            if (checkMeteredNetwork) {
                LaunchedEffect(Unit) {
                    if (connectivityManager.isActiveNetworkMetered) {
                        if (isAtLeastQ) {
                            val ret = snackbarState.showSnackbar(warning, settings, true)
                            if (ret == SnackbarResult.ActionPerformed) {
                                val panelIntent = Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                                startActivity(panelIntent)
                            }
                        } else {
                            snackbarState.showSnackbar(warning)
                        }
                    }
                }
            }
            val snackMessage = stringResource(Res.string.clipboard_gallery_url_snack_message)
            val snackAction = stringResource(Res.string.clipboard_gallery_url_snack_action)
            LifecycleResumeEffect(scope) {
                val job = scope.launch {
                    delay(300)
                    val text = clipboardManager.getUrlFromClipboard(applicationContext)
                    val hashCode = text?.hashCode() ?: 0
                    if (text != null && hashCode != 0 && Settings.clipboardTextHashCode != hashCode) {
                        val result1 = GalleryDetailUrlParser.parse(text, false)
                        var launch: (() -> Unit)? = null
                        if (result1 != null) {
                            launch = { navigator.navigate(result1.gid asDstWith result1.token) }
                        }
                        val result2 = GalleryPageUrlParser.parse(text, false)
                        if (result2 != null) {
                            launch = { navigator.navigate(ProgressScreenDestination(result2.gid, result2.pToken, result2.page)) }
                        }
                        launch?.let {
                            val ret = snackbarState.showSnackbar(snackMessage, snackAction, true)
                            if (ret == SnackbarResult.ActionPerformed) it()
                        }
                    }
                    Settings.clipboardTextHashCode = hashCode
                }
                onPauseOrDispose { job.cancel() }
            }
            val currentDestination by navController.currentDestinationAsState()
            val drawerHandle = remember { mutableStateListOf<Int>() }
            var snackbarFabPadding by remember { mutableStateOf(0.dp) }
            val drawerEnabled = drawerHandle.isNotEmpty()
            val density = LocalDensity.current
            val adaptiveInfo = currentWindowAdaptiveInfo()
            val needSignIn by Settings.needSignIn.collectAsState()
            CompositionLocalProvider(
                LocalNavDrawerState provides navDrawerState,
                LocalSideSheetState provides sideSheetState,
                LocalDrawerHandle provides drawerHandle,
                LocalSnackBarHostState provides snackbarState,
                LocalSnackBarFabPadding provides animateDpAsState(snackbarFabPadding, label = "SnackbarFabPadding"),
                LocalWindowSizeClass provides adaptiveInfo.windowSizeClass,
            ) {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarState,
                            modifier = Modifier.onGloballyPositioned {
                                with(density) {
                                    snackbarFabPadding = it.size.height.toDp()
                                }
                            },
                        )
                    },
                ) {
                    var minOffset by remember {
                        mutableFloatStateOf(-with(density) { DrawerDefaults.MaximumDrawerWidth.toPx() })
                    }
                    ModalNavigationDrawer(
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerState = navDrawerState,
                                modifier = Modifier.widthIn(max = (configuration.screenWidthDp - 56).dp)
                                    .onSizeChanged { minOffset = -it.width.toFloat() },
                                windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Start),
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier.verticalScroll(scrollState)
                                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.sadpanda_low_poly),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        contentScale = ContentScale.FillWidth,
                                    )
                                    navItems.forEach { (direction, stringId, icon) ->
                                        NavigationDrawerItem(
                                            label = {
                                                Text(text = stringResource(stringId))
                                            },
                                            selected = currentDestination === direction,
                                            onClick = {
                                                navigator.navigate(direction)
                                                closeDrawer()
                                            },
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            icon = {
                                                Icon(imageVector = icon, contentDescription = null)
                                            },
                                        )
                                    }
                                }
                            }
                        },
                        drawerState = navDrawerState,
                        gesturesEnabled = drawerEnabled && sideSheetState.isClosed || navDrawerState.isOpen,
                    ) {
                        val sheet = sideSheet.firstOrNull()
                        val radius by remember {
                            snapshotFlow {
                                val step = calculateFraction(minOffset, 0f, navDrawerState.currentOffset)
                                with(density) { lerp(0, 10, step).dp.toPx() }
                            }
                        }.collectAsState(0f)
                        ModalSideDrawer(
                            drawerContent = {
                                if (sheet != null) {
                                    ModalDrawerSheet(
                                        modifier = Modifier.widthIn(max = (configuration.screenWidthDp - 112).dp),
                                        drawerShape = ShapeDefaults.Large.copy(topEnd = CornerSize(0), bottomEnd = CornerSize(0)),
                                        windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.End),
                                    ) {
                                        sheet(sideSheetState)
                                    }
                                }
                            },
                            modifier = Modifier.graphicsLayer {
                                if (radius != 0f) {
                                    renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                                    shape = RectangleShape
                                    clip = true
                                }
                            },
                            drawerState = sideSheetState,
                            gesturesEnabled = sheet != null && drawerEnabled,
                        ) {
                            SharedTransitionLayout {
                                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                                    val start = when {
                                        needSignIn -> SignInScreenDestination
                                        hasNetwork -> StartDestination
                                        else -> DownloadsScreenDestination
                                    }
                                    DestinationsNavHost(
                                        navGraph = NavGraphs.root,
                                        start = start,
                                        defaultTransitions = rememberEhNavAnim(),
                                        navController = navController,
                                    )
                                }
                            }
                        }
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
                awaitConfirmationOrCancel(
                    confirmText = Res.string.open_settings,
                    title = Res.string.app_link_not_verified_title,
                    onCancelButtonClick = {
                        if (checked) Settings.appLinkVerifyTip = true
                    },
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.app_link_not_verified_message),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LabeledCheckbox(
                            modifier = Modifier.fillMaxWidth(),
                            checked = checked,
                            onCheckedChange = { checked = it },
                            label = stringResource(Res.string.dont_show_again),
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
                } catch (_: Throwable) {
                    val intent = Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName"),
                    )
                    startActivity(intent)
                }
            }
        }
    }

    fun showTip(message: String, useToast: Boolean = false) {
        if (useToast || !tipFlow.tryEmit(message)) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun showTip(@StringRes id: Int, useToast: Boolean = false) = showTip(getString(id), useToast)

    suspend fun showTip(res: StringResource, useToast: Boolean = false) = showTip(getString(res), useToast)

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        shareUrl?.let { outContent?.webUri = Uri.parse(shareUrl) }
    }
}

val LocalNavDrawerState = compositionLocalOf<DrawerState> { error("CompositionLocal LocalNavDrawerState not present!") }
val LocalSideSheetState = compositionLocalOf<DrawerState2> { error("CompositionLocal LocalSideSheetState not present!") }
val LocalDrawerHandle = compositionLocalOf<SnapshotStateList<Int>> { error("CompositionLocal LocalDrawerHandle not present!") }
val LocalSnackBarHostState = compositionLocalOf<SnackbarHostState> { error("CompositionLocal LocalSnackBarHostState not present!") }
val LocalSnackBarFabPadding = compositionLocalOf<State<Dp>> { error("CompositionLocal LocalSnackBarFabPadding not present!") }
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> { error("CompositionLocal LocalSharedTransitionScope not present!") }

@Composable
fun DrawerHandle(enabled: Boolean) {
    if (enabled) {
        val current = currentCompositeKeyHash
        val handle = LocalDrawerHandle.current
        DisposableEffect(current) {
            handle.add(current)
            onDispose {
                handle.remove(current)
            }
        }
    }
}
