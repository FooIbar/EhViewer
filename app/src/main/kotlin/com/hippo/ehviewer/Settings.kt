@file:Suppress("SameParameterValue")

package com.hippo.ehviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.hippo.ehviewer.client.CHROME_USER_AGENT
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import java.util.Locale
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import splitties.preferences.BoolPref
import splitties.preferences.DataStorePreferences
import splitties.preferences.FloatPref
import splitties.preferences.IntPref
import splitties.preferences.LongPref
import splitties.preferences.PrefDelegate
import splitties.preferences.StringOrNullPref
import splitties.preferences.StringPref
import splitties.preferences.StringSetOrNullPref
import splitties.preferences.StringSetPref
import splitties.preferences.edit

@Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
fun <T> PrefDelegate<T>.getValue(): T = when (this) {
    is BoolPref -> value
    is IntPref -> value
    is FloatPref -> value
    is LongPref -> value
    is StringPref -> value
    is StringOrNullPref -> value
    is StringSetPref -> value
    is StringSetOrNullPref -> value
} as T

@Suppress("UNCHECKED_CAST")
fun <T> PrefDelegate<T>.setValue(newValue: T) = when (this) {
    is BoolPref -> value = newValue as Boolean
    is IntPref -> value = newValue as Int
    is FloatPref -> value = newValue as Float
    is LongPref -> value = newValue as Long
    is StringPref -> value = newValue as String
    is StringOrNullPref -> value = newValue as? String
    is StringSetPref -> value = newValue as Set<String?>
    is StringSetOrNullPref -> value = newValue as? Set<String?>
}

@Stable
@Composable
inline fun <R, T> PrefDelegate<T>.collectAsState(crossinline transform: @DisallowComposableCalls (T) -> R): State<R> {
    val flow = remember { valueFlow().map { transform(it) } }
    val init = getValue()
    return flow.collectAsState(transform(init))
}

@Stable
@Composable
fun <T> PrefDelegate<T>.collectAsState(): State<T> {
    val flow = remember { valueFlow() }
    val init = getValue()
    return flow.collectAsState(init)
}

@Stable
@Composable
fun <T> PrefDelegate<T>.asMutableState(): MutableState<T> {
    val flow = remember { valueFlow() }
    val init = getValue()
    val readOnly = flow.collectAsState(init)
    return remember {
        object : MutableState<T> {
            override var value: T
                get() = readOnly.value
                set(value) {
                    setValue(value)
                }
            override fun component1() = TODO()
            override fun component2() = TODO()
        }
    }
}

object Settings : DataStorePreferences(null) {
    private const val KEY_SHOW_TAG_TRANSLATIONS = "show_tag_translations"

    private val _favFlow = MutableSharedFlow<Unit>()
    val favChangesFlow = _favFlow.debounce(1000)
    var favCat by stringArrayPref("fav_cat", 10, "Favorites").emitTo(_favFlow)
    var favCount by intArrayPref("fav_count", 10).emitTo(_favFlow)
    var favCloudCount by intPref("fav_cloud", 0).emitTo(_favFlow)

    val listMode = intPref("list_mode_2", 0)
    val detailSize = intPref("detail_size_2", 0)
    val thumbColumns = intPref("thumb_columns", 3)
    val listThumbSize = intPref("list_tile_size", 40)
    val languageFilter = intPref("language_filter", -1)
    val meteredNetworkWarning = boolPref("cellular_network_warning", false)
    val predictiveNavAnim = boolPref("predictive_nav_anim", true)

    val needSignInFlow: Flow<Boolean>
    var downloadScheme by stringOrNullPref("image_scheme", null)
    var downloadAuthority by stringOrNullPref("image_authority", null)
    var downloadPath by stringOrNullPref("image_path", null)
    var downloadQuery by stringOrNullPref("image_query", null)
    var downloadFragment by stringOrNullPref("image_fragment", null)
    var archivePasswds by stringSetOrNullPref("archive_passwds")
    var downloadDelay by intPref("download_delay_3", 1000)
    var gallerySite by intPref("gallery_site_2", 0).observed { updateWhenGallerySiteChanges() }
    var multiThreadDownload by intPref("download_thread_2", 3)
    var preloadImage by intPref("preload_image_2", 5)
    var downloadTimeout by intPref("download_timeout", 60)
    var theme by intPref("theme_2", -1).observed { updateWhenThemeChanges() }
    var thumbResolution by intPref("thumb_resolution_2", 0)
    var readCacheSize by intPref("read_cache_size_2", 640)
    var launchPage by intPref("launch_page_2", 0)
    var commentThreshold by intPref("comment_threshold", -101)
    var forceEhThumb by boolPref("force_eh_thumb", false)
    var showComments by boolPref("show_gallery_comments", true)
    var requestNews by boolPref("request_news", false).observed { updateWhenRequestNewsChanges() }
    var hideHvEvents by boolPref("hide_hv_events", false)
    var showJpnTitle by boolPref("show_jpn_title", false)
    var showGalleryPages by boolPref("show_gallery_pages", true)
    var showTagTranslations by boolPref(KEY_SHOW_TAG_TRANSLATIONS, false).observed { updateWhenTagTranslationChanges() }
    var appLinkVerifyTip by boolPref("app_link_verify_tip", false)
    var enabledSecurity by boolPref("enable_secure", false)
    var useCIUpdateChannel by boolPref("ci_update_channel", false)
    var mediaScan by boolPref("media_scan", false).observed { updateWhenKeepMediaStatusChanges() }
    var hasDefaultDownloadLabel by boolPref("has_default_download_label", false)
    var qSSaveProgress by boolPref("qs_save_progress", true)
    var saveParseErrorBody by boolPref("save_parse_error_body", true)
    var saveCrashLog by boolPref("save_crash_log", true)
    var security by boolPref("require_unlock", false)
    var removeImageFiles by boolPref("include_pic", true)
    var needSignIn by boolPref("need_sign_in", true).also { needSignInFlow = it.valueFlow() }
    var blackDarkTheme by boolPref("black_dark_theme", false).observed { updateWhenAmoledModeChanges() }
    var harmonizeCategoryColor by boolPref("harmonize_category_color", true)
    var preloadThumbAggressively by boolPref("preload_thumb_aggressively", false)
    var downloadOriginImage by boolPref("download_origin_image", false)
    var enableCronet by boolPref("enable_cronet", true)
    var recentFavCat by intPref("recent_fav_cat", FavListUrlBuilder.FAV_CAT_LOCAL)
    var defaultFavSlot by intPref("default_favorite_slot", -2)
    var securityDelay by intPref("require_unlock_delay", 0)
    var clipboardTextHashCode by intPref("clipboard_text_hash_code", 0)
    var searchCategory by intPref("search_pref", EhUtils.ALL_CATEGORY)
    var requestNewsTimerHour by intPref("request_news_timer_hour", -1)
    var requestNewsTimerMinute by intPref("request_news_timer_minute", -1)
    var updateIntervalDays by intPref("update_interval_days", 7)
    var recentToplist by stringPref("recent_toplist", "11")
    var userAgent by stringPref("user_agent", CHROME_USER_AGENT)
    var recentDownloadLabel by stringOrNullPref("recent_download_label", null)
    var defaultDownloadLabel by stringOrNullPref("default_download_label", null)
    var displayName by stringOrNullPref("display_name", null)
    var avatar by stringOrNullPref("avatar", null)
    var language by stringPref("app_language", "system").observed { updateWhenLocaleChanges() }
    var lastDawnTime by longPref("last_dawn_time", 0)
    var lastUpdateTime by longPref("last_update_time", 0)
    var favDialogTheta by floatPref("fav_select_dialog_delta", 0F)

    // TODO: Remove this after swipe gestures are correctly handled in compose
    var touchSlopFactor by intPref("touch_slop", 3)

    // Tachiyomi Reader
    val colorFilter = boolPref("pref_color_filter_key", false)
    val colorFilterValue = intPref("color_filter_value", 0)
    val colorFilterMode = intPref("color_filter_mode", 0)
    val customBrightness = boolPref("pref_custom_brightness_key", false)
    val customBrightnessValue = intPref("custom_brightness_value", 0)

    init {
        if ("CN" == Locale.getDefault().country) {
            edit {
                if (KEY_SHOW_TAG_TRANSLATIONS !in prefs) showTagTranslations = true
            }
        }
    }

    interface Delegate<R> {
        val flowGetter: () -> Flow<Unit>
        operator fun getValue(thisRef: Any?, prop: KProperty<*>?): R
        operator fun setValue(thisRef: Any?, prop: KProperty<*>?, value: R)
    }

    private fun intArrayPref(key: String, count: Int) = object : Delegate<IntArray> {
        override val flowGetter: () -> Flow<Unit> = { _value.asFlow().flatMapMerge { it.changesFlow() }.conflate() }

        @Suppress("ktlint:standard:property-naming")
        private var _value = (0 until count).map { intPref("${key}_$it", 0) }.toTypedArray()
        override fun getValue(thisRef: Any?, prop: KProperty<*>?): IntArray = _value.map { it.value }.toIntArray()
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: IntArray) {
            check(value.size == count)
            edit { value.zip(_value) { v, d -> d.value = v } }
        }
    }

    private fun stringArrayPref(key: String, count: Int, defMetaValue: String) = object : Delegate<Array<String>> {
        override val flowGetter: () -> Flow<Unit> = { _value.asFlow().flatMapMerge { it.changesFlow() }.conflate() }

        @Suppress("ktlint:standard:property-naming")
        private var _value = (0 until count).map { stringPref("${key}_$it", "$defMetaValue $it") }.toTypedArray()
        override fun getValue(thisRef: Any?, prop: KProperty<*>?): Array<String> = _value.map { it.value }.toTypedArray()
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: Array<String>) {
            check(value.size == count)
            edit { value.zip(_value) { v, d -> d.value = v } }
        }
    }
}
