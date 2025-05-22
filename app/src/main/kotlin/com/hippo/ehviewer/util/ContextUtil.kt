package com.hippo.ehviewer.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

context(ctx: Context)
inline fun <reified T : Activity> findActivity(): T {
    var context = ctx
    while (context is ContextWrapper) {
        if (context is Activity) return context as T
        context = context.baseContext
    }
    throw IllegalStateException("findActivity() should be called in the context of an Activity")
}

context(ctx: Context)
fun restartApplication() = with(ctx) {
    packageManager.getLaunchIntentForPackage(packageName)?.let {
        startActivity(Intent.makeRestartActivityTask(it.component))
        Runtime.getRuntime().exit(0)
    }
}
