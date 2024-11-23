package com.hippo.ehviewer.client.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class VoteStatus(val append: String) {
    NONE(""),
    UP("↑"),
    DOWN("↓"),
}

enum class PowerStatus {
    SOLID,
    ACTIVE,
    WEAK,
}

@Parcelize
data class GalleryTag(
    val text: String,
    val weak: PowerStatus,
    val vote: VoteStatus,
) : Parcelable
