@file:RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)

package com.hippo.ehviewer.ktor

import android.os.Build
import androidx.annotation.RequiresExtension
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory

object Cronet : HttpClientEngineFactory<CronetConfig> {
    override fun create(block: CronetConfig.() -> Unit): HttpClientEngine = CronetEngine(CronetConfig().apply(block))
}
