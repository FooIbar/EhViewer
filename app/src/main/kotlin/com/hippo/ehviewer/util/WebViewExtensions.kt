package com.hippo.ehviewer.util

import android.annotation.SuppressLint
import android.webkit.WebView
import com.hippo.ehviewer.Settings

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setDefaultSettings() = with(settings) {
    builtInZoomControls = true
    displayZoomControls = false
    javaScriptEnabled = true
    userAgentString = Settings.userAgent
}
