package com.hippo.ehviewer.ui.main

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

object AdvanceTable {
    const val SH = 0x1
    const val STO = 0x2
    const val SFL = 0x4
    const val SFU = 0x8
    const val SFT = 0x10
}

@Serializable
data class AdvancedSearchOption(
    @Transient
    val advanceSearch: Int = 0,
    val minRating: Int = 0,
    val fromPage: Int = 0,
    val toPage: Int = 0,
)
