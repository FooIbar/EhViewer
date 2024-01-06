package com.hippo.ehviewer.ui.settings

import android.content.DialogInterface
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.core.text.parseAsHtml
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.ui.destinations.FilterScreenDestination
import com.hippo.ehviewer.ui.destinations.MyTagsScreenDestination
import com.hippo.ehviewer.ui.destinations.SignInScreenDestination
import com.hippo.ehviewer.ui.destinations.UConfigScreenDestination
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.screen.popNavigate
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberedAccessor
import com.hippo.ehviewer.util.copyTextToClipboard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withUIContext
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import moe.tarsin.coroutines.runSuspendCatching

@Destination
@Composable
fun EhScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    fun launchSnackBar(content: String) = coroutineScope.launch { snackbarHostState.showSnackbar(content) }
    val signin = EhCookieStore.hasSignedIn()
    val dialogState = LocalDialogState.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_eh)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val guestMode = stringResource(id = R.string.settings_eh_identity_cookies_guest)
        val copiedToClipboard = stringResource(id = R.string.copied_to_clipboard)
        Column(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState()).padding(paddingValues)) {
            Preference(
                title = stringResource(id = R.string.account_name),
                summary = Settings.displayName ?: guestMode,
            ) {
                val eCookies = EhCookieStore.load(Url(EhUrl.HOST_E))
                val exCookies = EhCookieStore.load(Url(EhUrl.HOST_EX))
                var ipbMemberId: String? = null
                var ipbPassHash: String? = null
                var igneous: String? = null
                (eCookies + exCookies).forEach {
                    when (it.name) {
                        EhCookieStore.KEY_IPB_MEMBER_ID -> ipbMemberId = it.value
                        EhCookieStore.KEY_IPB_PASS_HASH -> ipbPassHash = it.value
                        EhCookieStore.KEY_IGNEOUS -> igneous = it.value
                    }
                }
                BaseDialogBuilder(context).apply {
                    if (ipbMemberId != null || ipbPassHash != null || igneous != null) {
                        val str = EhCookieStore.KEY_IPB_MEMBER_ID + ": " + ipbMemberId + "<br>" + EhCookieStore.KEY_IPB_PASS_HASH + ": " + ipbPassHash + "<br>" + EhCookieStore.KEY_IGNEOUS + ": " + igneous
                        val spanned = context.getString(R.string.settings_eh_identity_cookies_signed, str).parseAsHtml()
                        setMessage(spanned)
                        setNeutralButton(R.string.settings_eh_identity_cookies_copy) { _, _ ->
                            copyTextToClipboard(str.replace("<br>", "\n"), true)
                            // Avoid double notify user since system have done that on Tiramisu above
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) launchSnackBar(copiedToClipboard)
                        }
                    } else {
                        setMessage(guestMode)
                    }
                    setPositiveButton(R.string.settings_eh_sign_out) { _, _ ->
                        EhUtils.signOut()
                        navigator.popNavigate(SignInScreenDestination)
                    }
                }.show()
            }
            if (signin) {
                val placeholder = stringResource(id = R.string.please_wait)
                val resetImageLimitSucceed = stringResource(id = R.string.reset_limits_succeed)
                val noImageLimits = stringResource(id = R.string.image_limits_summary, 0, 0)
                var result by remember { mutableStateOf<HomeParser.Result?>(null) }
                var summary by rememberSaveable { mutableStateOf(noImageLimits) }
                suspend fun getImageLimits() = EhEngine.getImageLimits().also {
                    result = it
                    summary = context.getString(R.string.image_limits_summary, it.limits.current, it.limits.maximum)
                }
                val deferredResult = remember { coroutineScope.async { runSuspendCatching { getImageLimits() } } }
                Preference(
                    title = stringResource(id = R.string.image_limits),
                    summary = summary,
                ) {
                    val builder = BaseDialogBuilder(context).setMessage(placeholder)
                        .setPositiveButton(R.string.reset, null)
                        .setNegativeButton(android.R.string.cancel, null)
                    val dialog = builder.show()
                    val resetButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    fun bind(result: HomeParser.Result) {
                        val (current, maximum, resetCost) = result.limits
                        val (fundsGP, fundsC) = result.funds
                        val message = context.getString(R.string.current_limits, "$current / $maximum", resetCost) + "\n" + context.getString(R.string.current_funds, "$fundsGP+", fundsC)
                        dialog.setMessage(message)
                        resetButton.isEnabled = resetCost != 0
                    }
                    coroutineScope.launch {
                        runSuspendCatching {
                            result ?: deferredResult.await().getOrNull() ?: getImageLimits()
                            withUIContext { bind(result!!) }
                        }.onFailure {
                            withUIContext { dialog.setMessage(it.localizedMessage) }
                        }
                    }
                    resetButton.isEnabled = false
                    resetButton.setOnClickListener { button ->
                        button.isEnabled = false
                        dialog.setMessage(placeholder)
                        coroutineScope.launch {
                            runSuspendCatching {
                                EhEngine.resetImageLimits()
                                getImageLimits()
                            }.onSuccess {
                                launchSnackBar(resetImageLimitSucceed)
                                withUIContext { bind(it) }
                            }.onFailure {
                                withUIContext { dialog.setMessage(it.localizedMessage) }
                            }
                        }
                    }
                }
                SimpleMenuPreferenceInt(
                    title = stringResource(id = R.string.settings_eh_gallery_site),
                    entry = R.array.gallery_site_entries,
                    entryValueRes = R.array.gallery_site_entry_values,
                    value = Settings::gallerySite.observed,
                )
                Preference(
                    title = stringResource(id = R.string.settings_u_config),
                    summary = stringResource(id = R.string.settings_u_config_summary),
                ) { navigator.navigate(UConfigScreenDestination) }
                Preference(
                    title = stringResource(id = R.string.settings_my_tags),
                    summary = stringResource(id = R.string.settings_my_tags_summary),
                ) { navigator.navigate(MyTagsScreenDestination) }
            }
            var defaultFavSlot by Settings::defaultFavSlot.observed
            val disabled = stringResource(id = R.string.disabled_nav)
            val localFav = stringResource(id = R.string.local_favorites)
            val summary = when (defaultFavSlot) {
                -1 -> localFav
                in 0..9 -> Settings.favCat[defaultFavSlot]
                else -> stringResource(id = R.string.default_favorites_warning)
            }
            Preference(
                title = stringResource(id = R.string.default_favorites_collection),
                summary = summary,
            ) {
                coroutineScope.launch {
                    defaultFavSlot = dialogState.showSelectItem(
                        disabled,
                        localFav,
                        *Settings.favCat.takeIf { signin } ?: emptyArray(),
                        title = R.string.default_favorites_collection,
                    ) - 2
                }
            }
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.dark_theme),
                entry = R.array.night_mode_entries,
                entryValueRes = R.array.night_mode_values,
                value = Settings::theme.observed,
            )
            SwitchPreference(
                title = stringResource(id = R.string.black_dark_theme),
                value = Settings.blackDarkTheme::value,
            )
            SwitchPreference(
                title = stringResource(id = R.string.harmonize_category_color),
                value = Settings::harmonizeCategoryColor,
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_launch_page),
                entry = R.array.launch_page_entries,
                entryValueRes = R.array.launch_page_entry_values,
                value = Settings::launchPage.observed,
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_list_mode),
                entry = R.array.list_mode_entries,
                entryValueRes = R.array.list_mode_entry_values,
                value = Settings.listMode.asMutableState(),
            )
            IntSliderPreference(
                maxValue = 60,
                minValue = 20,
                step = 7,
                title = stringResource(id = R.string.list_tile_thumb_size),
                value = Settings.listThumbSize::value,
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_detail_size),
                entry = R.array.detail_size_entries,
                entryValueRes = R.array.detail_size_entry_values,
                value = Settings.detailSize.asMutableState(),
            )
            IntSliderPreference(
                maxValue = 10,
                minValue = 1,
                title = stringResource(id = R.string.settings_eh_thumb_columns),
                value = Settings.thumbColumns::value,
            )
            val thumbResolution = Settings::thumbResolution.observed
            val summary2 = stringResource(id = R.string.settings_eh_thumb_resolution_summary, stringArrayResource(id = R.array.thumb_resolution_entries)[thumbResolution.value])
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_thumb_resolution),
                summary = summary2,
                entry = R.array.thumb_resolution_entries,
                entryValueRes = R.array.thumb_resolution_entry_values,
                value = thumbResolution,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_force_eh_thumb),
                summary = stringResource(id = R.string.settings_eh_force_eh_thumb_summary),
                value = Settings::forceEhThumb,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_show_gallery_pages),
                summary = stringResource(id = R.string.settings_eh_show_gallery_pages_summary),
                value = Settings::showGalleryPages,
            )
            val showComments = Settings::showComments.observed
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_show_gallery_comments),
                summary = stringResource(id = R.string.settings_eh_show_gallery_comments_summary),
                value = showComments.rememberedAccessor,
            )
            AnimatedVisibility(visible = showComments.value) {
                IntSliderPreference(
                    maxValue = 100,
                    minValue = -101,
                    showTicks = false,
                    title = stringResource(id = R.string.settings_eh_show_gallery_comment_threshold),
                    summary = stringResource(id = R.string.settings_eh_show_gallery_comment_threshold_summary),
                    value = Settings::commentThreshold,
                )
            }
            if (EhTagDatabase.isTranslatable(context)) {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_eh_show_tag_translations),
                    summary = stringResource(id = R.string.settings_eh_show_tag_translations_summary),
                    value = Settings::showTagTranslations,
                )
                UrlPreference(
                    title = stringResource(id = R.string.settings_eh_tag_translations_source),
                    url = stringResource(id = R.string.settings_eh_tag_translations_source_url),
                )
            }
            Preference(
                title = stringResource(id = R.string.settings_eh_filter),
                summary = stringResource(id = R.string.settings_eh_filter_summary),
            ) { navigator.navigate(FilterScreenDestination) }
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_metered_network_warning),
                value = Settings.meteredNetworkWarning::value,
            )
            if (signin) {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_eh_show_jpn_title),
                    summary = stringResource(id = R.string.settings_eh_show_jpn_title_summary),
                    value = Settings::showJpnTitle,
                )
                val reqNews = Settings::requestNews.observed
                SwitchPreference(
                    title = stringResource(id = R.string.settings_eh_request_news),
                    value = reqNews.rememberedAccessor,
                )
                AnimatedVisibility(visible = reqNews.value) {
                    val pickerTitle = stringResource(id = R.string.settings_eh_request_news_timepicker)
                    Preference(title = pickerTitle) {
                        coroutineScope.launch {
                            val time = LocalTime.fromSecondOfDay(Settings.requestNewsTime)
                            val (hour, minute) = dialogState.showTimePicker(pickerTitle, time.hour, time.minute)
                            Settings.requestNewsTime = LocalTime(hour, minute).toSecondOfDay()
                        }
                    }
                }
                AnimatedVisibility(visible = reqNews.value) {
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_eh_hide_hv_events),
                        value = Settings::hideHvEvents,
                    )
                }
            }
        }
    }
}
