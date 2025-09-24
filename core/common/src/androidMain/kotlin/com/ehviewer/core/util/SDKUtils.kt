package com.ehviewer.core.util

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(Build.VERSION_CODES.N)
val isAtLeastN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

@ChecksSdkIntAtLeast(Build.VERSION_CODES.O)
val isAtLeastO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@ChecksSdkIntAtLeast(Build.VERSION_CODES.O_MR1)
val isAtLeastOMR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

@ChecksSdkIntAtLeast(Build.VERSION_CODES.P)
val isAtLeastP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

@ChecksSdkIntAtLeast(Build.VERSION_CODES.Q)
val isAtLeastQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@ChecksSdkIntAtLeast(Build.VERSION_CODES.R)
val isAtLeastR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

@ChecksSdkIntAtLeast(Build.VERSION_CODES.S)
val isAtLeastS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@ChecksSdkIntAtLeast(Build.VERSION_CODES.TIRAMISU)
val isAtLeastT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@ChecksSdkIntAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
val isAtLeastU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

@ChecksSdkIntAtLeast(api = 7, extension = Build.VERSION_CODES.S)
val isAtLeastSExtension7 = isAtLeastR && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 7
