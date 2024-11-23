package com.hippo.ehviewer.client.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class VoteStatus(val emoji: String) {
    NONE(""),
    UP("\uD83D\uDC4D"),
    DOWN("\uD83D\uDC4E"),
}

@Parcelize
data class GalleryTag(
    val text: String,
    val weak: Boolean,
    val vote: VoteStatus,
) : Parcelable
