package com.hippo.ehviewer.util

import com.google.android.gms.net.CronetProviderInstaller
import org.chromium.net.CronetProvider
import splitties.init.appCtx

val isCronetAvailable: Boolean
    get() = CronetProvider.getAllProviders(appCtx).any { it.isEnabled }.also {
        if (!it) {
            CronetProviderInstaller.installProvider(appCtx)
        }
    }
