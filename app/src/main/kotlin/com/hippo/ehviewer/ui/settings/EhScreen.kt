package com.hippo.ehviewer.ui.settings

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.ehviewer.core.i18n.R
import com.ehviewer.core.util.launch
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.FilterScreenDestination
import com.hippo.ehviewer.ui.destinations.MyTagsScreenDestination
import com.hippo.ehviewer.ui.destinations.UConfigScreenDestination
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.ui.tools.awaitSelectItem
import com.hippo.ehviewer.ui.tools.awaitSelectTime
import com.hippo.ehviewer.util.copyTextToClipboard
import com.hippo.ehviewer.util.isAtLeastT
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.datetime.LocalTime
import moe.tarsin.navigate
import moe.tarsin.snackbar

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.EhScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    fun launchSnackBar(content: String) = launch { snackbar(content) }
    val hasSignedIn by Settings.hasSignedIn.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_eh)) },
                navigationIcon = { NavigationIcon() },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val copiedToClipboard = stringResource(id = R.string.copied_to_clipboard)
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            if (hasSignedIn) {
                val displayName by Settings.displayName.collectAsState()
                Preference(
                    title = stringResource(id = R.string.account_name),
                    summary = displayName,
                ) {
                    launch {
                        val cookies = EhCookieStore.getIdentityCookies()
                        awaitConfirmationOrCancel(
                            confirmText = R.string.settings_eh_sign_out,
                            dismissText = R.string.settings_eh_clear_igneous,
                            showCancelButton = cookies.last().second != null,
                            onCancelButtonClick = { EhCookieStore.clearIgneous() },
                            secure = true,
                        ) {
                            Column {
                                val warning = stringResource(id = R.string.settings_eh_identity_cookies_signed)
                                val state = rememberTextFieldState(cookies.joinToString("\n") { (k, v) -> "$k: $v" })
                                Text(text = AnnotatedString.fromHtml(warning))
                                Spacer(modifier = Modifier.size(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)))
                                OutlinedTextField(
                                    state = state,
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                copyTextToClipboard(state.text, true)
                                                // Avoid double notify user since system have done that on Tiramisu above
                                                if (!isAtLeastT) launchSnackBar(copiedToClipboard)
                                            },
                                            shapes = IconButtonDefaults.shapes(),
                                        ) {
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
                    title = stringResource(id = R.string.settings_eh_gallery_site),
                    entry = com.hippo.ehviewer.R.array.gallery_site_entries,
                    entryValueRes = com.hippo.ehviewer.R.array.gallery_site_entry_values,
                    state = gallerySite,
                )
                Preference(
                    title = stringResource(id = R.string.settings_u_config),
                    summary = stringResource(id = R.string.settings_u_config_summary),
                ) { navigate(UConfigScreenDestination) }
                Preference(
                    title = stringResource(id = R.string.settings_my_tags),
                    summary = stringResource(id = R.string.settings_my_tags_summary),
                ) { navigate(MyTagsScreenDestination) }
            }
            var defaultFavSlot by Settings.defaultFavSlot.asMutableState()
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
                launch {
                    val items = buildList {
                        add(disabled)
                        add(localFav)
                        if (hasSignedIn) {
                            addAll(Settings.favCat)
                        }
                    }
                    defaultFavSlot = awaitSelectItem(
                        items = items,
                        title = R.string.default_favorites_collection,
                        selected = defaultFavSlot + 2,
                    ) - 2
                }
            }
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.dark_theme),
                entry = com.hippo.ehviewer.R.array.night_mode_entries,
                entryValueRes = com.hippo.ehviewer.R.array.night_mode_values,
                state = Settings.theme.asMutableState(),
            )
            SwitchPreference(
                title = stringResource(id = R.string.black_dark_theme),
                state = Settings.blackDarkTheme.asMutableState(),
            )
            SwitchPreference(
                title = stringResource(id = R.string.harmonize_category_color),
                state = Settings.harmonizeCategoryColor.asMutableState(),
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_launch_page),
                entry = com.hippo.ehviewer.R.array.launch_page_entries,
                entryValueRes = com.hippo.ehviewer.R.array.launch_page_entry_values,
                state = Settings.launchPage.asMutableState(),
            )
            val listMode = Settings.listMode.asMutableState()
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_list_mode),
                entry = com.hippo.ehviewer.R.array.list_mode_entries,
                entryValueRes = com.hippo.ehviewer.R.array.list_mode_entry_values,
                state = listMode,
            )
            AnimatedVisibility(visible = listMode.value == 0) {
                Column {
                    IntSliderPreference(
                        maxValue = 60,
                        minValue = 20,
                        step = 7,
                        title = stringResource(id = R.string.list_tile_thumb_size),
                        state = Settings.listThumbSize.asMutableState(),
                    )
                    SimpleMenuPreferenceInt(
                        title = stringResource(id = R.string.settings_eh_detail_size),
                        entry = com.hippo.ehviewer.R.array.detail_size_entries,
                        entryValueRes = com.hippo.ehviewer.R.array.detail_size_entry_values,
                        state = Settings.detailSize.asMutableState(),
                    )
                }
            }
            IntSliderPreference(
                maxValue = 10,
                minValue = 1,
                title = stringResource(id = R.string.settings_eh_thumb_columns),
                state = Settings.thumbColumns.asMutableState(),
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_show_gallery_pages),
                summary = stringResource(id = R.string.settings_eh_show_gallery_pages_summary),
                state = Settings.showGalleryPages.asMutableState(),
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_show_vote_status),
                state = Settings.showVoteStatus.asMutableState(),
            )
            val showComments = Settings.showComments.asMutableState()
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_show_gallery_comments),
                summary = stringResource(id = R.string.settings_eh_show_gallery_comments_summary),
                state = showComments,
            )
            AnimatedVisibility(visible = showComments.value) {
                IntSliderPreference(
                    maxValue = 100,
                    minValue = -101,
                    title = stringResource(id = R.string.settings_eh_show_gallery_comment_threshold),
                    summary = stringResource(id = R.string.settings_eh_show_gallery_comment_threshold_summary),
                    state = Settings.commentThreshold.asMutableState(),
                )
            }
            if (EhTagDatabase.translatable) {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_eh_show_tag_translations),
                    summary = stringResource(id = R.string.settings_eh_show_tag_translations_summary),
                    state = Settings.showTagTranslations.asMutableState(),
                )
                UrlPreference(
                    title = stringResource(id = R.string.settings_eh_tag_translations_source),
                    url = stringResource(id = R.string.settings_eh_tag_translations_source_url),
                )
            }
            Preference(
                title = stringResource(id = R.string.settings_eh_filter),
                summary = stringResource(id = R.string.settings_eh_filter_summary),
            ) { navigate(FilterScreenDestination) }
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_metered_network_warning),
                state = Settings.meteredNetworkWarning.asMutableState(),
            )
            if (hasSignedIn) {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_eh_show_jpn_title),
                    summary = stringResource(id = R.string.settings_eh_show_jpn_title_summary),
                    state = Settings.showJpnTitle.asMutableState(),
                )
                val reqNews = Settings.requestNews.asMutableState()
                SwitchPreference(
                    title = stringResource(id = R.string.settings_eh_request_news),
                    state = reqNews,
                )
                AnimatedVisibility(visible = reqNews.value) {
                    val pickerTitle = stringResource(id = R.string.settings_eh_request_news_timepicker)
                    Preference(title = pickerTitle) {
                        launch {
                            val time = LocalTime.fromSecondOfDay(Settings.requestNewsTime)
                            val (hour, minute) = awaitSelectTime(pickerTitle, time.hour, time.minute)
                            Settings.requestNewsTime = LocalTime(hour, minute).toSecondOfDay()
                        }
                    }
                }
                SwitchPreference(
                    title = stringResource(id = R.string.settings_eh_hide_hv_events),
                    state = Settings.hideHvEvents.asMutableState(),
                )
            }
        }
    }
}
