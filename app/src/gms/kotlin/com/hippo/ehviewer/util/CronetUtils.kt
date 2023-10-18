package com.hippo.ehviewer.util

import com.google.android.gms.net.CronetProviderInstaller
import com.hippo.ehviewer.Settings
import splitties.init.appCtx

val isCronetSupported: Boolean
    get() = Settings.enableCronet && CronetProviderInstaller.isInstalled().also {
        if (!it) {
            CronetProviderInstaller.installProvider(appCtx)
        }
    }
