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
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SideDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.material.snackbar.Snackbar
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_SUBSCRIPTION
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TOPLIST
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_WHATS_HOT
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.Subscriptions
import com.hippo.ehviewer.image.Image.Companion.decodeBitmap
import com.hippo.ehviewer.ui.destinations.DownloadScreenDestination
import com.hippo.ehviewer.ui.destinations.FavouritesScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryDetailScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryListScreenDestination
import com.hippo.ehviewer.ui.destinations.HistoryScreenDestination
import com.hippo.ehviewer.ui.destinations.ProgressScreenDestination
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.scene.TokenArgs
import com.hippo.ehviewer.ui.scene.navWithUrl
import com.hippo.ehviewer.ui.settings.showNewVersion
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.LocalTouchSlopProvider
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.getParcelableExtraCompat
import com.hippo.ehviewer.util.getUrlFromClipboard
import com.hippo.ehviewer.util.set
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.rememberNavHostEngine
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import splitties.systemservices.clipboardManager
import splitties.systemservices.connectivityManager

private val navItems = arrayOf(
    Triple(GalleryListScreenDestination(ListUrlBuilder()), R.string.homepage, Icons.Default.Home),
    Triple(GalleryListScreenDestination(ListUrlBuilder(MODE_SUBSCRIPTION)), R.string.subscription, EhIcons.Default.Subscriptions),
    Triple(GalleryListScreenDestination(ListUrlBuilder(MODE_WHATS_HOT)), R.string.whats_hot, Icons.Default.Whatshot),
    Triple(GalleryListScreenDestination(ListUrlBuilder(MODE_TOPLIST, mKeyword = Settings.recentToplist)), R.string.toplist, Icons.Default.FormatListNumbered),
    Triple(FavouritesScreenDestination, R.string.favourite, Icons.Default.Favorite),
    Triple(HistoryScreenDestination, R.string.history, Icons.Default.History),
    Triple(DownloadScreenDestination, R.string.downloads, Icons.Default.Download),
)

class MainActivity : EhActivity() {
    private lateinit var navController: DestinationsNavigator
    private val isInitializedFlow = MutableStateFlow(false)

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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        lifecycleScope.launchUI {
            isInitializedFlow.first { it }
            if (!handleIntent(intent)) {
                if (intent != null && Intent.ACTION_VIEW == intent.action) {
                    if (intent.data != null) {
                        val url = intent.data.toString()
                        EditTextDialogBuilder(this@MainActivity, url, "")
                            .setTitle(R.string.error_cannot_parse_the_url)
                            .setPositiveButton(android.R.string.copy) { _, _ ->
                                this@MainActivity.addTextToClipboard(url)
                            }
                            .show()
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?): Boolean {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return false
                return navController.navWithUrl(uri.toString())
            }

            Intent.ACTION_SEND -> {
                val type = intent.type
                if ("text/plain" == type) {
                    val builder = ListUrlBuilder()
                    builder.keyword = intent.getStringExtra(Intent.EXTRA_TEXT)
                    navController.navigate(GalleryListScreenDestination(builder))
                    return true
                } else if (type != null && type.startsWith("image/")) {
                    val uri = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
                    if (null != uri) {
                        val temp = saveImageToTempFile(uri)
                        if (null != temp) {
                            val builder = ListUrlBuilder()
                            builder.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                            builder.imagePath = temp.path
                            builder.isUseSimilarityScan = true
                            navController.navigate(GalleryListScreenDestination(builder))
                            return true
                        }
                    }
                }
            }

            DownloadService.ACTION_START_DOWNLOADSCENE -> {
                val args = intent.getBundleExtra(DownloadService.ACTION_START_DOWNLOADSCENE_ARGS)
                navController.navigate(DownloadScreenDestination)
            }
        }

        return false
    }

    var drawerLocked by mutableStateOf(false)
    private var openDrawerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var openSideSheetFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var recomposeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun recompose() {
        recomposeFlow.tryEmit(Unit)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setMD3Content {
            val configuration = LocalConfiguration.current
            val dialogState = LocalDialogState.current
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val recomposeScope = currentRecomposeScope
            val navController = rememberNavController()
            fun isSelected(id: Int) = ::navController.isInitialized && id == navController.currentDestination?.id
            fun closeDrawer(callback: () -> Unit = {}) = scope.launch {
                drawerState.close()
                callback()
            }

            LaunchedEffect(Unit) {
                runSuspendCatching {
                    withIOContext {
                        AppUpdater.checkForUpdate()?.let {
                            dialogState.showNewVersion(this@MainActivity, it)
                        }
                    }
                }.onFailure {
                    showTip(getString(R.string.update_failed, ExceptionUtils.getReadableString(it)), BaseScene.LENGTH_LONG)
                }
            }
            LaunchedEffect(Unit) {
                openDrawerFlow.collectLatest {
                    drawerState.open()
                }
            }
            LaunchedEffect(Unit) {
                recomposeFlow.collect {
                    recomposeScope.invalidate()
                }
            }
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
                                        selected = false,
                                        onClick = {
                                            navController.navigate(direction.route)
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
                    drawerState = drawerState,
                    gesturesEnabled = !drawerLocked || drawerState.isOpen,
                ) {
                    val sheet = sideSheet.firstOrNull()
                    val sideDrawerState = rememberDrawerState(DrawerValue.Closed)
                    LaunchedEffect(Unit) {
                        openSideSheetFlow.collectLatest {
                            sideDrawerState.open()
                        }
                    }
                    SideDrawer(
                        drawerContent = {
                            if (sheet != null) {
                                ModalDrawerSheet(
                                    modifier = Modifier.widthIn(max = (configuration.screenWidthDp - 112).dp),
                                    drawerShape = ShapeDefaults.Large.copy(topEnd = CornerSize(0), bottomEnd = CornerSize(0)),
                                    windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.End),
                                ) {
                                    sheet(sideDrawerState)
                                }
                            }
                        },
                        drawerState = sideDrawerState,
                        gesturesEnabled = sheet != null && !drawerLocked,
                    ) {
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
                            startRoute = GalleryListScreenDestination,
                            engine = rememberNavHostEngine(rootDefaultAnimations = RootNavGraphDefaultAnimations.ACCOMPANIST_FADING),
                            navController = navController,
                        )
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            if (intent.action != Intent.ACTION_MAIN) {
                onNewIntent(intent)
            }
            checkDownloadLocation()
            if (Settings.meteredNetworkWarning) {
                lifecycleScope.launchUI {
                    isInitializedFlow.first { it }
                    checkMeteredNetwork()
                }
            }
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

    private fun checkMeteredNetwork() {
        if (connectivityManager.isActiveNetworkMetered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Snackbar.make(
                    findViewById(R.id.fragment_container),
                    R.string.metered_network_warning,
                    Snackbar.LENGTH_LONG,
                )
                    .setAction(R.string.settings) {
                        val panelIntent =
                            Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                        startActivity(panelIntent)
                    }
                    .show()
            } else {
                showTip(R.string.metered_network_warning, BaseScene.LENGTH_LONG)
            }
        }
    }

    override fun onResume() {
        if (Settings.needSignIn) {
            startActivity(Intent(this, ConfigureActivity::class.java))
        }
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            checkClipboardUrl()
        }
    }

    private suspend fun checkClipboardUrl() {
        isInitializedFlow.first { it }
        val text = clipboardManager.getUrlFromClipboard(this)
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
                withUIContext {
                    val snackbar = Snackbar.make(
                        findViewById(R.id.fragment_container),
                        R.string.clipboard_gallery_url_snack_message,
                        Snackbar.LENGTH_SHORT,
                    )
                    snackbar.setAction(R.string.clipboard_gallery_url_snack_action) {
                        it()
                    }
                    snackbar.show()
                }
            }
        }
        Settings.clipboardTextHashCode = hashCode
    }

    fun openDrawer() {
        openDrawerFlow.tryEmit(Unit)
    }

    fun openSideSheet() {
        openSideSheetFlow.tryEmit(Unit)
    }

    fun showTip(@StringRes id: Int, length: Int, useToast: Boolean = false) {
        showTip(getString(id), length, useToast)
    }

    /**
     * If activity is running, show snack bar, otherwise show toast
     */
    fun showTip(message: CharSequence, length: Int, useToast: Boolean = false) {
        if (!useToast) {
            Snackbar.make(
                findViewById(R.id.fragment_container),
                message,
                if (length == BaseScene.LENGTH_LONG) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT,
            ).show()
        } else {
            Toast.makeText(
                this,
                message,
                if (length == BaseScene.LENGTH_LONG) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        shareUrl?.let { outContent?.webUri = Uri.parse(shareUrl) }
    }
}
