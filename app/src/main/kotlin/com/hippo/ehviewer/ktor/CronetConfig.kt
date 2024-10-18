@file:RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)

package com.hippo.ehviewer.ktor

import android.content.Context
import android.net.http.HttpEngine
import android.os.Build
import androidx.annotation.RequiresExtension
import io.ktor.client.engine.HttpClientEngineConfig

class CronetConfig(val context: Context) : HttpClientEngineConfig() {
    var config: HttpEngine.Builder.() -> Unit = {}
}
