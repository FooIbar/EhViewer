package com.hippo.ehviewer.ui.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.account_name
import com.ehviewer.core.common.black_dark_theme
import com.ehviewer.core.common.copied_to_clipboard
import com.ehviewer.core.common.dark_theme
import com.ehviewer.core.common.default_favorites_collection
import com.ehviewer.core.common.default_favorites_warning
import com.ehviewer.core.common.disabled_nav
import com.ehviewer.core.common.harmonize_category_color
import com.ehviewer.core.common.list_tile_thumb_size
import com.ehviewer.core.common.local_favorites
import com.ehviewer.core.common.settings_eh
import com.ehviewer.core.common.settings_eh_clear_igneous
import com.ehviewer.core.common.settings_eh_detail_size
import com.ehviewer.core.common.settings_eh_filter
import com.ehviewer.core.common.settings_eh_filter_summary
import com.ehviewer.core.common.settings_eh_force_eh_thumb
import com.ehviewer.core.common.settings_eh_force_eh_thumb_summary
import com.ehviewer.core.common.settings_eh_gallery_site
import com.ehviewer.core.common.settings_eh_hide_hv_events
import com.ehviewer.core.common.settings_eh_identity_cookies_signed
import com.ehviewer.core.common.settings_eh_launch_page
import com.ehviewer.core.common.settings_eh_list_mode
import com.ehviewer.core.common.settings_eh_metered_network_warning
import com.ehviewer.core.common.settings_eh_request_news
import com.ehviewer.core.common.settings_eh_request_news_timepicker
import com.ehviewer.core.common.settings_eh_show_gallery_comment_threshold
import com.ehviewer.core.common.settings_eh_show_gallery_comment_threshold_summary
import com.ehviewer.core.common.settings_eh_show_gallery_comments
import com.ehviewer.core.common.settings_eh_show_gallery_comments_summary
import com.ehviewer.core.common.settings_eh_show_gallery_pages
import com.ehviewer.core.common.settings_eh_show_gallery_pages_summary
import com.ehviewer.core.common.settings_eh_show_jpn_title
import com.ehviewer.core.common.settings_eh_show_jpn_title_summary
import com.ehviewer.core.common.settings_eh_show_tag_translations
import com.ehviewer.core.common.settings_eh_show_tag_translations_summary
import com.ehviewer.core.common.settings_eh_show_vote_status
import com.ehviewer.core.common.settings_eh_sign_out
import com.ehviewer.core.common.settings_eh_tag_translations_source
import com.ehviewer.core.common.settings_eh_tag_translations_source_url
import com.ehviewer.core.common.settings_eh_thumb_columns
import com.ehviewer.core.common.settings_my_tags
import com.ehviewer.core.common.settings_my_tags_summary
import com.ehviewer.core.common.settings_u_config
import com.ehviewer.core.common.settings_u_config_summary
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.FilterScreenDestination
import com.hippo.ehviewer.ui.destinations.MyTagsScreenDestination
import com.hippo.ehviewer.ui.destinations.UConfigScreenDestination
import com.hippo.ehviewer.ui.screen.implicit
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberedAccessor
import com.hippo.ehviewer.util.copyTextToClipboard
import com.hippo.ehviewer.util.isAtLeastT
import com.jamal.composeprefs3.ui.prefs.SwitchPref
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.EhScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    fun launchSnackBar(content: String) = coroutineScope.launch { snackbarHostState.showSnackbar(content) }
    val hasSignedIn by Settings.hasSignedIn.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.settings_eh)) },
                navigationIcon = {
                    IconButton(onClick = { popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val copiedToClipboard = stringResource(Res.string.copied_to_clipboard)
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            if (hasSignedIn) {
                val displayName by Settings.displayName.collectAsState()
                Preference(
                    title = stringResource(Res.string.account_name),
                    summary = displayName,
                ) {
                    coroutineScope.launch {
                        val cookies = EhCookieStore.getIdentityCookies()
                        awaitConfirmationOrCancel(
                            confirmText = Res.string.settings_eh_sign_out,
                            dismissText = Res.string.settings_eh_clear_igneous,
                            showCancelButton = cookies.last().second != null,
                            onCancelButtonClick = { EhCookieStore.clearIgneous() },
                            secure = true,
                        ) {
                            Column {
                                val warning = stringResource(Res.string.settings_eh_identity_cookies_signed)
                                val state = rememberTextFieldState(cookies.joinToString("\n") { (k, v) -> "$k: $v" })
                                Text(text = AnnotatedString.fromHtml(warning))
                                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                                OutlinedTextField(
                                    state = state,
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            copyTextToClipboard(state.text, true)
                                            // Avoid double notify user since system have done that on Tiramisu above
                                            if (!isAtLeastT) launchSnackBar(copiedToClipboard)
                                        }) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                                        }
                                    },
                                )
                            }
                        }
                        EhUtils.signOut()
                    }
                }
                val gallerySite = Settings.gallerySite.asMutableState()
                SimpleMenuPreferenceInt(
                    title = stringResource(Res.string.settings_eh_gallery_site),
                    entry = R.array.gallery_site_entries,
                    entryValueRes = R.array.gallery_site_entry_values,
                    value = gallerySite,
                )
                AnimatedVisibility(gallerySite.value == EhUrl.SITE_EX) {
                    var forceEhThumb by Settings.forceEhThumb.asMutableState()
                    SwitchPref(
                        checked = forceEhThumb,
                        onMutate = { forceEhThumb = !forceEhThumb },
                        title = stringResource(Res.string.settings_eh_force_eh_thumb),
                        summary = stringResource(Res.string.settings_eh_force_eh_thumb_summary),
                    )
                }
                Preference(
                    title = stringResource(Res.string.settings_u_config),
                    summary = stringResource(Res.string.settings_u_config_summary),
                ) { navigator.navigate(UConfigScreenDestination) }
                Preference(
                    title = stringResource(Res.string.settings_my_tags),
                    summary = stringResource(Res.string.settings_my_tags_summary),
                ) { navigator.navigate(MyTagsScreenDestination) }
            }
            var defaultFavSlot by Settings::defaultFavSlot.observed
            val disabled = stringResource(Res.string.disabled_nav)
            val localFav = stringResource(Res.string.local_favorites)
            val summary = when (defaultFavSlot) {
                -1 -> localFav
                in 0..9 -> Settings.favCat[defaultFavSlot]
                else -> stringResource(Res.string.default_favorites_warning)
            }
            Preference(
                title = stringResource(Res.string.default_favorites_collection),
                summary = summary,
            ) {
                coroutineScope.launch {
                    val items = buildList {
                        add(disabled)
                        add(localFav)
                        if (hasSignedIn) {
                            addAll(Settings.favCat)
                        }
                    }
                    defaultFavSlot = awaitSelectItem(
                        items = items,
                        title = Res.string.default_favorites_collection,
                        selected = defaultFavSlot + 2,
                    ) - 2
                }
            }
            SimpleMenuPreferenceInt(
                title = stringResource(Res.string.dark_theme),
                entry = R.array.night_mode_entries,
                entryValueRes = R.array.night_mode_values,
                value = Settings::theme.observed,
            )
            SwitchPreference(
                title = stringResource(Res.string.black_dark_theme),
                value = Settings.blackDarkTheme::value,
            )
            SwitchPreference(
                title = stringResource(Res.string.harmonize_category_color),
                value = Settings::harmonizeCategoryColor,
            )
            SimpleMenuPreferenceInt(
                title = stringResource(Res.string.settings_eh_launch_page),
                entry = R.array.launch_page_entries,
                entryValueRes = R.array.launch_page_entry_values,
                value = Settings::launchPage.observed,
            )
            val listMode = Settings.listMode.asMutableState()
            SimpleMenuPreferenceInt(
                title = stringResource(Res.string.settings_eh_list_mode),
                entry = R.array.list_mode_entries,
                entryValueRes = R.array.list_mode_entry_values,
                value = listMode,
            )
            AnimatedVisibility(visible = listMode.value == 0) {
                Column {
                    IntSliderPreference(
                        maxValue = 60,
                        minValue = 20,
                        step = 7,
                        title = stringResource(Res.string.list_tile_thumb_size),
                        value = Settings.listThumbSize::value,
                    )
                    SimpleMenuPreferenceInt(
                        title = stringResource(Res.string.settings_eh_detail_size),
                        entry = R.array.detail_size_entries,
                        entryValueRes = R.array.detail_size_entry_values,
                        value = Settings.detailSize.asMutableState(),
                    )
                }
            }
            IntSliderPreference(
                maxValue = 10,
                minValue = 1,
                title = stringResource(Res.string.settings_eh_thumb_columns),
                value = Settings.thumbColumns::value,
            )
            var showGalleryPages by Settings.showGalleryPages.asMutableState()
            SwitchPref(
                checked = showGalleryPages,
                onMutate = { showGalleryPages = !showGalleryPages },
                title = stringResource(Res.string.settings_eh_show_gallery_pages),
                summary = stringResource(Res.string.settings_eh_show_gallery_pages_summary),
            )
            SwitchPreference(
                title = stringResource(Res.string.settings_eh_show_vote_status),
                value = Settings.showVoteStatus::value,
            )
            val showComments = Settings::showComments.observed
            SwitchPreference(
                title = stringResource(Res.string.settings_eh_show_gallery_comments),
                summary = stringResource(Res.string.settings_eh_show_gallery_comments_summary),
                value = showComments.rememberedAccessor,
            )
            AnimatedVisibility(visible = showComments.value) {
                IntSliderPreference(
                    maxValue = 100,
                    minValue = -101,
                    showTicks = false,
                    title = stringResource(Res.string.settings_eh_show_gallery_comment_threshold),
                    summary = stringResource(Res.string.settings_eh_show_gallery_comment_threshold_summary),
                    value = Settings::commentThreshold,
                )
            }
            if (EhTagDatabase.isTranslatable(implicit<Context>())) {
                SwitchPreference(
                    title = stringResource(Res.string.settings_eh_show_tag_translations),
                    summary = stringResource(Res.string.settings_eh_show_tag_translations_summary),
                    value = Settings::showTagTranslations,
                )
                UrlPreference(
                    title = stringResource(Res.string.settings_eh_tag_translations_source),
                    url = stringResource(Res.string.settings_eh_tag_translations_source_url),
                )
            }
            Preference(
                title = stringResource(Res.string.settings_eh_filter),
                summary = stringResource(Res.string.settings_eh_filter_summary),
            ) { navigate(FilterScreenDestination) }
            SwitchPreference(
                title = stringResource(Res.string.settings_eh_metered_network_warning),
                value = Settings.meteredNetworkWarning::value,
            )
            if (hasSignedIn) {
                SwitchPreference(
                    title = stringResource(Res.string.settings_eh_show_jpn_title),
                    summary = stringResource(Res.string.settings_eh_show_jpn_title_summary),
                    value = Settings::showJpnTitle,
                )
                val reqNews = Settings::requestNews.observed
                SwitchPreference(
                    title = stringResource(Res.string.settings_eh_request_news),
                    value = reqNews.rememberedAccessor,
                )
                AnimatedVisibility(visible = reqNews.value) {
                    val pickerTitle = stringResource(Res.string.settings_eh_request_news_timepicker)
                    Preference(title = pickerTitle) {
                        coroutineScope.launch {
                            val time = LocalTime.fromSecondOfDay(Settings.requestNewsTime)
                            val (hour, minute) = awaitSelectTime(pickerTitle, time.hour, time.minute)
                            Settings.requestNewsTime = LocalTime(hour, minute).toSecondOfDay()
                        }
                    }
                }
                SwitchPreference(
                    title = stringResource(Res.string.settings_eh_hide_hv_events),
                    value = Settings::hideHvEvents,
                )
            }
        }
    }
}
