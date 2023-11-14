package com.hippo.ehviewer.util

import android.os.Build
import android.os.ext.SdkExtensions

val isAtLeastN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
val isAtLeastO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
val isAtLeastP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
val isAtLeastQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
val isAtLeastR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
val isAtLeastS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val isAtLeastT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
val isAtLeastU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
val isAtLeastSExtension7 = isAtLeastR && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 7
