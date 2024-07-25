package com.hippo.ehviewer.ktor

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory

object Cronet : HttpClientEngineFactory<CronetConfig> {
    override fun create(block: CronetConfig.() -> Unit): HttpClientEngine =
        CronetEngine(CronetConfig().apply(block))
}
