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
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_NONE
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.DrawerState2
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SideDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.rememberNavController
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
import com.hippo.ehviewer.image.Image.Companion.decodeBitmap
import com.hippo.ehviewer.ui.destinations.DownloadsScreenDestination
import com.hippo.ehviewer.ui.destinations.FavouritesScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryDetailScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryListScreenDestination
import com.hippo.ehviewer.ui.destinations.HistoryScreenDestination
import com.hippo.ehviewer.ui.destinations.HomePageScreenDestination
import com.hippo.ehviewer.ui.destinations.ProgressScreenDestination
import com.hippo.ehviewer.ui.destinations.SubscriptionScreenDestination
import com.hippo.ehviewer.ui.destinations.ToplistScreenDestination
import com.hippo.ehviewer.ui.destinations.WhatshotScreenDestination
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.scene.TokenArgs
import com.hippo.ehviewer.ui.scene.navWithUrl
import com.hippo.ehviewer.ui.scene.navigate
import com.hippo.ehviewer.ui.settings.showNewVersion
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.LocalTouchSlopProvider
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.getParcelableExtraCompat
import com.hippo.ehviewer.util.getUrlFromClipboard
import com.hippo.ehviewer.util.isAtLeastQ
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.utils.currentDestinationAsState
import eu.kanade.tachiyomi.util.lang.withIOContext
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import splitties.systemservices.clipboardManager
import splitties.systemservices.connectivityManager

private val navItems = arrayOf(
    Triple(HomePageScreenDestination, R.string.homepage, Icons.Default.Home),
    Triple(SubscriptionScreenDestination, R.string.subscription, EhIcons.Default.Subscriptions),
    Triple(WhatshotScreenDestination, R.string.whats_hot, Icons.Default.Whatshot),
    Triple(ToplistScreenDestination, R.string.toplist, Icons.Default.FormatListNumbered),
    Triple(FavouritesScreenDestination, R.string.favourite, Icons.Default.Favorite),
    Triple(HistoryScreenDestination, R.string.history, Icons.Default.History),
    Triple(DownloadsScreenDestination, R.string.downloads, Icons.Default.Download),
)

class MainActivity : EhActivity() {
    private var sideSheet = mutableStateListOf<@Composable ColumnScope.(DrawerState2) -> Unit>()

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

    private fun saveImageToTempFile(uri: Uri): File? {
        val bitmap = runCatching {
            decodeBitmap(uri)
        }.getOrNull() ?: return null
        val temp = AppConfig.createTempFile() ?: return null
        return runCatching {
            FileOutputStream(temp).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            temp
        }.getOrElse {
            it.printStackTrace()
            null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentFlow.tryEmit(intent)
    }

    private var tipFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private var intentFlow = MutableSharedFlow<Intent>(extraBufferCapacity = 4)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setMD3Content {
            val configuration = LocalConfiguration.current
            val dialogState = LocalDialogState.current
            val navDrawerState = rememberDrawerState(DrawerValue.Closed)
            val sideSheetState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()
            fun closeDrawer(callback: () -> Unit = {}) = scope.launch {
                navDrawerState.close()
                callback()
            }

            val cannotParse = stringResource(R.string.error_cannot_parse_the_url)
            LaunchedEffect(Unit) {
                intentFlow.collect {
                    when (intent?.action) {
                        Intent.ACTION_VIEW -> {
                            val url = intent.data?.toString()
                            if (url != null && !navController.navWithUrl(url)) {
                                val new = dialogState.awaitInputText(initial = url, title = cannotParse)
                                addTextToClipboard(new)
                            }
                        }
                        Intent.ACTION_SEND -> {
                            val type = intent.type
                            if ("text/plain" == type) {
                                val keyword = intent.getStringExtra(Intent.EXTRA_TEXT)
                                navController.navigate(GalleryListScreenDestination(ListUrlBuilder(mKeyword = keyword)))
                            } else if (type != null && type.startsWith("image/")) {
                                val uri = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
                                if (null != uri) {
                                    val temp = saveImageToTempFile(uri)
                                    if (null != temp) {
                                        val builder = ListUrlBuilder(
                                            mode = ListUrlBuilder.MODE_IMAGE_SEARCH,
                                            imagePath = temp.path,
                                            isUseSimilarityScan = true,
                                        )
                                        navController.navigate(GalleryListScreenDestination(builder))
                                    }
                                }
                            }
                        }
                        DownloadService.ACTION_START_DOWNLOADSCENE -> {
                            val args = intent.getBundleExtra(DownloadService.ACTION_START_DOWNLOADSCENE_ARGS)
                            navController.navigate(DownloadsScreenDestination)
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                runSuspendCatching {
                    withIOContext {
                        AppUpdater.checkForUpdate()?.let {
                            dialogState.showNewVersion(this@MainActivity, it)
                        }
                    }
                }.onFailure {
                    showTip(getString(R.string.update_failed, ExceptionUtils.getReadableString(it)))
                }
            }
            val snackbarState = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                tipFlow.collectLatest {
                    snackbarState.showSnackbar(it)
                }
            }
            val warning = stringResource(R.string.metered_network_warning)
            val settings = stringResource(R.string.settings)
            val checkMeteredNetwork by Settings.meteredNetworkWarning.collectAsState()
            if (checkMeteredNetwork) {
                LaunchedEffect(Unit) {
                    if (connectivityManager.isActiveNetworkMetered) {
                        if (isAtLeastQ) {
                            val ret = snackbarState.showSnackbar(warning, settings)
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
            val snackMessage = stringResource(R.string.clipboard_gallery_url_snack_message)
            val snackAction = stringResource(R.string.clipboard_gallery_url_snack_action)
            LifecycleResumeEffect {
                scope.launch {
                    delay(300)
                    val text = clipboardManager.getUrlFromClipboard(applicationContext)
                    val hashCode = text?.hashCode() ?: 0
                    if (text != null && hashCode != 0 && Settings.clipboardTextHashCode != hashCode) {
                        val result1 = GalleryDetailUrlParser.parse(text, false)
                        var launch: (() -> Unit)? = null
                        if (result1 != null) {
                            launch = { navController.navigate(GalleryDetailScreenDestination(TokenArgs(result1.gid, result1.token))) }
                        }
                        val result2 = GalleryPageUrlParser.parse(text, false)
                        if (result2 != null) {
                            launch = { navController.navigate(ProgressScreenDestination(result2.gid, result2.pToken, result2.page)) }
                        }
                        launch?.let {
                            val ret = snackbarState.showSnackbar(snackMessage, snackAction)
                            if (ret == SnackbarResult.ActionPerformed) it()
                        }
                    }
                    Settings.clipboardTextHashCode = hashCode
                }
                onPauseOrDispose { }
            }
            val currentDestination by navController.currentDestinationAsState()
            val lockDrawerHandle = remember { mutableStateListOf<Int>() }
            val drawerLocked = lockDrawerHandle.isNotEmpty()
            CompositionLocalProvider(
                LocalNavDrawerState provides navDrawerState,
                LocalSideSheetState provides sideSheetState,
                LocalDrawerLockHandle provides lockDrawerHandle,
            ) {
                Scaffold(snackbarHost = { SnackbarHost(snackbarState) }) {
                    LocalTouchSlopProvider(Settings.touchSlopFactor.toFloat()) {
                        ModalNavigationDrawer(
                            drawerContent = {
                                ModalDrawerSheet(
                                    modifier = Modifier.widthIn(max = (configuration.screenWidthDp - 56).dp),
                                    windowInsets = WindowInsets(0, 0, 0, 0),
                                ) {
                                    val scrollState = rememberScrollState()
                                    Column(
                                        modifier = Modifier.verticalScroll(scrollState).navigationBarsPadding(),
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
                                                    Text(text = stringResource(id = stringId))
                                                },
                                                selected = currentDestination === direction,
                                                onClick = {
                                                    navController.navigate(direction.route)
                                                    closeDrawer()
                                                },
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                icon = {
                                                    Icon(imageVector = icon, contentDescription = null)
                                                },
                                            )
                                        }
                                        NavigationDrawerItem(
                                            label = {
                                                Text(text = stringResource(id = R.string.settings))
                                            },
                                            selected = false,
                                            onClick = {
                                                closeDrawer { startActivity(Intent(applicationContext, ConfigureActivity::class.java)) }
                                            },
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            icon = {
                                                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                                            },
                                        )
                                    }
                                }
                            },
                            drawerState = navDrawerState,
                            gesturesEnabled = !drawerLocked || navDrawerState.isOpen,
                        ) {
                            val sheet = sideSheet.firstOrNull()
                            SideDrawer(
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
                                drawerState = sideSheetState,
                                gesturesEnabled = sheet != null && !drawerLocked,
                            ) {
                                DestinationsNavHost(
                                    navGraph = NavGraphs.root,
                                    startRoute = navItems[Settings.launchPage].first,
                                    engine = rememberNavHostEngine(rootDefaultAnimations = ehNavAnim),
                                    navController = navController,
                                )
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
            checkDownloadLocation()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!Settings.appLinkVerifyTip) {
                    try {
                        checkAppLinkVerify()
                    } catch (ignored: PackageManager.NameNotFoundException) {
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkAppLinkVerify() {
        val manager = getSystemService(DomainVerificationManager::class.java)
        val userState = manager.getDomainVerificationUserState(packageName) ?: return
        val hasUnverified = userState.hostToStateMap.values.any { it == DOMAIN_STATE_NONE }
        if (hasUnverified) {
            BaseDialogBuilder(this)
                .setTitle(R.string.app_link_not_verified_title)
                .setMessage(R.string.app_link_not_verified_message)
                .setPositiveButton(R.string.open_settings) { _: DialogInterface?, _: Int ->
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
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.dont_show_again) { _: DialogInterface?, _: Int ->
                    Settings.appLinkVerifyTip = true
                }
                .show()
        }
    }

    private fun checkDownloadLocation() {
        val uniFile = downloadLocation
        // null == uniFile for first start
        if (uniFile.ensureDir()) {
            return
        }
        BaseDialogBuilder(this)
            .setTitle(R.string.waring)
            .setMessage(R.string.invalid_download_location)
            .setPositiveButton(R.string.get_it, null)
            .show()
    }

    override fun onResume() {
        if (Settings.needSignIn) {
            startActivity(Intent(this, ConfigureActivity::class.java))
        }
        super.onResume()
    }

    fun showTip(@StringRes id: Int, useToast: Boolean = false) {
        showTip(getString(id), useToast)
    }

    fun showTip(message: CharSequence, useToast: Boolean = false) {
        if (useToast || !tipFlow.tryEmit(message.toString())) {
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
val LocalDrawerLockHandle = compositionLocalOf<SnapshotStateList<Int>> { error("CompositionLocal LocalSideSheetState not present!") }

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
