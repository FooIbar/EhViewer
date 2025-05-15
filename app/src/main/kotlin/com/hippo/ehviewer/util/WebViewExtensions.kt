package com.hippo.ehviewer.util

import android.annotation.SuppressLint
import android.webkit.WebView
import com.hippo.ehviewer.ktor.CHROME_MOBILE_USER_AGENT

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setDefaultSettings() = with(settings) {
    builtInZoomControls = true
    displayZoomControls = false
    javaScriptEnabled = true

    // Always use mobile user-agent to bypass Cloudflare
    userAgentString = CHROME_MOBILE_USER_AGENT
}
