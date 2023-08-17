package com.hippo.ehviewer.util

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@get:ChecksSdkIntAtLeast(Build.VERSION_CODES.S, extension = 7)
val isCronetSupported: Boolean
    get() = isAtLeastSExtension7
