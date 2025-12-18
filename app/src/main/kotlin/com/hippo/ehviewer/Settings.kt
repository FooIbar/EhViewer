@file:Suppress("SameParameterValue")

package com.hippo.ehviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.preferences.DataStorePreferences
import com.ehviewer.core.preferences.PrefDelegate
import com.ehviewer.core.preferences.edit
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.download.DownloadsFilterMode
import com.hippo.ehviewer.download.SortMode
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import java.util.Locale
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map

@Stable
@Composable
inline fun <R, T> PrefDelegate<T>.collectAsState(crossinline transform: @DisallowComposableCalls (T) -> R): State<R> {
    val flow = remember { valueFlow().map { transform(it) } }
    val init = value
    return flow.collectAsState(transform(init))
}

@Stable
@Composable
fun <T> PrefDelegate<T>.collectAsState(): State<T> {
    val flow = remember { valueFlow() }
    val init = value
    return flow.collectAsState(init)
}

@Stable
@Composable
fun <T> PrefDelegate<T>.asMutableState(): MutableState<T> {
    val readOnly = collectAsState()
    return remember {
        object : MutableState<T> {
            override var value: T
                get() = readOnly.value
                set(value) {
                    this@asMutableState.value = value
                }
            override fun component1() = readOnly.value
            override fun component2() = this@asMutableState::value::set
        }
    }
}

object Settings : DataStorePreferences(null) {
    @Suppress("ktlint:standard:backing-property-naming")
    private val _favFlow = MutableSharedFlow<Unit>()
    val favChangesFlow = _favFlow.debounce(1000)
    var favCat by stringArrayPref("fav_cat", 10, "Favorites").emitTo(_favFlow)
    var favCount by intArrayPref("fav_count", 10).emitTo(_favFlow)
    var favCloudCount by intPref("fav_cloud", 0).emitTo(_favFlow)

    // Eh
    val gallerySite = intPref("gallery_site_2", 0).observed(::updateWhenGallerySiteChanges)
    val defaultFavSlot = intPref("default_favorite_slot", -2)
    val theme = intPref("theme_2", -1).observed(::updateWhenThemeChanges)
    val blackDarkTheme = boolPref("black_dark_theme", false)
    val harmonizeCategoryColor = boolPref("harmonize_category_color", true)
    val launchPage = intPref("launch_page_2", 0)
    val listMode = intPref("list_mode_2", 0)
    val listThumbSize = intPref("list_tile_size", 40)
    val detailSize = intPref("detail_size_2", 0)
    val thumbColumns = intPref("thumb_columns", 3)
    val showGalleryPages = boolPref("show_gallery_pages", true)
    val showReadingProgress = boolPref("show_reading_progress", false)
    val showVoteStatus = boolPref("show_vote_status", false)
    val showComments = boolPref("show_gallery_comments", true)
    val commentThreshold = intPref("comment_threshold", -100)
    val showTagTranslations = boolPref("show_tag_translations", false).observed(::updateWhenTagTranslationChanges)
    val meteredNetworkWarning = boolPref("cellular_network_warning", false)
    val showJpnTitle = boolPref("show_jpn_title", false)
    val requestNews = boolPref("request_news", false).observed { updateWhenRequestNewsChanges() }
    val hideHvEvents = boolPref("hide_hv_events", false)

    // Download
    val mediaScan = boolPref("media_scan", false).observed(::updateWhenKeepMediaStatusChanges)
    val multiThreadDownload = intPref("download_thread_2", 3)
    val downloadDelay = intPref("download_delay_3", 1000)
    val timeoutSpeed = intPref("timeout_speed_level", 6)
    val preloadImage = intPref("preload_image_2", 5)
    val downloadOriginImage = boolPref("download_origin_image", false)
    val saveAsCbz = boolPref("save_as_cbz", false)
    val archiveMetadata = boolPref("archive_metadata", true)

    // Privacy
    val security = boolPref("require_unlock", false)
    val securityDelay = intPref("require_unlock_delay", 0)
    val enabledSecurity = boolPref("enable_secure", false)

    // Advanced
    val saveParseErrorBody = boolPref("save_parse_error_body", true)
    val saveCrashLog = boolPref("save_crash_log", true)
    val readCacheSize = intPref("read_cache_size_2", 640)
    val enableCronet = boolPref("enable_cronet", true)
    val enableQuic = boolPref("enable_quic", true)
    val hardwareBitmapThreshold = intPref("hardware_bitmap_threshold", 16384)
    val preloadThumbAggressively = boolPref("preload_thumb_aggressively", false)
    val animateItems = boolPref("animate_items", true)
    val desktopSite = boolPref("desktop_site", true)

    // About
    val backupBeforeUpdate = boolPref("backup_before_update", false)
    val useCIUpdateChannel = boolPref("ci_update_channel", BuildConfig.SNAPSHOT)
    val updateIntervalDays = intPref("update_interval_days", 7)

    // Misc
    val languageFilter = intPref("language_filter", -1)
    val downloadSortMode = intPref("download_sort_mode", SortMode.Default.flag)
    val downloadFilterMode = intPref("download_filter_mode", DownloadsFilterMode.Default.flag)
    val hasSignedIn = boolPref("has_signed_in", EhCookieStore.hasSignedIn())
    val needSignIn = boolPref("need_sign_in", true)
    val gridView = boolPref("grid_view", false)
    val qSSaveProgress = boolPref("qs_save_progress", true)
    val displayName = stringOrNullPref("display_name")
    val avatar = stringOrNullPref("avatar")
    val recentDownloadLabel = stringOrNullPref("recent_download_label")

    var downloadScheme by stringOrNullPref("image_scheme")
    var downloadAuthority by stringOrNullPref("image_authority")
    var downloadPath by stringOrNullPref("image_path")
    var downloadQuery by stringOrNullPref("image_query")
    var downloadFragment by stringOrNullPref("image_fragment")
    var archivePasswds by stringSetPref("archive_passwds")
    var appLinkVerifyTip by boolPref("app_link_verify_tip", false)
    var hasDefaultDownloadLabel by boolPref("has_default_download_label", false)
    var removeImageFiles by boolPref("include_pic", true)
    var recentFavCat by intPref("recent_fav_cat", FavListUrlBuilder.FAV_CAT_LOCAL)
    var clipboardTextHashCode by intPref("clipboard_text_hash_code", 0)
    var requestNewsTime by intPref("request_news_time", 0).observed { updateWhenRequestNewsChanges() }
    var lastDawnDays by intPref("last_dawn_days", 0)
    var recentToplist by stringPref("recent_toplist", "11")
    var defaultDownloadLabel by stringOrNullPref("default_download_label")
    var lastUpdateTime by longPref("last_update_time", BuildConfig.COMMIT_TIME)

    // Reader
    val cropBorder = boolPref("crop_borders", false)
    val colorFilter = boolPref("pref_color_filter_key", false)
    val colorFilterValue = intPref("color_filter_value", 0)
    val colorFilterMode = intPref("color_filter_mode", 0)
    val customBrightness = boolPref("pref_custom_brightness_key", false)
    val customBrightnessValue = intPref("custom_brightness_value", 0)
    val readingMode = intPref("pref_default_reading_mode_key", ReadingModeType.DEFAULT.prefValue)
    val orientationMode = intPref("pref_default_orientation_type_key", OrientationType.DEFAULT.prefValue)
    val showReaderSeekbar = boolPref("pref_show_reader_seekbar", true)
    val showPageNumber = boolPref("pref_show_page_number_key", true)
    val readerTheme = intPref("pref_reader_theme_key", 1)
    val doubleTapToZoom = boolPref("pref_double_tap_to_zoom", true)
    val fullscreen = boolPref("fullscreen", true)
    val cutoutShort = boolPref("cutout_short", true)
    val keepScreenOn = boolPref("pref_keep_screen_on_key", true)
    val readerLongTapAction = boolPref("reader_long_tap", true)
    val pageTransitions = boolPref("pref_enable_transitions_key", true)
    val readWithVolumeKeys = boolPref("reader_volume_keys", false)
    val readWithVolumeKeysInverted = boolPref("reader_volume_keys_inverted", false)
    val grayScale = boolPref("pref_grayscale", false)
    val invertedColors = boolPref("pref_inverted_colors", false)
    val readerWebtoonNav = intPref("reader_navigation_mode_webtoon", 0)
    val readerPagerNav = intPref("reader_navigation_mode_pager", 0)
    val readerPagerNavInverted = intPref("reader_tapping_inverted_2", 0)
    val readerWebtoonNavInverted = intPref("reader_tapping_inverted_webtoon_2", 0)
    val webtoonSidePadding = intPref("webtoon_side_padding", 0)
    val navigateToPan = boolPref("navigate_pan", true)
    val imageScaleType = intPref("pref_image_scale_type_key", 1)
    val landscapeZoom = boolPref("landscape_zoom", false)
    val zoomStart = intPref("pref_zoom_start_key", 1)
    val showNavigationOverlayNewUser = boolPref("reader_navigation_overlay_new_user", true)
    val showNavigationOverlayOnStart = boolPref("reader_navigation_overlay_on_start", false)
    val stripExtraneousAds = boolPref("strip_extraneous_ads", false)

    init {
        edit { pref ->
            if ("CN" == Locale.getDefault().country) {
                if (showTagTranslations !in pref) pref[showTagTranslations] = true
            }
            if (pref[downloadFilterMode] == DownloadsFilterMode.ARTIST.flag && pref[recentDownloadLabel] == null) {
                pref[recentDownloadLabel] = ""
            }
            val orientation = pref[orientationMode]
            if (OrientationType.entries.none { it.prefValue == orientation }) {
                pref.remove(orientationMode)
            }
        }
    }

    interface Delegate<R> {
        fun changesFlow(): Flow<Unit>
        operator fun getValue(thisRef: Any?, prop: KProperty<*>?): R
        operator fun setValue(thisRef: Any?, prop: KProperty<*>?, value: R)
    }

    private fun intArrayPref(key: String, count: Int) = object : Delegate<IntArray> {
        private val delegates = Array(count) { intPref("${key}_$it", 0) }
        override fun changesFlow(): Flow<Unit> = delegates.asFlow().flatMapMerge { it.changesFlow() }.conflate()
        override fun getValue(thisRef: Any?, prop: KProperty<*>?) = IntArray(delegates.size) { delegates[it].value }
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: IntArray) {
            check(value.size == count)
            edit { pref -> value.zip(delegates) { v, d -> pref[d] = v } }
        }
    }

    private fun stringArrayPref(key: String, count: Int, defMetaValue: String) = object : Delegate<Array<String>> {
        private val delegates = Array(count) { stringPref("${key}_$it", "$defMetaValue $it") }
        override fun changesFlow(): Flow<Unit> = delegates.asFlow().flatMapMerge { it.changesFlow() }.conflate()
        override fun getValue(thisRef: Any?, prop: KProperty<*>?) = Array(delegates.size) { delegates[it].value }
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: Array<String>) {
            check(value.size == count)
            edit { pref -> value.zip(delegates) { v, d -> pref[d] = v } }
        }
    }
}
