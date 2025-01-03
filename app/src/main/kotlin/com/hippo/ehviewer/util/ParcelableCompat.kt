package com.hippo.ehviewer.util

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String?): T? = IntentCompat.getParcelableExtra(this, key, T::class.java)
