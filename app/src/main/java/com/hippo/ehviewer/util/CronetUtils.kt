package com.hippo.ehviewer.util

import com.google.android.gms.net.CronetProviderInstaller

val isCronetSupported: Boolean
    get() = isAtLeastQ && CronetProviderInstaller.isInstalled()
