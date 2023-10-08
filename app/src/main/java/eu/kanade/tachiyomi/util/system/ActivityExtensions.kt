package eu.kanade.tachiyomi.util.system

import android.app.Activity
import com.hippo.ehviewer.util.isAtLeastP

/**
 * Checks whether if the device has a display cutout (i.e. notch, camera cutout, etc.).
 *
 * Only works in Android 9+.
 */
fun Activity.hasDisplayCutout(): Boolean {
    return isAtLeastP && window.decorView.rootWindowInsets?.displayCutout != null
}
