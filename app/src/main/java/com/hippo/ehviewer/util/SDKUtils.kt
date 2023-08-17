package com.hippo.ehviewer.util

import android.os.Build
import android.os.ext.SdkExtensions

val isAtLeastQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
val isAtLeastR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
val isAtLeastSExtension7 = isAtLeastR && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 7
