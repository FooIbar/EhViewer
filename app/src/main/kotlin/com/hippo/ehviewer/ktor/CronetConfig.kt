package com.hippo.ehviewer.ktor

import android.content.Context
import io.ktor.client.engine.HttpClientEngineConfig
import org.chromium.net.CronetEngine

class CronetConfig : HttpClientEngineConfig() {
    var client: CronetEngine? = null

    fun config(context: Context, block: CronetEngine.Builder.() -> Unit) {
        client = CronetEngine.Builder(context).apply(block).build()
    }
}
