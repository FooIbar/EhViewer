package com.hippo.ehviewer.client.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class VoteStatus(val emoji: String) {
    NONE(""),
    UP("↑"),
    DOWN("↓"),
}

enum class WeakStatus {
    NORMAL,
    ACTIVE,
    WEAK,
}

@Parcelize
data class GalleryTag(
    val text: String,
    val weak: WeakStatus,
    val vote: VoteStatus,
) : Parcelable
