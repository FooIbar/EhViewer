package com.hippo.ehviewer.util

import com.google.android.gms.net.CronetProviderInstaller
import com.hippo.ehviewer.Settings

val isCronetSupported: Boolean
    get() = Settings.enableQuic && isAtLeastQ && CronetProviderInstaller.isInstalled()
