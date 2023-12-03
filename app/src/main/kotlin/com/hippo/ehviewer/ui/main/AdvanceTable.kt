package com.hippo.ehviewer.ui.main

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object AdvanceTable {
    const val SH = 0x1
    const val STO = 0x2
    const val SFL = 0x4
    const val SFU = 0x8
    const val SFT = 0x10
}

@Parcelize
data class AdvancedSearchOption(
    val advanceSearch: Int = 0,
    val minRating: Int = 0,
    val fromPage: Int = 0,
    val toPage: Int = 0,
) : Parcelable
