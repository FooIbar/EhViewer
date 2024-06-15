package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.provider.Settings
import androidx.appcompat.view.ContextThemeWrapper
import com.hippo.ehviewer.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences

/**
 * Converts to px.
 */
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

/** Gets the duration multiplier for general animations on the device
 * @see Settings.Global.ANIMATOR_DURATION_SCALE
 */
val Context.animatorDurationScale: Float
    get() = Settings.Global.getFloat(this.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

private const val TABLET_UI_REQUIRED_SCREEN_WIDTH_DP = 720

fun Configuration.isTabletUi(): Boolean = smallestScreenWidthDp >= TABLET_UI_REQUIRED_SCREEN_WIDTH_DP

/**
 * Returns true if current context is in night mode
 */
fun Context.isNightMode(): Boolean = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

/**
 * Creates night mode Context depending on reader theme/background
 *
 * Context wrapping method obtained from AppCompatDelegateImpl
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:appcompat/appcompat/src/main/java/androidx/appcompat/app/AppCompatDelegateImpl.java;l=348;drc=e28752c96fc3fb4d3354781469a1af3dbded4898
 */
fun Context.createReaderThemeContext(): Context {
    val isDarkBackground = when (ReaderPreferences.readerTheme().get()) {
        1, 2 -> true // Black, Gray
        3 -> applicationContext.isNightMode() // Automatic bg uses activity background by default
        else -> false // White
    }
    val expected = if (isDarkBackground) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != expected) {
        val overrideConf = Configuration()
        overrideConf.setTo(resources.configuration)
        overrideConf.uiMode = overrideConf.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or expected

        val wrappedContext = ContextThemeWrapper(this, R.style.AppTheme)
        wrappedContext.applyOverrideConfiguration(overrideConf)
        return wrappedContext
    }
    return this
}
