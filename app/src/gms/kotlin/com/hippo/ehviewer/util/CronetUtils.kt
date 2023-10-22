package com.hippo.ehviewer.util

import com.google.android.gms.net.CronetProviderInstaller
import com.hippo.ehviewer.Settings
import org.chromium.net.CronetProvider
import splitties.init.appCtx

val isCronetSupported: Boolean
    get() = Settings.enableCronet && CronetProvider.getAllProviders(appCtx).any { it.isEnabled }.also {
        if (!it) {
            CronetProviderInstaller.installProvider(appCtx)
        }
    }
