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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
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

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.EhScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val hasSignedIn by Settings.hasSignedIn.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = settingsEh) },
                navigationIcon = {
                    IconButton(onClick = { popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            if (hasSignedIn) {
                val displayName by Settings.displayName.collectAsState()
                Preference(
                    title = accountName,
                    summary = displayName,
                ) {
                    coroutineScope.launch {
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
                                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                                OutlinedTextField(
                                    state = state,
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            copyTextToClipboard(state.text, true)
                                            // Avoid double notify user since system have done that on Tiramisu above
                                            if (!isAtLeastT) launchSnackbar(copiedToClipboard)
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
                    title = settingsEhGallerySite,
                    entry = R.array.gallery_site_entries,
                    entryValueRes = R.array.gallery_site_entry_values,
                    value = gallerySite,
                )
                AnimatedVisibility(gallerySite.value == EhUrl.SITE_EX) {
                    var forceEhThumb by Settings.forceEhThumb.asMutableState()
                    SwitchPref(
                        checked = forceEhThumb,
                        onMutate = { forceEhThumb = !forceEhThumb },
                        title = settingsEhForceEhThumb,
                        summary = settingsEhForceEhThumbSummary,
                    )
                }
                Preference(
                    title = settingsUConfig,
                    summary = settingsUConfigSummary,
                ) { navigator.navigate(UConfigScreenDestination) }
                Preference(
                    title = settingsMyTags,
                    summary = settingsMyTagsSummary,
                ) { navigator.navigate(MyTagsScreenDestination) }
            }
            var defaultFavSlot by Settings::defaultFavSlot.observed
            val summary = when (defaultFavSlot) {
                -1 -> localFavorites
                in 0..9 -> Settings.favCat[defaultFavSlot]
                else -> defaultFavoritesWarning
            }
            Preference(
                title = defaultFavoritesCollection,
                summary = summary,
            ) {
                coroutineScope.launch {
                    val items = buildList {
                        add(disabledNav)
                        add(localFavorites)
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
                title = darkTheme,
                entry = R.array.night_mode_entries,
                entryValueRes = R.array.night_mode_values,
                value = Settings::theme.observed,
            )
            SwitchPreference(
                title = blackDarkTheme,
                value = Settings.blackDarkTheme::value,
            )
            SwitchPreference(
                title = harmonizeCategoryColor,
                value = Settings::harmonizeCategoryColor,
            )
            SimpleMenuPreferenceInt(
                title = settingsEhLaunchPage,
                entry = R.array.launch_page_entries,
                entryValueRes = R.array.launch_page_entry_values,
                value = Settings::launchPage.observed,
            )
            val listMode = Settings.listMode.asMutableState()
            SimpleMenuPreferenceInt(
                title = settingsEhListMode,
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
                        title = listTileThumbSize,
                        value = Settings.listThumbSize::value,
                    )
                    SimpleMenuPreferenceInt(
                        title = settingsEhDetailSize,
                        entry = R.array.detail_size_entries,
                        entryValueRes = R.array.detail_size_entry_values,
                        value = Settings.detailSize.asMutableState(),
                    )
                }
            }
            IntSliderPreference(
                maxValue = 10,
                minValue = 1,
                title = settingsEhThumbColumns,
                value = Settings.thumbColumns::value,
            )
            var showGalleryPages by Settings.showGalleryPages.asMutableState()
            SwitchPref(
                checked = showGalleryPages,
                onMutate = { showGalleryPages = !showGalleryPages },
                title = settingsEhShowGalleryPages,
                summary = settingsEhShowGalleryPagesSummary,
            )
            SwitchPreference(
                title = settingsEhShowVoteStatus,
                value = Settings.showVoteStatus::value,
            )
            val showComments = Settings::showComments.observed
            SwitchPreference(
                title = settingsEhShowGalleryComments,
                summary = settingsEhShowGalleryCommentsSummary,
                value = showComments.rememberedAccessor,
            )
            AnimatedVisibility(visible = showComments.value) {
                IntSliderPreference(
                    maxValue = 100,
                    minValue = -101,
                    showTicks = false,
                    title = settingsEhShowGalleryCommentThreshold,
                    summary = settingsEhShowGalleryCommentThresholdSummary,
                    value = Settings::commentThreshold,
                )
            }
            if (EhTagDatabase.isTranslatable(implicit<Context>())) {
                SwitchPreference(
                    title = settingsEhShowTagTranslations,
                    summary = settingsEhShowTagTranslationsSummary,
                    value = Settings::showTagTranslations,
                )
                UrlPreference(
                    title = settingsEhTagTranslationsSource,
                    url = settingsEhTagTranslationsSourceUrl,
                )
            }
            Preference(
                title = settingsEhFilter,
                summary = settingsEhFilterSummary,
            ) { navigate(FilterScreenDestination) }
            SwitchPreference(
                title = settingsEhMeteredNetworkWarning,
                value = Settings.meteredNetworkWarning::value,
            )
            if (hasSignedIn) {
                SwitchPreference(
                    title = settingsEhShowJpnTitle,
                    summary = settingsEhShowJpnTitleSummary,
                    value = Settings::showJpnTitle,
                )
                val reqNews = Settings::requestNews.observed
                SwitchPreference(
                    title = settingsEhRequestNews,
                    value = reqNews.rememberedAccessor,
                )
                AnimatedVisibility(visible = reqNews.value) {
                    Preference(title = settingsEhRequestNewsTimepicker) {
                        coroutineScope.launch {
                            val time = LocalTime.fromSecondOfDay(Settings.requestNewsTime)
                            val (hour, minute) = awaitSelectTime(settingsEhRequestNewsTimepicker, time.hour, time.minute)
                            Settings.requestNewsTime = LocalTime(hour, minute).toSecondOfDay()
                        }
                    }
                }
                SwitchPreference(
                    title = settingsEhHideHvEvents,
                    value = Settings::hideHvEvents,
                )
            }
        }
    }
}
