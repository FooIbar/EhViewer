package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.core.text.parseAsHtml
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.ui.destinations.FilterScreenDestination
import com.hippo.ehviewer.ui.destinations.MyTagsScreenDestination
import com.hippo.ehviewer.ui.destinations.SignInScreenDestination
import com.hippo.ehviewer.ui.destinations.UConfigScreenDestination
import com.hippo.ehviewer.ui.screen.popNavigate
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberedAccessor
import com.hippo.ehviewer.ui.tools.toAnnotatedString
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.copyTextToClipboard
import com.hippo.ehviewer.util.isAtLeastT
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.Dispatchers
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
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            Preference(
                title = stringResource(id = R.string.account_name),
                summary = Settings.displayName ?: guestMode,
            ) {
                coroutineScope.launch {
                    val cookies = EhCookieStore.getIdentityCookies()
                    dialogState.awaitPermissionOrCancel(
                        confirmText = R.string.settings_eh_sign_out,
                        dismissText = R.string.settings_eh_clear_igneous,
                        showCancelButton = cookies.last().second != null,
                        onCancelButtonClick = { EhCookieStore.clearIgneous() },
                    ) {
                        if (signin) {
                            Column {
                                val warning = stringResource(id = R.string.settings_eh_identity_cookies_signed)
                                val str = cookies.joinToString("\n") { (k, v) -> "$k: $v" }
                                Text(text = warning.parseAsHtml().toAnnotatedString())
                                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                                OutlinedTextField(
                                    value = str,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            copyTextToClipboard(str, true)
                                            // Avoid double notify user since system have done that on Tiramisu above
                                            if (!isAtLeastT) launchSnackBar(copiedToClipboard)
                                        }) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                                        }
                                    },
                                )
                            }
                        } else {
                            Text(text = guestMode)
                        }
                    }
                    EhUtils.signOut()
                    withUIContext {
                        navigator.popNavigate(SignInScreenDestination)
                    }
                }
            }
            if (signin) {
                val placeholder = stringResource(id = R.string.please_wait)
                val resetImageLimitSucceed = stringResource(id = R.string.reset_limits_succeed)
                var result by rememberSaveable { mutableStateOf<HomeParser.Result?>(null) }
                var error by rememberSaveable { mutableStateOf<String?>(null) }
                val summary by rememberUpdatedState(
                    stringResource(
                        id = R.string.image_limits_summary,
                        result?.run { limits.current } ?: 0,
                        result?.run { limits.maximum } ?: 0,
                    ),
                )
                suspend fun getImageLimits() {
                    result = EhEngine.getImageLimits()
                    error = null
                }
                if (result == null && error == null) {
                    LaunchedEffect(Unit) {
                        runSuspendCatching {
                            withIOContext { getImageLimits() }
                        }.onFailure {
                            error = ExceptionUtils.getReadableString(it)
                        }
                    }
                }
                Preference(
                    title = stringResource(id = R.string.image_limits),
                    summary = summary,
                ) {
                    coroutineScope.launch {
                        dialogState.awaitPermissionOrCancel(
                            confirmText = R.string.reset,
                            title = R.string.image_limits,
                            confirmButtonEnabled = result?.run { limits.resetCost != 0 } ?: false,
                        ) {
                            error?.let {
                                Text(text = it)
                            } ?: result?.let { (limits, funds) ->
                                Text(
                                    text = stringResource(id = R.string.current_limits, summary, limits.resetCost) +
                                        "\n" + stringResource(id = R.string.current_funds, "${funds.fundsGP}+", funds.fundsC),
                                )
                            } ?: run {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                                    Text(text = placeholder)
                                }
                            }
                        }
                        runSuspendCatching {
                            EhEngine.resetImageLimits()
                            getImageLimits()
                        }.onSuccess {
                            launchSnackBar(resetImageLimitSucceed)
                        }.onFailure {
                            error = ExceptionUtils.getReadableString(it)
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
                    val items = buildList {
                        add(disabled)
                        add(localFav)
                        if (signin) {
                            addAll(Settings.favCat)
                        }
                    }
                    defaultFavSlot = dialogState.showSelectItem(items, R.string.default_favorites_collection) - 2
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
                SwitchPreference(
                    title = stringResource(id = R.string.settings_eh_hide_hv_events),
                    value = Settings::hideHvEvents,
                )
            }
        }
    }
}
