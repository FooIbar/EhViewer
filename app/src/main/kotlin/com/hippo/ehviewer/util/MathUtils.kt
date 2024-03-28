package com.hippo.ehviewer.util

fun calculateFraction(a: Float, b: Float, pos: Float) = ((pos - a) / (b - a)).coerceIn(0f, 1f)
