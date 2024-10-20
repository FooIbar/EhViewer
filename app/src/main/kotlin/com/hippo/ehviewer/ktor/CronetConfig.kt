package com.hippo.ehviewer.ktor

import android.content.Context
import io.ktor.client.engine.HttpClientEngineConfig
import org.chromium.net.CronetEngine

class CronetConfig(val context: Context) : HttpClientEngineConfig() {
    var config: CronetEngine.Builder.() -> Unit = {}
}
