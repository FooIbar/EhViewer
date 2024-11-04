package com.hippo.ehviewer.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

/**
 * Find the closest Activity in a given Context.
 */
inline fun <reified T : Activity> Context.findActivity(): T {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context as T
        context = context.baseContext
    }
    throw IllegalStateException("findActivity() should be called in the context of an Activity")
}

fun Context.restartApplication() {
    packageManager.getLaunchIntentForPackage(packageName)?.let {
        startActivity(Intent.makeRestartActivityTask(it.component))
        Runtime.getRuntime().exit(0)
    }
}
