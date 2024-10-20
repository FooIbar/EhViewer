package com.hippo.ehviewer.ktor

import io.ktor.client.engine.HttpClientEngineFactory
import splitties.init.appCtx

object Cronet : HttpClientEngineFactory<CronetConfig> {
    override fun create(block: CronetConfig.() -> Unit) = CronetEngine(CronetConfig(appCtx).apply(block))
}
