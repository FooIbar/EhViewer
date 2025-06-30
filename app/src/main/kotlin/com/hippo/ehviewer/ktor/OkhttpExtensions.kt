package com.hippo.ehviewer.ktor

import com.hippo.ehviewer.util.isAtLeastQ
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.AsyncDns
import okhttp3.android.AndroidAsyncDns

fun OkHttpConfig.configureClient() {
    config {
        if (isAtLeastQ) {
            dns(AsyncDns.toDns(AndroidAsyncDns.IPv4, AndroidAsyncDns.IPv6))
        }
    }
}
