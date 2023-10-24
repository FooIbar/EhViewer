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
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.Settings.launchPage
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.databinding.ActivityMainBinding
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.image.Image.Companion.decodeBitmap
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.scene.GalleryDetailScene
import com.hippo.ehviewer.ui.scene.GalleryListScene.Companion.toStartArgs
import com.hippo.ehviewer.ui.scene.ProgressScene
import com.hippo.ehviewer.ui.scene.navAnimated
import com.hippo.ehviewer.ui.scene.navWithUrl
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.getParcelableExtraCompat
import com.hippo.ehviewer.util.getUrlFromClipboard
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import splitties.systemservices.clipboardManager
import splitties.systemservices.connectivityManager

class MainActivity : EhActivity() {
    private lateinit var navController: NavController

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
            if (!handleIntent(intent)) {
                if (intent != null && Intent.ACTION_VIEW == intent.action) {
                    if (intent.data != null) {
                        val url = intent.data.toString()
                        EditTextDialogBuilder(this@MainActivity, url, "")
                            .setTitle(R.string.error_cannot_parse_the_url)
                            .setPositiveButton(android.R.string.copy) { _: DialogInterface?, _: Int ->
                                this@MainActivity.addTextToClipboard(
                                    url,
                                    false,
                                )
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
                    navController.navAnimated(
                        R.id.galleryListScene,
                        builder.toStartArgs(),
                    )
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
                            navController.navAnimated(
                                R.id.galleryListScene,
                                builder.toStartArgs(),
                            )
                            return true
                        }
                    }
                }
            }

            DownloadService.ACTION_START_DOWNLOADSCENE -> {
                val args = intent.getBundleExtra(DownloadService.ACTION_START_DOWNLOADSCENE_ARGS)
                navController.navAnimated(R.id.nav_downloads, args)
            }
        }

        return false
    }

    var drawerLocked by mutableStateOf(false)
    private var openDrawer = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val items = listOf(
        Triple(R.id.nav_homepage, R.string.homepage, R.drawable.v_homepage_black_x24),
        Triple(R.id.nav_subscription, R.string.subscription, R.drawable.v_eh_subscription_black_x24),
        Triple(R.id.nav_whats_hot, R.string.whats_hot, R.drawable.v_fire_black_x24),
        Triple(R.id.nav_toplist, R.string.toplist, R.drawable.ic_baseline_format_list_numbered_24),
        Triple(R.id.nav_favourite, R.string.favourite, R.drawable.v_heart_x24),
        Triple(R.id.nav_history, R.string.history, R.drawable.v_history_black_x24),
        Triple(R.id.nav_downloads, R.string.downloads, R.drawable.v_download_x24),
        Triple(R.id.nav_settings, R.string.settings, R.drawable.v_settings_black_x24),
    )
    private var selectedItem by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setMD3Content {
            val scope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            scope.launch {
                openDrawer.collect {
                    drawerState.open()
                }
            }
            BackHandler(drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }
            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet(
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
                            items.forEach { (id, stringId, drawableId) ->
                                NavigationDrawerItem(
                                    label = {
                                        Text(text = stringResource(id = stringId))
                                    },
                                    selected = id == selectedItem,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (id != selectedItem) {
                                            navController.navigate(id)
                                            selectedItem = id
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    icon = {
                                        Icon(painter = painterResource(id = drawableId), contentDescription = null)
                                    },
                                )
                            }
                        }
                    }
                },
                drawerState = drawerState,
                gesturesEnabled = !drawerLocked,
            ) {
                AndroidViewBinding(factory = ActivityMainBinding::inflate) {
                    val navHostFragment = fragmentContainer.getFragment<NavHostFragment>()
                    navController = navHostFragment.navController.apply {
                        graph = navInflater.inflate(R.navigation.nav_graph).apply {
                            check(launchPage in 0..3)
                            val id = items[launchPage].first
                            setStartDestination(id)
                            selectedItem = id
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
            if (Settings.meteredNetworkWarning) {
                checkMeteredNetwork()
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
                    findViewById(R.id.snackbar),
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
        val text = clipboardManager.getUrlFromClipboard(this)
        val hashCode = text?.hashCode() ?: 0
        if (text != null && hashCode != 0 && Settings.clipboardTextHashCode != hashCode) {
            val result1 = GalleryDetailUrlParser.parse(text, false)
            var launch: (() -> Unit)? = null
            if (result1 != null) {
                val args = Bundle()
                args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN)
                args.putLong(GalleryDetailScene.KEY_GID, result1.gid)
                args.putString(GalleryDetailScene.KEY_TOKEN, result1.token)
                launch = { navController.navAnimated(R.id.galleryDetailScene, args) }
            }
            val result2 = GalleryPageUrlParser.parse(text, false)
            if (result2 != null) {
                val args = Bundle()
                args.putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN)
                args.putLong(ProgressScene.KEY_GID, result2.gid)
                args.putString(ProgressScene.KEY_PTOKEN, result2.pToken)
                args.putInt(ProgressScene.KEY_PAGE, result2.page)
                launch = { navController.navAnimated(R.id.progressScene, args) }
            }
            launch?.let {
                withUIContext {
                    val snackbar = Snackbar.make(
                        findViewById(R.id.snackbar),
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

    fun addAboveSnackView(view: View) {
//        binding.snackbar.addView(view)
    }

    fun removeAboveSnackView(view: View) {
//        binding.snackbar.removeView(view)
    }

    fun openDrawer() {
        openDrawer.tryEmit(Unit)
    }

    fun clearNavCheckedItem() {
        selectedItem = 0
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
                findViewById(R.id.snackbar),
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

    var mShareUrl: String? = null
    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        mShareUrl?.let { outContent?.webUri = Uri.parse(mShareUrl) }
    }
}
