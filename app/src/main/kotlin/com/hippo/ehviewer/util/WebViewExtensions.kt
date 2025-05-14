package com.hippo.ehviewer.util

import android.annotation.SuppressLint
import android.webkit.WebView
import com.hippo.ehviewer.BuildConfig

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setDefaultSettings() = with(settings) {
    builtInZoomControls = true
    displayZoomControls = false
    javaScriptEnabled = true

    // Use mobile user-agent to bypass Cloudflare
    userAgentString = CHROME_MOBILE_USER_AGENT
}

private const val CHROME_MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${BuildConfig.CHROME_VERSION}.0.0.0 Mobile Safari/537.36"
