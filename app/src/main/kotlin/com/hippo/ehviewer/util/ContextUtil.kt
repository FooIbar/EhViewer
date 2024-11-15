package com.hippo.ehviewer.util

import android.content.Context
import android.content.Intent

fun Context.restartApplication() {
    packageManager.getLaunchIntentForPackage(packageName)?.let {
        startActivity(Intent.makeRestartActivityTask(it.component))
        Runtime.getRuntime().exit(0)
    }
}
